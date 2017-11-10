package org.robovm.utils.codesign.utils;

import com.dd.plist.NSArray;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;
import org.robovm.utils.codesign.CodeSign;
import org.robovm.utils.codesign.context.SignCtx;
import org.robovm.utils.codesign.context.VerifyCtx;
import org.robovm.utils.codesign.exceptions.CodeSignException;
import org.robovm.utils.codesign.exceptions.CodeSignSkippableException;
import org.robovm.utils.codesign.macho.CodeDirectoryBlob;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * utils to sign/check against Cryptographic Message Syntax
 */
public class CmsSignatureUtils {
    private static ASN1ObjectIdentifier appleHashAgilityOid = new ASN1ObjectIdentifier("1.2.840.113635.100.9.1");

    @SuppressWarnings("unchecked")
    public static boolean verifySignature(VerifyCtx rootCtx, byte[] detachedSignature, List<CodeDirectoryBlob> codeDirs) {
        boolean signatureValid = true;
        VerifyCtx ctx = rootCtx.push();

        // check code signature for primary CodeDirectory
        CodeDirectoryBlob primaryCodeDir = codeDirs.get(0);
        CMSProcessableByteArray originalData = new CMSProcessableByteArray(primaryCodeDir.rawBytes());
        CMSSignedData cms;
        try {
            cms = new CMSSignedData(originalData, detachedSignature);
        } catch (CMSException e) {
            ctx.debug("Failed to create CMSSignedData: " + e.getMessage());
            ctx.onError(new CodeSignSkippableException("Signature verification failed(CMSSignedData)", e));
            return false;
        }

        Store store = cms.getCertificates();
        SignerInformationStore signers = cms.getSignerInfos();

        debugDumpCertificates(ctx, store);

        Collection<SignerInformation> allSigners = (Collection<SignerInformation>) signers.getSigners();
        ctx.debug("Signers in signature(" + allSigners.size() + "):");
        VerifyCtx allSignersCtx = ctx.push();
        VerifyCtx oneSignerCtx = allSignersCtx.push();
        for (SignerInformation signer : allSigners) {
            allSignersCtx.debug(signer.getSID().getIssuer());
            Collection<X509CertificateHolder> certCollection = store.getMatches(signer.getSID());
            for (X509CertificateHolder h : certCollection) {
                X509Certificate cert = null;
                boolean matched;
                try {
                    cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(h);
                    matched = signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert));
                } catch (CertificateException | CMSException | OperatorCreationException e) {
                    oneSignerCtx.debug((cert == null ? "(unknown cert)" : cert.getSubjectDN()) + " verify exception: " + e.getMessage());
                    ctx.onError(new CodeSignSkippableException("Signature verification failed for " + signer.getSID().getIssuer(), e));

                    signatureValid = false;
                    continue;
                }

                oneSignerCtx.debug("[cert] " + cert.getSubjectDN() + (matched ? " matched" : " not matched"));
                if (!matched) {
                    // not verified
                    ctx.onError(new CodeSignSkippableException("Signature verification failed for " + signer.getSID().getIssuer()));
                    signatureValid = false;
                }
            }
        }

        if (codeDirs.size() == 1) {
            // only primary code directory
            return signatureValid;
        }

        // calculate code directory hashes
        ctx.debug("checking appleHashAgilityOid");
        VerifyCtx appleHashesCtx = ctx.push();
        byte[][] cdHashes = new byte[codeDirs.size()][];
        // primary (legacy) uses sha1, calculate separately
        cdHashes[0] = DiggestAlgorithm.HashSHA1.hash(codeDirs.get(0).rawBytes());
        for (int i = 1; i < codeDirs.size(); i++)
            cdHashes[i] = DiggestAlgorithm.HashSHA256Truncated.hash(codeDirs.get(i).rawBytes());

        // SHA256 code directory is present check for hashes stored in signed attributes
        //noinspection unchecked
        for (SignerInformation signer : (Collection<SignerInformation>) signers.getSigners()) {
            Attribute attr = signer.getSignedAttributes().get(appleHashAgilityOid);
            if (attr == null) {
                ctx.onError(new CodeSignSkippableException("appleHashAgilityOid is missing in " + signer.getSID().getIssuer()));
                signatureValid = false;
                continue;
            }

            DEROctetString attrData = (DEROctetString) attr.getAttrValues().getObjectAt(0).toASN1Primitive();
            // create a plist from it
            NSDictionary plist;
            try {
                String rawPlist = new String(attrData.getOctets());
                appleHashesCtx.debug("raw:");
                appleHashesCtx.push().debug(rawPlist);
                plist = (NSDictionary) PropertyListParser.parse(attrData.getOctets());
            } catch (IOException | SAXException | ParserConfigurationException | ParseException | PropertyListFormatException e) {
                // plist broken !
                ctx.onError(new CodeSignSkippableException("appleHashAgilityOid is broken(plist) in " + signer.getSID().getIssuer()));
                signatureValid = false;
                continue;
            }

            // verify hashes
            NSArray plistCdHashes = (NSArray) plist.get("cdhashes");
            if (plistCdHashes == null) {
                ctx.onError(new CodeSignSkippableException("appleHashAgilityOid: cdhashes is missing in " + signer.getSID().getIssuer()));
                signatureValid = false;
                continue;
            }

            if (cdHashes.length == plistCdHashes.count()) {
                // verify
                for (int i = 0; i < cdHashes.length; i++) {
                    byte[] storedHash = ((NSData) plistCdHashes.objectAtIndex(i)).bytes();
                    boolean matched = Arrays.equals(cdHashes[i], storedHash);
                    appleHashesCtx.debug(i + ": CodeDirectory hash " + (matched ? "matched" : "not matched"));

                    if (!matched) {
                        ctx.onError(new CodeSignSkippableException("appleHashAgilityOid: number of hashes is diffenent that codedirs in " + signer.getSID().getIssuer()));
                        signatureValid = false;
                    }
                }
            } else {
                ctx.onError(new CodeSignSkippableException("appleHashAgilityOid: number of hashes is diffenent that codedirs in " + signer.getSID().getIssuer()));
                signatureValid = false;
            }
        }

        return signatureValid;
    }


    private static void debugDumpCertificates(VerifyCtx log, Store store) {
        // dump all certificates
        @SuppressWarnings("unchecked") Collection<X509CertificateHolder> allCertificates = store.getMatches(null);
        log.debug("Cerificates in signature (" + allCertificates.size() + "):");
        VerifyCtx log2 = log.push();
        for (X509CertificateHolder h : allCertificates) {
            log2.debug(h.getSubject().toString());
        }
    }

    public static byte[] sign(SignCtx ctx, CodeDirectoryBlob[] codeDirs) {
        PrivateKey privateKey = ctx.getCertificate().getPrivateKey();
        X509Certificate cert = ctx.getCertificate().getCertificate();

        try {
            // prepare attributes table
            ASN1EncodableVector signedAttributes = new ASN1EncodableVector();
            // add signing time
            signedAttributes.add(new Attribute(CMSAttributes.signingTime, new DERSet(new DERUTCTime(new Date()))));

            // if there is more than two CDS there should be diggest of these added to attributes under appleHashAgilityOid
            if (codeDirs.length > 1) {
                NSObject[] hashes = new NSObject[codeDirs.length];
                // legacy one
                hashes[0] = new NSData(DiggestAlgorithm.HashSHA1.hash(codeDirs[0].rawBytes()));
                for (int i = 1; i < codeDirs.length; i++)
                    hashes[i] = new NSData(DiggestAlgorithm.HashSHA256Truncated.hash(codeDirs[i].rawBytes()));
                NSDictionary plist = new NSDictionary();
                plist.put("cdhashes", new NSArray(hashes));
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                PropertyListParser.saveAsXML(plist, os);

                // workaround
                byte[] xmlPlistBytes = os.toByteArray();
                // on windows these could be broken due \n\r separators
                String sep = System.getProperty("line.separator");
                if (!"\n".equals(sep)) {
                    String xmlPlist = new String(xmlPlistBytes);
                    xmlPlist = xmlPlist.replace(sep, "\n");
                    xmlPlistBytes = xmlPlist.getBytes();
                }

                // put to attribute
                signedAttributes.add(new Attribute(appleHashAgilityOid, new DERSet(new DEROctetString(xmlPlistBytes))));
            }
            AttributeTable signedAttributesTable = new AttributeTable(signedAttributes);
            DefaultSignedAttributeTableGenerator signedAttributeGenerator = new DefaultSignedAttributeTableGenerator(signedAttributesTable);

            // create certificate chain, add Apple credentials are public knowledge
            CertificateFactory certFactory = CertificateFactory.getInstance("X509");
            X509Certificate certAppleWWDRCA = (X509Certificate)certFactory.generateCertificate(CodeSign.class.getResourceAsStream("AppleWWDRCA.cer"));
            X509Certificate certAppleIncRootCertificate = (X509Certificate) certFactory.generateCertificate(CodeSign.class.getResourceAsStream("AppleIncRootCertificate.cer"));

            // make cert chain
            List<X509Certificate> x509Certificates = new ArrayList<>();
            x509Certificates.add(certAppleWWDRCA);
            x509Certificates.add(certAppleIncRootCertificate);
            x509Certificates.add(cert);
            Store certs = new JcaCertStore(x509Certificates);

            // set up the generator
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            JcaSimpleSignerInfoGeneratorBuilder signerInfoBuilder = new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BC");
            signerInfoBuilder.setSignedAttributeGenerator(signedAttributeGenerator);
            gen.addSignerInfoGenerator(signerInfoBuilder.build("SHA256withRSA", privateKey, cert));
            gen.addCertificates(certs);

            // create the signed-data object
            CMSSignedData cms = gen.generate(new CMSProcessableByteArray(codeDirs[0].rawBytes()));
            return cms.getEncoded();
        } catch (OperatorCreationException | CMSException | IOException | CertificateException e ) {
            throw new CodeSignException("Failed to create CMS signature due " + e.getMessage(), e);
        }
    }
}
