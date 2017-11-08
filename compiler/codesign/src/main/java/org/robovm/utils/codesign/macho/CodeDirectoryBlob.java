package org.robovm.utils.codesign.macho;

import org.robovm.utils.codesign.context.VerifyCtx;
import org.robovm.utils.codesign.exceptions.CodeSignException;
import org.robovm.utils.codesign.utils.ByteBufferUtils;
import org.robovm.utils.codesign.utils.DiggestAlgorithm;
import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Code directory blob as described in OSX/libsecurity_codesigning/lib/codedirectory.h
 */
public class CodeDirectoryBlob extends Blob {
    private int version;
    private byte[] specialSlotHashes;
    private int nSpecialSlots;
    private byte[] codeSlotHashes;
    private int codeLimit;
    private int nCodeSlots;
    private DiggestAlgorithm diggestAlgorithm;
    private byte pageSizePow;
    private String ident;
    private String teamId;

    public static final class Version {
        public static final int currentVersion = 0x20200;       // "version 2.2"
        public static final int compatibilityLimit = 0x2F000;   // "version 3 with wiggle room"
        public static final int earliestVersion = 0x20001;      // earliest supported version
        public static final int supportsScatter = 0x20100;      // first version to support scatter option
        public static final int supportsTeamID = 0x20200;       // first version to support team ID option
        public static final int supportsCodeLimit64 = 0x20300;  // first version to support codeLimit64
    }

    public enum SpecialSlot {
        InfoSlot(1),         // Info.plist
        RequirementsSlot(2), // internal requirements
        ResourceDirSlot(3),  // resource directory
        TopDirectorySlot(4), // Application specific slot
        EntitlementSlot(5),  // embedded entitlement configuration
        RepSpecificSlot(6);  // for use by disk rep

        public final int offset;
        SpecialSlot(int v) {
            this.offset = v;
        }

        public static SpecialSlot fromValue(int v) {
            if (v > 0 && v <= SpecialSlot.values().length)
                return SpecialSlot.values()[v - 1];
            return null;
        }
    }

    private CodeDirectoryBlob() {
        super(Blob.Magic.CODEDIRECTORY);
    }

    /**
     * private constructor for builder
     * @param ctx
     * @param magic
     * @param reader
     */
    public CodeDirectoryBlob(VerifyCtx ctx, int magic, FileByteBuffer reader) {
        super(magic);

        // save blob data as it will be used for code sign testing
        setBlobData(reader);

        // special slots
        final String[] specialSlotsName = new String[]{"cdInfoSlot", "cdRequirementsSlot", "cdResourceDirSlot", "cdTopDirectorySlot", "cdEntitlementSlot"};

        reader.position(8); // skip magic and length

        // read fields that correspond the structure
        //class CodeDirectory: public Blob<CodeDirectory, kSecCodeMagicCodeDirectory> {
        //public:
        //    Endian<uint32_t> version;		// compatibility version
        version = reader.getInt();
        //    Endian<uint32_t> flags;		// setup and mode flags
        int flags = reader.getInt();
        //    Endian<uint32_t> hashOffset;	// offset of hash slot element at index zero
        int hashOffset = reader.getInt();
        //    Endian<uint32_t> identOffset;	// offset of identifier string
        int identOffset = reader.getInt();
        //    Endian<uint32_t> nSpecialSlots;	// number of special hash slots
        nSpecialSlots = reader.getInt();
        //    Endian<uint32_t> nCodeSlots;	// number of ordinary (code) hash slots
        nCodeSlots = reader.getInt();
        //    Endian<uint32_t> codeLimit;	// limit to main image signature range
        codeLimit = reader.getInt();
        //    uint8_t hashSize;			// size of each hash digest (bytes)
        byte hashSize = reader.get();
        //    uint8_t hashType;			// type of hash (kSecCodeSignatureHash* constants)
        byte hashType = reader.get();
        //    uint8_t platform;			// platform identifier; zero if not platform binary
        byte platform = reader.get();					/* unused (must be zero) */
        //    uint8_t	pageSizePow;		// log2(page size in bytes); 0 => infinite
        pageSizePow = reader.get();
        //    Endian<uint32_t> spare2;		// unused (must be zero)
        long spare2 = reader.getInt();
        //    Endian<uint32_t> scatterOffset;	// offset of optional scatter vector (zero if absent)
        long scatterOffset = 0;
        if (version >= Version.supportsScatter)
            scatterOffset = reader.getInt();
        //    Endian<uint32_t> teamIDOffset;	// offset of optional teamID string
        int teamIDOffset = 0;
        if (version >= Version.supportsTeamID)
            teamIDOffset = reader.getInt();
        // TODO: not covering codeLimit64 now
        //    Endian<uint32_t> spare3;		// unused (most be zero)
        //    Endian<uint64_t> codeLimit64; // limit to main image signature range, 64 bits
        //}


        // read zero terminated strings
        ident = ByteBufferUtils.readStringZ(reader, identOffset);
        teamId = (teamIDOffset> 0) ? ByteBufferUtils.readStringZ(reader, teamIDOffset) : null;

        // get hash type
        diggestAlgorithm = DiggestAlgorithm.fromCodeDirValue(hashType);
        if (diggestAlgorithm == null || diggestAlgorithm.hashSize != hashSize)
            throw new CodeSignException("Invalid hashType=" + hashType + " and hashSize=" + hashSize);

        // get hashes
        if (nSpecialSlots != 0) {
            // sanity
            if (nSpecialSlots < 0 || nSpecialSlots > SpecialSlot.values().length)
                throw new CodeSignException("Unexpected nSpecialSlots=" + nSpecialSlots);
            // allocate as much as possible
            specialSlotHashes = new byte[SpecialSlot.values().length * diggestAlgorithm.hashSize];
            // but copy only available, align end of the array
            reader.position(hashOffset - nSpecialSlots * hashSize);
            ByteBufferUtils.readBytes(reader, specialSlotHashes, specialSlotHashes.length - nSpecialSlots * hashSize, nSpecialSlots * hashSize);
        } else {
            specialSlotHashes = null;
        }
        if (nCodeSlots != 0) {
            reader.position(hashOffset);
            codeSlotHashes = ByteBufferUtils.readBytes(reader, nCodeSlots * hashSize);
        } else {
            codeSlotHashes = null;
        }

        ctx.debug("version 0x" + Integer.toHexString(version));
        ctx.debug("flags " + flags );
        ctx.debug("hashOffset " + hashOffset );
        ctx.debug("identOffset " + identOffset + ": " + ident);
        ctx.debug("nSpecialSlots " + nSpecialSlots );
        ctx.debug("nCodeSlots " + nCodeSlots );
        ctx.debug("codeLimit " + codeLimit );
        ctx.debug("hashType " + hashType );
        ctx.debug("hashSize " + hashSize );
        ctx.debug("platform " + platform );
        ctx.debug("pageSize " + pageSizePow);
        ctx.debug("spare2 " + spare2 );
        ctx.debug("scatterOffset " + scatterOffset);
        ctx.debug("teamIDOffset " + teamIDOffset + ": " + teamId);

        if (nSpecialSlots != 0) {
            ctx.debug("Special hashes:");
            VerifyCtx specialSlotsCtx = ctx.push();
            byte[] sign = new byte[hashSize];
            for (int idx = 0; idx < nSpecialSlots; idx++) {
                System.arraycopy(specialSlotHashes, specialSlotHashes.length - (idx + 1) * hashSize, sign, 0, hashSize);
                String slotName = specialSlotsName[idx];
                specialSlotsCtx.debug(slotName + " (" + (- (idx + 1)) + "): " + ByteBufferUtils.byteArrayToHex(sign));
            }
        }
    }


    private byte[] buildBinary() {
        ByteBufferUtils.Writer writer = new ByteBufferUtils.Writer();
        writer.order(ByteOrder.BIG_ENDIAN);

        // write magic and size template
        writer.putInt( magic);
        writer.putInt(0);         /* offset of hash slot element at index zero */

        // lets dump values, up to structure
        //class CodeDirectory: public Blob<CodeDirectory, kSecCodeMagicCodeDirectory> {
        //public:
        //    Endian<uint32_t> version;	        // compatibility version
        writer.putInt( version);

        //    Endian<uint32_t> flags;           // setup and mode flags
        writer.putInt( 0);

        //    Endian<uint32_t> hashOffset;      // offset of hash slot element at index zero
        int hashOffsetPos = writer.position();
        writer.putInt( 0);

        //    Endian<uint32_t> identOffset;     // offset of identifier string
        int identOffsetPos = writer.position();
        writer.putInt( 0);

        //    Endian<uint32_t> nSpecialSlots;   // number of special hash slots
        writer.putInt( nSpecialSlots);

        //    Endian<uint32_t> nCodeSlots;      // number of ordinary (code) hash slots
        writer.putInt( nCodeSlots);			/* number of ordinary (code) hash slots */

        //    Endian<uint32_t> codeLimit;       // limit to main image signature range
        writer.putInt( codeLimit);

        //    uint8_t hashSize;	                // size of each hash digest (bytes)
        writer.put((byte) diggestAlgorithm.hashSize);

        //    uint8_t hashType;                 // type of hash (kSecCodeSignatureHash* constants)
        writer.put((byte) diggestAlgorithm.hashType);

        //    uint8_t platform;                 // platform identifier; zero if not platform binary
        writer.put((byte) 0);

        //    uint8_t	pageSizePow;               // log2(page size in bytes); 0 => infinite
        writer.put(pageSizePow);          /* log2(page size in bytes); 0 => infinite */

        //    Endian<uint32_t> spare2;          // unused (must be zero)
        writer.putInt( 0);

        //    Endian<uint32_t> scatterOffset;   // offset of optional scatter vector (zero if absent)
        if (version >= Version.supportsScatter)
            writer.putInt( 0);   // not used for now

        //    Endian<uint32_t> teamIDOffset;    // offset of optional teamID string
        int teamIdOffsetPos = 0;
        if (version >= Version.supportsTeamID) {
            teamIdOffsetPos = writer.position();
            writer.putInt( 0); // will write it later
        }

        // TODO: version set ot 0x20200 so these are not required
        //    Endian<uint32_t> spare3;          // unused (most be zero)
        //    Endian<uint64_t> codeLimit64;     // limit to main image signature range, 64 bits
        //}

        // dump identity
        int identPos = writer.position();
        writer.writeStringZ(ident);

        // dump teamId
        int teamIdPos = writer.position();
        if (version >= Version.supportsTeamID)
            writer.writeStringZ(teamId);

        // dump special hashes
        int hashDataPos = writer.position();
        int hashSize = diggestAlgorithm.hashSize;
        if (nSpecialSlots != 0) {
            writer.put(specialSlotHashes, specialSlotHashes.length - nSpecialSlots * hashSize, nSpecialSlots * hashSize);
            hashDataPos = writer.position();
        }
        // dump code hashes
        if (nCodeSlots > 0)
            writer.put(codeSlotHashes);

        // update offsets
        writer.position(identOffsetPos);
        writer.putInt(identPos);
        if (teamIdOffsetPos > 0) {
            writer.position(teamIdOffsetPos);
            writer.putInt(teamIdPos);
        }
        writer.position(hashOffsetPos);
        writer.putInt(hashDataPos);

        // now write size after magic
        writer.position(4);
        writer.putInt(writer.limit());

        // return as byte array
        writer.position(0);
        return writer.readBytes(writer.limit());
    }

    public byte[] getCodeSlotHash(int idx) {
        byte[] result = new byte[diggestAlgorithm.hashSize];
        System.arraycopy(codeSlotHashes, idx * diggestAlgorithm.hashSize, result, 0, diggestAlgorithm.hashSize);
        return result;
    }

    public int getCodeLimit() {
        return codeLimit;
    }

    public int getCodeSlotCount() {
        return nCodeSlots;
    }

    public int getSpecialSlotCount() {
        return nSpecialSlots;
    }

    public byte getPageSizePow() {
        return pageSizePow;
    }

    public byte[] hash(byte[] data) {
        return diggestAlgorithm.hash(data);
    }

    public byte[] hash(File f) {
        return diggestAlgorithm.hash(f);
    }

    public byte[] getSpecialSlotHash(SpecialSlot slotNo) {
        byte[] result = new byte[diggestAlgorithm.hashSize];
        System.arraycopy(specialSlotHashes, specialSlotHashes.length - slotNo.offset * diggestAlgorithm.hashSize,
                result, 0, diggestAlgorithm.hashSize);
        return result;
    }


    public static class Builder {
        private final boolean isEstimate;
        private DiggestAlgorithm diggestAlgorithm;
        private byte[] specialSlotHashes;
        private int nSpecialSlots;
        private int nCodeSlots;
        private byte[] codeSlotHashes;
        private int codeLimit;
        private String ident;
        private String teamId;
        private int pageSizePow;

        public Builder(DiggestAlgorithm diggestAlgorithm, String ident, String teamId, boolean itsEstimate) {
            this.isEstimate = itsEstimate;
            this.diggestAlgorithm = diggestAlgorithm;
            this.ident = ident;
            this.teamId = teamId;
            pageSizePow = 12; // default page size is 2^12 = 4096
        }

        CodeDirectoryBlob build() {
            CodeDirectoryBlob result = new CodeDirectoryBlob();
            result.version = Version.currentVersion;
            result.ident = this.ident;
            result.teamId = this.teamId;
            result.diggestAlgorithm = diggestAlgorithm;
            result.pageSizePow = (byte) pageSizePow;

            if (specialSlotHashes != null) {
                result.specialSlotHashes = specialSlotHashes;
                result.nSpecialSlots = nSpecialSlots;
            }

            if (codeSlotHashes != null) {
                result.codeLimit = codeLimit;
                result.codeSlotHashes = codeSlotHashes;
                result.nCodeSlots = nCodeSlots;
            }

            result.setBlobData(result.buildBinary());

            return result;
        }

        public Builder calculateCodeHash(FileByteBuffer reader, int codeLimit) {
            reader.position(0);

            // calculate hash for all code
            int pageSize = 1 << pageSizePow;
            this.codeLimit = codeLimit;
            nCodeSlots = (codeLimit + pageSize - 1) / pageSize;
            codeSlotHashes = new byte[nCodeSlots * diggestAlgorithm.hashSize];

            // skip calculating real hashes if it is estimate run
            if (!isEstimate) {
                int pos = 0;
                int hashPos = 0;
                byte[] page = null;
                while (pos < codeLimit) {
                    int buffLen = codeLimit - pos;
                    if (buffLen > pageSize)
                        buffLen = pageSize;

                    // get code page
                    if (page == null || page.length != buffLen)
                        page = new byte[buffLen];
                    reader.get(page);

                    // calculate hash for page and save it
                    byte[] hash = diggestAlgorithm.hash(page);
                    System.arraycopy(hash, 0, codeSlotHashes, hashPos, diggestAlgorithm.hashSize);

                    pos += pageSize;
                    hashPos += diggestAlgorithm.hashSize;
                }
            }

            return this;
        }

        public Builder calculateSpecialSlotHash(SpecialSlot slot, byte[] slotData) {
            byte[] hash =  isEstimate ? new byte[diggestAlgorithm.hashSize] : diggestAlgorithm.hash(slotData);
            setSpecialSlotHash(slot, hash);
            return this;
        }

        public Builder calculateSpecialSlotHash(SpecialSlot slot, File file) {
            byte[] hash = isEstimate ? new byte[diggestAlgorithm.hashSize] : diggestAlgorithm.hash(file);
            setSpecialSlotHash(slot, hash);
            return this;
        }

        private void setSpecialSlotHash(SpecialSlot slot, byte[] hash) {
            if (specialSlotHashes == null)
                specialSlotHashes = new byte[SpecialSlot.values().length * diggestAlgorithm.hashSize];
            // use diggestAlgorithm.size instead hash.length as it can be shorted in case of HashSHA256Truncated
            System.arraycopy(hash, 0 , specialSlotHashes, specialSlotHashes.length - diggestAlgorithm.hashSize * slot.offset,
                    diggestAlgorithm.hashSize);
            if (nSpecialSlots < slot.offset)
                nSpecialSlots = slot.offset;
        }
    }
}
