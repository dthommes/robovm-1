package org.robovm.utils.codesign.macho;


import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Very simple mach-o loader
 */
public class SimpleMachOLoader implements AutoCloseable{
    private static final int LC_CODE_SIGNATURE = 0x1d;

    private static final int FAT_MAGIC = 0xcafebabe;
    private static final int FAT_CIGAM = 0xbebafeca;

    private static final int MH_MAGIC = 0xfeedface;
    private static final int MH_CIGAM = 0xcefaedfe; // NXSwapInt(MH_MAGIC)
    private static final int MH_MAGIC_64 = 0xfeedfacf; // the 64-bit mach magic number
    private static final int MH_CIGAM_64 = 0xcffaedfe; // NXSwapInt(MH_MAGIC_64)

    private RandomAccessFile executableFile;
    private List<ArchSlice> sliceList;

    public SimpleMachOLoader(File executable) throws MachOException {
        this(executable, false);
    }

    public SimpleMachOLoader(File executable, boolean modify) throws MachOException {
        FileByteBuffer rootReader;
        try {
            executableFile = new RandomAccessFile(executable, modify ? "rw" : "r");
            rootReader = new FileByteBuffer(executableFile);

            rootReader.order(ByteOrder.BIG_ENDIAN);
        } catch (IOException e) {
            throw new MachOException("Failed to open mach-o file", e);
        }

        // read architectures
        sliceList = new ArrayList<>();
        int magic = rootReader.getInt();
        if (magic == FAT_CIGAM || magic == FAT_MAGIC) {
            // get another reader, as fat header always big endian
            FileByteBuffer fatReader = rootReader.slice();
            fatReader.order(magic == FAT_MAGIC ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

            int count = fatReader.getInt();
            for (int i = 0; i < count; i++) {
                // read FatArch struct
                //cpu_type_t	cputype;	/* cpu specifier (int) */
                int cputype = fatReader.getInt();
                //cpu_subtype_t	cpusubtype;	/* machine specifier (int) */
                int cpusubtype = fatReader.getInt();
                //uint32_t	offset;		/* file offset to this object file */
                int offset = fatReader.getInt();
                //uint32_t	size;		/* size of this object file */
                int size = fatReader.getInt();
                //uint32_t	align;		/* alignment as a power of 2 */
                int align = fatReader.getInt();

                // handle slice
                rootReader.position(offset);
                FileByteBuffer sliceReader = rootReader.slice();
                sliceReader.order(ByteOrder.BIG_ENDIAN);
                sliceReader.limit(size);
                int sliceMagic = sliceReader.getInt();
                sliceList.add(readSlice(offset, sliceReader, sliceMagic));
            }
        } else  {
            sliceList.add(readSlice(0, rootReader, magic));
        }
    }

    public void close() {
        try {
            executableFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ArchSlice readSlice(int fileOffset, FileByteBuffer reader, int magic) throws MachOException {
        if (magic != MH_MAGIC && magic != MH_CIGAM && magic != MH_MAGIC_64 && magic != MH_CIGAM_64)
            throw new MachOException("unexpected Mach header MAGIC 0x" + Integer.toHexString(magic));

        reader.order((magic == MH_MAGIC || magic == MH_MAGIC_64) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        boolean is64bit = (magic == MH_CIGAM_64 || magic == MH_MAGIC_64);

        // read mach header
        // magic already was read
        //cpu_type_t	cputype;	/* cpu specifier */
        int cputype = reader.getInt();
        //cpu_subtype_t	cpusubtype;	/* machine specifier */
        int cpusubtype = reader.getInt();
        //uint32_t	filetype;	/* type of file */
        int filetype = reader.getInt();
        //uint32_t	ncmds;		/* number of load commands */
        int ncmds = reader.getInt();
        //uint32_t	sizeofcmds;	/* the size of all the load commands */
        int sizeofcmds = reader.getInt();
        //uint32_t	flags;		/* flags */
        int flags = reader.getInt();
        if (is64bit) {
            // just skip
            int reserved = reader.getInt();
        }

        // look through commands to find code sign
        int codeSignOffset = -1;
        int codeSignSize = -1;
        for (int idx = 0; idx < ncmds; idx++) {
            int pos = reader.position();
            int cmd = reader.getInt();
            int cmdsize = reader.getInt();

            if (cmd == LC_CODE_SIGNATURE) {
                // read code sign data
                codeSignOffset = reader.getInt();
                codeSignSize = reader.getInt();
                break;
            }

            reader.position(pos + cmdsize);
        }

        return new ArchSlice(fileOffset, reader, cputype, cpusubtype, reader.limit(), codeSignOffset, codeSignSize);
    }

    public List<ArchSlice> getSliceList() {
        return sliceList;
    }

    public static String archToString(int arch) {
        final int CPU_ARCH_ABI64 = 0x01000000;
        final int CPU_TYPE_X86 = 7;
        final int CPU_TYPE_X86_64 = (CPU_TYPE_X86 | CPU_ARCH_ABI64);
        final int CPU_TYPE_ARM = 12;
        final int CPU_TYPE_ARM64 = (CPU_TYPE_ARM | CPU_ARCH_ABI64);

        switch (arch) {
            case CPU_TYPE_X86:
                return "x86";
            case CPU_TYPE_X86_64:
                return "x86_64";
            case CPU_TYPE_ARM:
                return "armv7";
            case CPU_TYPE_ARM64:
                return "arm64";
            default:
                return "unknown " + arch;

        }
    }

    /**
     * writes data to file at specified position, if data length is less than section than empty space is filled with zero
     */
    public void writeSectionToFile(int filePosition, byte[] bytes, int sectionSize) throws IOException {
        executableFile.seek(filePosition);
        executableFile.write(bytes);
        for (int i = bytes.length; i < sectionSize; i++)
            executableFile.writeByte(0);
    }


    public static class ArchSlice {
        private final int sliceFileOffset;
        private final FileByteBuffer reader;
        private final int cputype;
        private final int cpusubtype;
        private final int sliceSize;
        private final int codeSignOffset;
        private final int codeSignSize;

        public ArchSlice(int sliceFileOffset, FileByteBuffer reader, int cputype, int cpusubtype, int sliceSize, int codeSignOffset, int codeSignSize) {
            this.sliceFileOffset = sliceFileOffset;
            this.reader = reader;
            this.cputype = cputype;
            this.cpusubtype = cpusubtype;
            this.sliceSize = sliceSize;
            this.codeSignOffset = codeSignOffset;
            this.codeSignSize = codeSignSize;
        }

        public int cpuType() {
            return cputype;
        }

        public int sliceFileOffset() {
            return sliceFileOffset;
        }

        public int codeSignOffset() {
            return codeSignOffset;
        }

        public int codeSignSize() {
            return codeSignSize;
        }

        public FileByteBuffer archBytes() {
            return reader;
        }

        public FileByteBuffer codeSignBytes() {
            if (codeSignSize <= 0)
                return null;

            reader.position(codeSignOffset);
            FileByteBuffer codeSignReader = reader.slice();
            codeSignReader.limit(codeSignSize);
            codeSignReader.order(ByteOrder.BIG_ENDIAN);
            return codeSignReader;
        }

        public int sliceSize() {
            return sliceSize;
        }
    }
}
