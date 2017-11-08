package org.robovm.utils.codesign.macho;

import org.robovm.utils.codesign.context.SignCtx;
import org.robovm.utils.codesign.context.VerifyCtx;
import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.nio.ByteBuffer;

/**
 * it is quite same as SuperBlob the only moment that it expects only Requirement items
 */
public class RequirementsBlob extends SuperBlob {


    /** type of entry to be used as type field in superblob */
    public static final class Type {
        public static final int Host = 1;
        public static final int Guest = 2;
        public static final int Designated = 3;
        public static final int LibraryRequirement = 4;
    }

    private RequirementsBlob(SignCtx ctx) {
        super(Magic.REQUIREMENTS);
    }

    public RequirementsBlob(VerifyCtx ctx, int magic, FileByteBuffer reader) {
        super(ctx, magic, reader);
        setBlobData(reader);
    }

    public static RequirementsBlob build(SignCtx ctx, RequirementBlob... blobs) {
        Builder builder = new Builder(new RequirementsBlob(ctx));
        // all requirements are considered as designated
        for (RequirementBlob req: blobs)
            builder.addItem(Type.Designated, req);
        return builder.build();
    }


    @Override
    protected Blob parseItem(VerifyCtx ctx, int type, int magic, FileByteBuffer reader) {
        if (magic != Blob.Magic.REQUIREMENT)
            throw new RuntimeException("RequirementsBlob find unsupported item with magic 0x" + Integer.toHexString(magic));

        ctx.debug("BLOB " + Blob.magicToString(magic));
        return new RequirementBlob(ctx.push(), magic, reader);
    }
}
