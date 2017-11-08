package org.robovm.utils.codesign.utils;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;

public class FileByteBuffer {
    private final RandomAccessFile file;
    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    private int position;
    private int limit;
    private int fileStartOffset;

    public FileByteBuffer(RandomAccessFile file) {
        this.file = file;
        try {
            this.limit = (int) file.length();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileByteBuffer(RandomAccessFile file, ByteOrder byteOrder, int limit, int fileStartOffset) {
        this.file = file;
        this.byteOrder = byteOrder;
        this.limit = limit;
        this.fileStartOffset = fileStartOffset;
    }

    public FileByteBuffer slice() {
        return new FileByteBuffer(file, byteOrder, limit - position, fileStartOffset + position );
    }

    public void get(byte[] bytes) {
        sanityReadSize(bytes.length);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += bytes.length;
                file.read(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void get(byte[] bytes, int offset, int size) {
        sanityReadSize(size);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += size;
                file.read(bytes, offset, size);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public byte get() {
        sanityReadSize(1);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += 1;
                return file.readByte();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public char getChar() {
        sanityReadSize(2);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += 2;
                char c = file.readChar();
                return byteOrder == ByteOrder.BIG_ENDIAN ? c : Character.reverseBytes(c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public short getShort() {
        sanityReadSize(2);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += 2;
                short s = file.readShort();
                return byteOrder == ByteOrder.BIG_ENDIAN ? s : Short.reverseBytes(s);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getInt() {
        sanityReadSize(4);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += 4;
                int i = file.readInt();
                return byteOrder == ByteOrder.BIG_ENDIAN ? i : Integer.reverseBytes(i);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long getLong() {
        sanityReadSize(8);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += 8;
                long l = file.readLong();
                return byteOrder == ByteOrder.BIG_ENDIAN ? l : Long.reverseBytes(l);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public float getFloat() {
        sanityReadSize(4);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += 4;
                float f = file.readFloat();
                return byteOrder == ByteOrder.BIG_ENDIAN ? f : Float.intBitsToFloat(Integer.reverse(Float.floatToIntBits(f)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public double getDouble() {
        sanityReadSize(8);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += 8;
                double d = file.readDouble();
                return byteOrder == ByteOrder.BIG_ENDIAN ? d : Double.longBitsToDouble(Long.reverse(Double.doubleToLongBits(d)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void order(ByteOrder order) {
        this.byteOrder = order;
    }

    public void position(int offset) {
        if (position < 0 || position > limit)
            throw new IllegalArgumentException();
        this.position = offset;
    }

    public int position() {
        return position;
    }

    public void limit(int size) {
        if (size < 0 || size > limit)
            throw new IllegalArgumentException();
        limit = size;
    }

    public int limit() {
        return this.limit;
    }

    public int remaining() {
        return limit - position;
    }

    private void sanityReadSize(int size) {
        if (position + size > limit)
            throw new BufferUnderflowException();
    }

}
