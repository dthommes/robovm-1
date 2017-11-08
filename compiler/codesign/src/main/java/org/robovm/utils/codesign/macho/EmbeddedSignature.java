package org.robovm.utils.codesign.macho;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import org.robovm.utils.codesign.context.SignCtx;
import org.robovm.utils.codesign.context.VerifyCtx;
import org.robovm.utils.codesign.exceptions.CodeSignException;
import org.robovm.utils.codesign.exceptions.CodeSignSkippableException;
import org.robovm.utils.codesign.macho.Requirements.Expression;
import org.robovm.utils.codesign.macho.Requirements.Match;
import org.robovm.utils.codesign.utils.ByteBufferUtils;
import org.robovm.utils.codesign.utils.CmsSignatureUtils;
import org.robovm.utils.codesign.utils.DiggestAlgorithm;
import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * root super blob with ability to verify code sign
 */
public class EmbeddedSignature extends SuperBlob {
    private EmbeddedSignature(VerifyCtx ctx, int magic, FileByteBuffer reader) {
        super(ctx, magic, reader);
    }

    private EmbeddedSignature(SignCtx ctx) {
        super(Magic.EMBEDDED_SIGNATURE);
    }

    public static EmbeddedSignature parse(VerifyCtx ctx, FileByteBuffer reader) {
        reader.position(0);
        int magic = reader.getInt();
        int size = reader.getInt();

        // expecting embedded signature
        if (magic != Blob.Magic.EMBEDDED_SIGNATURE) {
            if (magic == 0)
                throw new RuntimeException("Empty LC_CODE_SIGNATURE, slice probably is not signed");

            throw new RuntimeException("Unexpected magic " + magicToString(magic) + ", while expected " + magicToString(Blob.Magic.EMBEDDED_SIGNATURE));
        }

        ctx.debug("BLOB " + magicToString(magic));
        if (size != reader.limit())
            reader.limit(size);

        return new EmbeddedSignature(ctx.push(), magic, reader);
    }


    public void verifySignature(VerifyCtx ctx, FileByteBuffer archBytes, File codeResourcePath, File infoPlistPath) {
        CodeDirectoryBlob cd = getBlob(Blob.Magic.CODEDIRECTORY);
        int slotCount = cd.getSpecialSlotCount();
        if (slotCount >= CodeDirectoryBlob.SpecialSlot.InfoSlot.offset) {
            // validate info data
            // this slot hash corresponds to hash of entire Info.plist file content
            byte[] calculatedHash = cd.hash(infoPlistPath);
            byte[] signedHash = cd.getSpecialSlotHash(CodeDirectoryBlob.SpecialSlot.InfoSlot);
            if (!Arrays.equals(calculatedHash, signedHash))
                ctx.onError(new CodeSignSkippableException("Info.plist hash mismatch!"));
        }

        if (slotCount >= CodeDirectoryBlob.SpecialSlot.RequirementsSlot.offset) {
            // validate requirements slot
            // check if hash of requirements slot bytes corresponds ones written in code directory
            RequirementsBlob blob = getBlob(Blob.Magic.REQUIREMENTS);
            byte[] calculatedHash = cd.hash(blob.rawBytes());
            byte[] signedHash = cd.getSpecialSlotHash(CodeDirectoryBlob.SpecialSlot.RequirementsSlot);
            if (!Arrays.equals(calculatedHash, signedHash))
                ctx.onError(new CodeSignSkippableException("RequirementsBlob hash mismatch!"));
        }

        if (slotCount >= CodeDirectoryBlob.SpecialSlot.ResourceDirSlot.offset) {
            // validate resource dir
            // this slot hash corresponds to hash of entire _CodeSignature/CodeResources file content
            byte[] calculatedHash = cd.hash(codeResourcePath);
            byte[] signedHash = cd.getSpecialSlotHash(CodeDirectoryBlob.SpecialSlot.ResourceDirSlot);
            if (!Arrays.equals(calculatedHash, signedHash))
                ctx.onError(new CodeSignSkippableException("CodeResources hash mismatch!"));
        }

        if (slotCount >= CodeDirectoryBlob.SpecialSlot.EntitlementSlot.offset) {
            // validate entitlement slot
            // check if hash of entitlement slot bytes corresponds ones written in code directory
            EntitlementBlob blob = getBlob(Blob.Magic.ENTITLEMENT);
            byte[] calculatedHash = cd.hash(blob.rawBytes());
            byte[] signedHash = cd.getSpecialSlotHash(CodeDirectoryBlob.SpecialSlot.EntitlementSlot);
            if (!Arrays.equals(calculatedHash, signedHash))
                ctx.onError(new CodeSignSkippableException("EntitlementBlob hash mismatch!"));
        }

        // perform code sign check
        if (cd.getCodeSlotCount() > 0) {
            int pageSize = 1 << cd.getPageSizePow();
            int codeLimit = cd.getCodeLimit();

            // sanity
            if ((codeLimit + pageSize - 1) / pageSize != cd.getCodeSlotCount())
                throw new CodeSignException("CodeSlots  data integrity is broken in in CoeDirectory!");

            // check code bytes
            archBytes.position(0);
            for (int idx = 0; idx < cd.getCodeSlotCount(); idx++) {
                int bytesToRead = codeLimit > pageSize ? pageSize : codeLimit;
                codeLimit -= bytesToRead;
                byte[] calculatedHash = cd.hash(ByteBufferUtils.readBytes(archBytes, bytesToRead));
                byte[] signedHash = cd.getCodeSlotHash(idx);
                if (!Arrays.equals(calculatedHash, signedHash))
                    throw new CodeSignException("CodeSlot hash mismatch at pos: 0x" + Integer.toHexString(idx));
            }
        }

        // check signature itself
        // 1. get blob wrapper to test signature over code directory
        BlobWrapper bw = getBlob(Blob.Magic.BLOBWRAPPER);
        // check against first cs
        List<CodeDirectoryBlob> codeDirs = getBlobs(Blob.Magic.CODEDIRECTORY);

        ctx.debug("Verifying CMS signature");
        boolean status = CmsSignatureUtils.verifySignature(ctx, bw.getSignedData(), codeDirs);
        ctx.info("Verifying CMS signature" + (status ? "" : " - FAILED"));
    }


    /**
     * Signs or does dummy run to estimate space required to hold LC_CODE_SIGNATURE
     */
    private static EmbeddedSignature signOrEstimate(SignCtx ctx, FileByteBuffer archBytes, int archCodeLimit, File codeResourcePath,
                                                    File infoPlistPath, byte[] entitlements, boolean isEstimate) {
        Oid appleExtensionWwdrIntermediateOid;
        try {
            appleExtensionWwdrIntermediateOid = new Oid("1.2.840.113635.100.6.2.1");
        } catch (GSSException e) {
            throw new CodeSignException("Internal error constructing OID");
        }
        // start making blobs

        // requirements
        RequirementsBlob reqBlob = RequirementsBlob.build(ctx,
                new RequirementBlob(ctx,
                        Expression.opAnd(
                                Expression.opIdent(ctx.getIdentifier()),
                                Expression.opAnd(
                                        Expression.opAppleGenericAnchor(),
                                        Expression.opAnd(
                                            Expression.opCertField(0, "subject.CN", Match.matchEqual(ctx.getCertificate().getCertificateName())),
                                            Expression.opCertGeneric(1, appleExtensionWwdrIntermediateOid, Match.matchExists())
                                        )
                                )
                        )
                )
        );

        EntitlementBlob entitlementBlob = entitlements != null ? new EntitlementBlob(ctx, entitlements) : null;

        // create code directories
        DiggestAlgorithm[] cdAlgs = new DiggestAlgorithm[]{DiggestAlgorithm.HashSHA1, DiggestAlgorithm.HashSHA256};
        CodeDirectoryBlob[] cds = new CodeDirectoryBlob[cdAlgs.length];
        for (int idx = 0; idx < cds.length; idx ++) {
            CodeDirectoryBlob.Builder cdBuilder = new CodeDirectoryBlob.Builder(cdAlgs[idx], ctx.getIdentifier(), ctx.getCertificate().getCertificateTeamId(), isEstimate);
            cdBuilder.calculateCodeHash(archBytes, archCodeLimit);
            cdBuilder.calculateSpecialSlotHash(CodeDirectoryBlob.SpecialSlot.RequirementsSlot, reqBlob.rawBytes());
            if (entitlementBlob != null)
                cdBuilder.calculateSpecialSlotHash(CodeDirectoryBlob.SpecialSlot.EntitlementSlot, entitlementBlob.rawBytes());
            if (codeResourcePath != null)
                cdBuilder.calculateSpecialSlotHash(CodeDirectoryBlob.SpecialSlot.ResourceDirSlot, codeResourcePath);
            if (infoPlistPath != null)
                cdBuilder.calculateSpecialSlotHash(CodeDirectoryBlob.SpecialSlot.InfoSlot, infoPlistPath);
            cds[idx] = cdBuilder.build();
        }

        // creating blob wraper with CMS signature
        // if case of estimate assume 9K would be enough for CMS, check CodeSigner.cpp, state.mCMSSize = 9000;	// likely big enough
        byte[] cmsSignature = isEstimate ? new byte[9*1024] : CmsSignatureUtils.sign(ctx, cds);
        BlobWrapper bw = new BlobWrapper(ctx, cmsSignature);

        // getting everything together
        Builder signatureBuilder = new Builder(new EmbeddedSignature(ctx));
        signatureBuilder.addItem(Type.CodeDirectory, cds[0]);
        signatureBuilder.addItem(Type.Requirements, reqBlob);
        if (entitlementBlob != null)
            signatureBuilder.addItem(Type.Entitlement, entitlementBlob);
        for (int idx = 1; idx < cds.length; idx++)
            signatureBuilder.addItem(Type.AlternateCodeDirectory + (idx - 1), cds[idx]);
        signatureBuilder.addItem(Type.Signature, bw);

        return signatureBuilder.build();
    }

    public static EmbeddedSignature sign(SignCtx ctx, FileByteBuffer archBytes, int archCodeLimit, File codeResourcePath, File infoPlistPath, byte[] entitlements) {
        return signOrEstimate(ctx, archBytes, archCodeLimit, codeResourcePath, infoPlistPath, entitlements, false);
    }

    public static int estimateSignatureSize(SignCtx ctx, FileByteBuffer archBytes, int archCodeLimit, File codeResourcePath, File infoPlistPath, byte[] entitlements) {
        EmbeddedSignature dummySignature = signOrEstimate(ctx, archBytes, archCodeLimit, codeResourcePath, infoPlistPath, entitlements, true);
        // align 16 byte
        return (dummySignature.rawBytes().length + 15) & ~15;
    }
}
