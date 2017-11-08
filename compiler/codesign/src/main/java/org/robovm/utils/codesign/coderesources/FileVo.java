package org.robovm.utils.codesign.coderesources;

import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.Map;

/**
 * Simple reimplementation of ResourceBuilder, in the Apple Open Source
 * file bundlediskrep.cpp
 */
class FileVo {
    String name;
    byte[] hash;
    byte[] hash2;
    boolean optional;

    public FileVo(String name, Object properties) {
        this.name = name;
        if (properties instanceof NSData) {
            hash = ((NSData) properties).bytes();
        } else if (properties instanceof Map) {
            //noinspection Java8MapForEach,unchecked
            for (Map.Entry<String, Object> e : ((Map<String, Object>) properties).entrySet()) {
                String key = e.getKey();
                Object value = e.getValue();
                if ("optional".equals(key))
                    this.optional = ((NSNumber) value).boolValue();
                else if ("hash".equals(key))
                    this.hash = ((NSData) value).bytes();
                else if ("hash2".equals(key))
                    this.hash2 = ((NSData) value).bytes();
            }
        }
    }

    public FileVo(String name, byte[] hash1, byte[] hash2, boolean optional) {
        this.name = name;
        this.hash = hash1;
        this.hash2 = hash2;
        this.optional = optional;
    }


    /**
     * Compares file against reference on
     * @param v2 true if it is version2 comparision, e.g. compare hash2
     */
    boolean compare(FileVo referenceFile, boolean v2) {
        if (this.optional != referenceFile.optional)
            return false;
        if (!Arrays.equals(this.hash, referenceFile.hash))
            return false;
        if (v2 && !Arrays.equals(this.hash2, referenceFile.hash2))
            return false;

        // matches
        return true;
    }

    /**
     * makes an entry about this file in plist
     * @param v2 true if it is version2 slice, e.g. contains hash2
     */
    void writeToPlist(NSDictionary plist, boolean v2) {
        NSObject obj;
        if (v2) {
            NSDictionary data = new NSDictionary();
            data.put("hash", new NSData(hash));
            data.put("hash2", new NSData(hash2));
            if (optional)
                data.put("optional", NSNumber.wrap(true));
            obj = data;
        } else {
            // v1
            if (optional) {
                NSDictionary data = new NSDictionary();
                data.put("hash", new NSData(hash));
                data.put("optional", NSNumber.wrap(true));
                obj = data;
            } else obj = new NSData(hash);
        }

        // save
        plist.put(name, obj);
    }

    public static NSDictionary toPlist(FileVo[] files, boolean v2) {
        NSDictionary plist = new NSDictionary();
        for (FileVo f : files)
            f.writeToPlist(plist, v2);
        return plist;
    }

    @Override
    public String toString() {
        return "FileVo{" +
                "name='" + name + '\'' +
                ((hash != null )? (", hash=" + Hex.toHexString(hash)) : "") +
                ((hash2 != null) ? (", hash2=" + Hex.toHexString(hash2)) : "") +
                (optional ? ", optional=" + optional : "") +
                '}';
    }
}

