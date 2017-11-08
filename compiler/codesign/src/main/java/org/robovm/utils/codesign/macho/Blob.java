package org.robovm.utils.codesign.macho;

import org.robovm.utils.codesign.utils.ByteBufferUtils;
import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.nio.ByteBuffer;

public class Blob {

    protected final int magic;
    private byte[] rawBytes;


    public Blob(int magic) {
        this.magic = magic;
    }

    public byte[] rawBytes() {
        return rawBytes;
    }

    protected void setBlobData(byte[] blobData) {
        this.rawBytes = blobData;
    }

    protected void setBlobData(ByteBuffer reader) {
        reader.position(0);
        this.rawBytes = ByteBufferUtils.readBytes(reader, reader.limit());
    }

    protected void setBlobData(FileByteBuffer reader) {
        reader.position(0);
        this.rawBytes = ByteBufferUtils.readBytes(reader, reader.limit());
    }

    public int getMagic() {
        return magic;
    }

    public static final class Magic {
        public static final int REQUIREMENT = 0xfade0c00;           // single requirement
        public static final int REQUIREMENTS = 0xfade0c01;          // requirement set
        public static final int CODEDIRECTORY = 0xfade0c02;         // CodeDirectory
        public static final int ENTITLEMENT = 0xfade7171;           // entitlement blob
        public static final int BLOBWRAPPER = 0xfade0b01;           // a generic blob wrapped around arbitrary (flat) binary data
        public static final int EMBEDDED_SIGNATURE = 0xfade0cc0;    // single-architecture embedded signature
        public static final int DETACHED_SIGNATURE = 0xfade0cc1;    // detached multi-architecture signature
        public static final int CODE_SIGN_DRS = 0xfade0c05;
    }

    public static String magicToString(int m) {
        String s;
        if (m == Magic.REQUIREMENT)
            s = "REQUIREMENT";
        else if (m == Magic.REQUIREMENTS)
            s = "REQUIREMENTS";
        else if (m == Magic.CODEDIRECTORY)
            s = "CODEDIRECTORY";
        else if (m == Magic.ENTITLEMENT)
            s = "ENTITLEMENT";
        else if (m == Magic.BLOBWRAPPER)
            s = "BLOBWRAPPER";
        else if (m == Magic.EMBEDDED_SIGNATURE)
            s = "EMBEDDED_SIGNATURE";
        else if (m == Magic.DETACHED_SIGNATURE)
            s = "DETACHED_SIGNATURE";
        else if (m == Magic.CODE_SIGN_DRS)
            s = "CODE_SIGN_DRS";
        else
            s = "unknown";

        s += "(0x" + Integer.toHexString(m) + ")";
        return s;
    }

}
