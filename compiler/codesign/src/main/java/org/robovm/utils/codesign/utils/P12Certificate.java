package org.robovm.utils.codesign.utils;

import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.ContentInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.bc.BcDefaultDigestProvider;
import org.bouncycastle.pkcs.*;
import org.bouncycastle.pkcs.bc.BcPKCS12MacCalculatorBuilderProvider;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.robovm.utils.codesign.exceptions.CodeSignException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Enumeration;

public class P12Certificate {

//    static {
//        Security.addProvider(new BouncyCastleProvider());
//    }

    private final X509Certificate certificate;
    private final String certificateName;
    private final String certificateTeamId;
    private final String certificateFingerprint;
    private final PrivateKey privateKey;
    private final String privateKeyName;

    private P12Certificate(X509Certificate certificate, String certificateName, String certificateTeamId, String certificateFingerprint, PrivateKey privateKey, String privateKeyName) {
        this.certificate = certificate;
        this.certificateName = certificateName;
        this.certificateTeamId = certificateTeamId;
        this.certificateFingerprint = certificateFingerprint;
        this.privateKey = privateKey;
        this.privateKeyName = privateKeyName;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public String getCertificateName() {
        return certificateName;
    }

    public String getCertificateTeamId() {
        return certificateTeamId;
    }

    public String getCertificateFingerprint() {
        return certificateFingerprint;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getPrivateKeyName() {
        return privateKeyName;
    }

    /**
     * based on
     * joschi/cryptoworkshop-bouncycastle/blob/master/src/main/java/cwguide/JcePKCS12Example.java
     * using bouncycastle as KeyStore doesn't load certificate from apple exported p12
     *
     * @param path to p12 to read
     */
    public static P12Certificate load(File path) {
        final String p12password = "";
        X509Certificate certificate = null;
        String certificateName = null;
        String certificateTeamId = null;
        String certificateFingerprint = null;
        PrivateKey privateKey = null;
        String privateKeyName = null;

        try {
            PKCS12PfxPdu pfx = new PKCS12PfxPdu(Files.readAllBytes(path.toPath()));

            if (!pfx.isMacValid(new BcPKCS12MacCalculatorBuilderProvider(BcDefaultDigestProvider.INSTANCE), p12password.toCharArray())) {
                throw new CodeSignException("PKCS#12 MAC test failed @ " + path);
            }

            ContentInfo[] infos = pfx.getContentInfos();
            InputDecryptorProvider inputDecryptorProvider = new JcePKCSPBEInputDecryptorProviderBuilder().setProvider("BC").build(p12password.toCharArray());
            JcaX509CertificateConverter jcaConverter = new JcaX509CertificateConverter().setProvider("BC");

            // move through info bags to get certificate/privete key
            for (int i = 0; i != infos.length; i++) {
                if (infos[i].getContentType().equals(PKCSObjectIdentifiers.encryptedData)) {

                    // currently expect only one cert
                    if (certificate != null) {
                        // already have one
                        continue;
                    }

                    PKCS12SafeBagFactory dataFact = new PKCS12SafeBagFactory(infos[i], inputDecryptorProvider);
                    for (PKCS12SafeBag bag : dataFact.getSafeBags()) {
                        // get certificate
                        X509CertificateHolder certHldr = (X509CertificateHolder) bag.getBagValue();
                        certificateFingerprint = ByteBufferUtils.byteArrayToHex(DiggestAlgorithm.HashSHA1.hash(certHldr.getEncoded()));
                        certificate = jcaConverter.getCertificate(certHldr);
                        // get certificate team id
                        RDN r = certHldr.getSubject().getRDNs(BCStyle.OU)[0];
                        certificateTeamId = r.getFirst().getValue().toString();

                        // move through attributes to get cert name
                        for (Attribute attr : bag.getAttributes()) {

                            if (attr.getAttrType().equals(PKCS12SafeBag.friendlyNameAttribute))
                                certificateName = ((DERBMPString) attr.getAttributeValues()[0]).getString();
                        }
                    }
                } else {
                    if (privateKey != null) {
                        // already have one
                        continue;
                    }

                    PKCS12SafeBagFactory dataFact = new PKCS12SafeBagFactory(infos[i]);
                    PKCS12SafeBag[] bags = dataFact.getSafeBags();
                    PKCS8EncryptedPrivateKeyInfo encInfo = (PKCS8EncryptedPrivateKeyInfo) bags[0].getBagValue();
                    PrivateKeyInfo info = encInfo.decryptPrivateKeyInfo(inputDecryptorProvider);
                    KeyFactory keyFact = KeyFactory.getInstance(info.getPrivateKeyAlgorithm().getAlgorithm().getId(), "BC");
                    privateKey = keyFact.generatePrivate(new PKCS8EncodedKeySpec(info.getEncoded()));
                    for (Attribute attr : bags[0].getAttributes()) {
                        if (attr.getAttrType().equals(PKCS12SafeBag.friendlyNameAttribute))
                            privateKeyName = ((DERBMPString) attr.getAttributeValues()[0]).getString();
                    }
                }
            }

        } catch (IOException | NoSuchProviderException | PKCSException | InvalidKeySpecException | NoSuchAlgorithmException | CertificateException e) {
            throw new CodeSignException("Reading PKCS#12 failed due exception ! " + e.getMessage() + "@ " + path, e);
        }


        return new P12Certificate(certificate, certificateName, certificateTeamId, certificateFingerprint, privateKey, privateKeyName);
    }
}