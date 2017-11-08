package org.robovm.utils.codesign.utils;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utilities for manipulating byte-buffer
 */
public class ByteBufferUtils {
    public static String readStringZ(FileByteBuffer reader, int possition) {
        reader.position(possition);

        // reads null terminated string
        StringBuilder sb = new StringBuilder();
        for (byte b = reader.get(); b != 0; b = reader.get())
            sb.append((char) b);
        return sb.toString();
    }

    public static byte[] readBytes(FileByteBuffer reader, int size) {
        byte[] result = new byte[size];
        reader.get(result);
        return result;
    }

    public static byte[] readBytes(ByteBuffer reader, int size) {
        byte[] result = new byte[size];
        reader.get(result);
        return result;
    }

    public static void readBytes(FileByteBuffer reader, byte[] dest, int offset, int size) {
        reader.get(dest, offset, size);
    }

    public static String readRequirementString(FileByteBuffer reader) {
        int len = reader.getInt();
        byte[] bytes = new byte[len];
        reader.get(bytes);
        skipRequirementPadding(reader, bytes.length);
        return new String(bytes);
    }

    public static Oid readRequirementOid(FileByteBuffer reader) {
        int size = reader.getInt();
        byte[] der = new byte[size + 2];
        reader.get(der, 2, size);
        skipRequirementPadding(reader, size);

        // fix 6 and size bytes
        der[0] = 6;
        der[1] = (byte) size;
        try {
            return new Oid(der);
        } catch (GSSException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readRequirementBytes(FileByteBuffer reader) {
        int size = reader.getInt();
        byte[] bytes = new byte[size];
        reader.get(bytes);
        skipRequirementPadding(reader, size);

        return bytes;
    }

    private static void skipRequirementPadding(FileByteBuffer reader, int bytesRead) {
        int paddingCount = (4 - (bytesRead & 3) & 3);
        while (paddingCount > 0) {
            reader.get();
            paddingCount -= 1;
        }
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }


    /**
     * Wrapper around byte buffer that supports capacity grow once exceeded
     */
    public static class Writer {
        private ByteBuffer writer;

        public Writer(int initialCapacity) {
            this.writer = ByteBuffer.allocate(initialCapacity);
            this.writer.limit(0);
        }

        public Writer() {
            this(4 * 1024);
        }

        private void resizeToFit(int size) {
            // sanity checks that there is enough capacity
            long afterWritePos = writer.position() + size;
            long capacity = writer.capacity();
            if (afterWritePos > capacity) {
                // make capacity twice bigger that required
                long requiredCap = (afterWritePos + capacity - 1) / capacity;
                requiredCap *= capacity * 2;
                ByteBuffer buffer = ByteBuffer.allocate((int) requiredCap);
                // copy old buffer
                int oldPosition = writer.position();
                writer.position(0);
                buffer.put(writer);
                buffer.position(oldPosition);
                buffer.limit(writer.limit());
                buffer.order(writer.order());
                // replace with new
                writer = buffer;
            }

            // check limit
            if (afterWritePos > writer.limit())
                writer.limit((int) afterWritePos);
        }

        public int writeStringZ(String s) {
            byte[] bytes = s.getBytes();

            resizeToFit(bytes.length + 1);
            writer.put(bytes);
            writer.put((byte) 0);
            return bytes.length + 1;
        }

        public void put(byte b) {
            resizeToFit(1);
            writer.put(b);
        }

        public void put(byte[] bytes) {
            resizeToFit(bytes.length);
            writer.put(bytes);
        }

        public void put(byte[] bytes, int offset, int size) {
            resizeToFit(size);
            writer.put(bytes, offset, size);
        }


        public void putInt(int i) {
            resizeToFit(4);
            writer.putInt(i);
        }

        public void writeRequirementString(String s) {
            byte[] bytes = s.getBytes();
            resizeToFit(4 + bytes.length);

            writer.putInt(bytes.length);
            writer.put(bytes);
            writeRequirementPadding(bytes.length);
        }

        public void writeRequirementOid(Oid oid) {
            try {
                byte[] der = oid.getDER();
                int size = der.length - 2;
                // skip 6 and size bytes
                resizeToFit(size + 4);
                writer.putInt(size);
                writer.put(der, 2, size);
                writeRequirementPadding(size);
            } catch (GSSException e) {
                throw new RuntimeException(e);
            }
        }

        public void writeRequirementBytes(byte[] bytes) {
            resizeToFit(bytes.length + 4);
            writer.putInt(bytes.length);
            writer.put(bytes);
            writeRequirementPadding(bytes.length);
        }

        private void writeRequirementPadding(int bytesWritten) {
            int paddingCount = (4 - (bytesWritten & 3) & 3);
            if (paddingCount == 0)
                return;
            resizeToFit(paddingCount);
            while (paddingCount > 0) {
                writer.put((byte) 0);
                paddingCount -= 1;
            }
        }

        public void order(ByteOrder order) {
            writer.order(order);
        }

        public int position() {
            return writer.position();
        }

        public void position(int position) {
            writer.position(position);
        }

        // same as position but allow to move limit/capacity
        public void positionEx(int position) {
            if (position >= writer.limit())
                resizeToFit(position - writer.position());

            writer.position(position);
        }

        public int limit() {
            return writer.limit();
        }

        public byte[] readBytes(int count) {
            byte[] result = new byte[count];
            writer.get(result);
            return result;
        }

        public ByteBuffer rawByteBuffer() {
            return writer;
        }
    }
}
