package org.robovm.utils.codesign.macho;

import org.robovm.utils.codesign.context.VerifyCtx;
import org.robovm.utils.codesign.utils.ByteBufferUtils;
import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * contains list of blob-indexes, refer OSX/sec/Security/Tool/codesign.c for details
 *
 */

public class SuperBlob extends Blob {

    /** type of entry to be used as type field in superblob */
    public static final class Type {
        // These values are potentially present in the CodeDirectory hash array
        // under their negative values. They are also used in APIs and SuperBlobs.
        // Note that zero must not be used for these (it's page 0 of the main code array),
        // and it is important to assign contiguous (very) small values for them.
        public static final int Requirements = 2;
        public static final int Entitlement = 5;
        public static final int CodeDirectory = 0;
        public static final int AlternateCodeDirectory = 0x1000;
        public static final int Signature = 0x10000;
    }

    // blob items
    private List<Blob> items = new ArrayList<>();
    // and their types
    private List<Integer> itemTypes = new ArrayList<>();

    public SuperBlob(int magic) {
        super(magic);
    }

    /**
     * Constructor from reader, e.g. reading from existing code-signature
     */
    public SuperBlob(VerifyCtx ctx, int magic, FileByteBuffer reader) {
        super(magic);

        // read following structure
        //typedef struct __SuperBlob {
        //    uint32_t magic;				/* magic number */
        //    uint32_t length;				/* total length of SuperBlob */
        //    uint32_t count;				/* number of index entries following */
        //    CS_BlobIndex index[];			/* (count) entries */
        //   /* followed by Blobs in no particular order as indicated by offsets in index */
        //} CS_SuperBlob;

        // read entries
        reader.position(8); // skip magic and length
        int count = reader.getInt();

        // now read number of indexes, structure is following
        //typedef struct __BlobIndex {
        //    uint32_t type;					/* type of entry */
        //    uint32_t offset;				/* offset of entry */
        //} CS_BlobIndex;
        for (int idx = 0; idx < count; idx++) {
            int type = reader.getInt();
            int offset = reader.getInt();
            int savedPos = reader.position();

            // read out other items
            reader.position(offset);
            int itemMagic = reader.getInt();
            int itemSize = reader.getInt();

            // slice into sub-reader
            reader.position(offset);
            FileByteBuffer subReader = reader.slice();
            subReader.limit(itemSize);

            Blob item = parseItem(ctx, type, itemMagic, subReader);
            if (item != null) {
                items.add(item);
                itemTypes.add(type);
            }

            // restore pos
            reader.position(savedPos);
        }
    }


    public List<Blob> getBlobs() {
        return items;
    }

    @SuppressWarnings("unchecked")
    public <T extends Blob> T getBlob(int blobType) {
        for (Blob blob : getBlobs())
            if (blob.magic == blobType)
                return (T) blob;
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Blob> List<T> getBlobs(int blobType) {
        List<Blob> result = new ArrayList<>();
        for (Blob blob : getBlobs())
            if (blob.magic == blobType)
                result.add(blob);
        return result.size() == 0 ? null : (List<T>) result;
    }

    protected Blob parseItem(VerifyCtx ctx, int type, int magic, FileByteBuffer reader) {

        ctx.debug("BLOB " + magicToString(magic));
        VerifyCtx itemCtx = ctx.push();

        switch (magic) {
            case Blob.Magic.REQUIREMENTS:
                return new RequirementsBlob(itemCtx, magic, reader);
            case Blob.Magic.CODEDIRECTORY:
                return new CodeDirectoryBlob(itemCtx, magic, reader);
            case Blob.Magic.ENTITLEMENT:
                return new EntitlementBlob(itemCtx, magic, reader);
            case Blob.Magic.BLOBWRAPPER:
                return new BlobWrapper(itemCtx, magic, reader);
//            case CSConsts.magic.DETACHED_SIGNATURE:
//                return new SuperBlob(magic, reader);
//            case CSConsts.magic.CODE_SIGN_DRS:
//                return new SuperBlob(magic, reader);
            default: throw new RuntimeException("Unexpected magic 0x" + Integer.toHexString(magic));
        }
    }

    /**
     * separate builder class as during adding the item it is required to consider item type that is stored along with item
     *
     */
    protected static class Builder {
        public final SuperBlob result;

        public Builder(SuperBlob result) {
            this.result = result;
        }

        public Builder addItem(int type, Blob item) {
            this.result.items.add(item);
            this.result.itemTypes.add(type);
            return this;
        }

        public <T extends SuperBlob> T build() {

            // build binary representation
            ByteBufferUtils.Writer writer = new ByteBufferUtils.Writer();

            // data offset -- where data will start, right after table
            int dataOffset = (4 + 4 + 4) + (4 + 4) * result.items.size(); // (magic + length + count) + (type + offset) * count

            // header
            writer.putInt(result.getMagic());
            writer.putInt(0); // size -- will get back here
            writer.putInt(result.items.size()); // count
            int headerPos = writer.position();
            // dumping everything
            for (int idx = 0; idx < result.items.size(); idx++) {
                // writing header, type and offset
                writer.putInt(result.itemTypes.get(idx));
                writer.putInt(dataOffset);

                // dump blob bytes
                writer.positionEx(dataOffset);
                byte[] blobData = result.items.get(idx).rawBytes();
                writer.put(blobData);
                dataOffset += (blobData.length + 3) & ~3; // and align by 4

                headerPos += 8;
                writer.position(headerPos);
            }

            // write total size - it is known now
            writer.position(4);
            writer.putInt(writer.limit());

            // save as raw bytes
            result.setBlobData(writer.rawByteBuffer());

            //noinspection unchecked
            return (T) result;
        }
    }
}
