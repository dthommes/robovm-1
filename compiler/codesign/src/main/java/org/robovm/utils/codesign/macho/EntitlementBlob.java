package org.robovm.utils.codesign.macho;

import org.robovm.utils.codesign.context.SignCtx;
import org.robovm.utils.codesign.context.VerifyCtx;
import org.robovm.utils.codesign.utils.ByteBufferUtils;
import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.nio.ByteBuffer;

/**
 * this blob contains Entitlement plist
 */
public class EntitlementBlob extends Blob {
    String entitlement;

    /**
     * constructor that is used byte-buffer
     */
    public EntitlementBlob(VerifyCtx ctx, int magic, FileByteBuffer reader) {
        super(magic);
        setBlobData(reader);

        reader.position(8); // skip magic and length
        byte[] entitlementBytes = new byte[reader.remaining()];
        reader.get(entitlementBytes);
        entitlement = new String(entitlementBytes);
        ctx.debug(entitlement);
    }

    public EntitlementBlob(SignCtx ctx, byte[] entitlementBytes) {
        super(Magic.ENTITLEMENT);
        entitlement = new String(entitlementBytes);

        // on windows these could be broken due \n\r separators
        String sep = System.getProperty("line.separator");
        if (!"\n".equals(sep)) {
            entitlement = entitlement.replace(sep, "\n");
            entitlementBytes = entitlement.getBytes();
        }

        // construct blob data
        ByteBufferUtils.Writer writer = new ByteBufferUtils.Writer();
        writer.putInt(magic);
        writer.putInt(8 + entitlementBytes.length);
        writer.put(entitlementBytes);
        setBlobData(writer.rawByteBuffer());
    }

    public String getEntitlement() {
        return entitlement;
    }

    @Override
    public String toString() {
        return "EntitlementBlob{" +
                "entitlement='" + entitlement + '\'' +
                '}';
    }
}
