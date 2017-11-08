package org.robovm.utils.codesign.macho;

import org.robovm.utils.codesign.context.SignCtx;
import org.robovm.utils.codesign.context.VerifyCtx;
import org.robovm.utils.codesign.utils.ByteBufferUtils;
import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.nio.ByteBuffer;

public class BlobWrapper extends Blob {
    byte[] signedData;
    public BlobWrapper(VerifyCtx ctx, int magic, FileByteBuffer reader) {
        super(magic);

        reader.position(8); // skip magic and length
        signedData = new byte[reader.remaining()];
        reader.get(signedData);
    }

    public BlobWrapper(SignCtx ctx, byte[] signedData) {
        super(Magic.BLOBWRAPPER);

        this.signedData = signedData;
        ByteBufferUtils.Writer writer = new ByteBufferUtils.Writer();
        writer.putInt(magic);
        writer.putInt(8 + signedData.length);
        writer.put(signedData);

        setBlobData(writer.rawByteBuffer());
    }

    public byte[] getSignedData() {
        return signedData;
    }
}
