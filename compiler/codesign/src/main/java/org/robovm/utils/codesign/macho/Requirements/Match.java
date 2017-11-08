package org.robovm.utils.codesign.macho.Requirements;

import org.apache.commons.lang3.StringUtils;
import org.robovm.utils.codesign.exceptions.CodeSignException;
import org.robovm.utils.codesign.utils.ByteBufferUtils;
import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.nio.ByteBuffer;

/**
 * represent match suffix for rule expression
 */
public class Match {
    
    //
    // api
    // 

    // anything but explicit "false" - no value stored
    public static Match matchExists() {
        return new Match(matchExists, null, "/* exists */");
    }

    // equal (CFEqual)
    public static Match matchEqual(String data) {
        return new Match(matchEqual, data, " = ");
    }

    // partial match (substring)
    public static Match matchContains(String data) {
        return new Match(matchContains, data, " ~ ");
    }

    // partial match (initial substring)
    public static Match matchBeginsWith(String data) {
        return new Match(matchBeginsWith, data, " = ", "*");
    }

    // partial match (terminal substring)
    public static Match matchEndsWith(String data) {
        return new Match(matchEndsWith, data, " = *");
    }

    // less than (string with numeric comparison)
    public static Match matchLessThan(String data) {
        return new Match(matchLessThan, data, " < ");
    }

    // greater than (string with numeric comparison)
    public static Match matchGreaterThan(String data) {
        return new Match(matchGreaterThan, data, " >= ");
    }

    // less or equal (string with numeric comparison)
    public static Match matchLessEqual(String data) {
        return new Match(matchLessEqual, data, " <= ");
    }

    // greater or equal (string with numeric comparison)
    public static Match matchGreaterEqual(String data) {
        return new Match(matchGreaterEqual, data, " > ");
    }
    
    
    //
    // implementation details
    //
    private final static int matchExists = 0;       // anything but explicit "false" - no value stored
    private final static int matchEqual = 1;        // equal (CFEqual)
    private final static int matchContains = 2;     // partial match (substring)
    private final static int matchBeginsWith = 3;   // partial match (initial substring)
    private final static int matchEndsWith = 4;     // partial match (terminal substring)
    private final static int matchLessThan = 5;     // less than (string with numeric comparison)
    private final static int matchGreaterThan = 6;  // greater than (string with numeric comparison)
    private final static int matchLessEqual = 7;    // less or equal (string with numeric comparison)
    private final static int matchGreaterEqual = 8; // greater or equal (string with numeric comparison)


    // just fields
    private final int matchCode;
    private final String data;
    private final String prefix;
    private final String suffix;

    public Match(int matchCode, String data, String prefix, String suffix) {
        this.matchCode = matchCode;
        this.data = data;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public Match(int matchCode, String data, String prefix) {
        this.matchCode = matchCode;
        this.data = data;
        this.prefix = prefix;
        this.suffix = null;
    }

    public void write(ByteBufferUtils.Writer writer) {
        writer.putInt(matchCode);
        if (data != null) {
            writer.writeRequirementString(data);
        }
    }

    @Override
    public String toString() {
        String dataStr = data;
        if (dataStr != null && !StringUtils.isNumeric(dataStr))
            dataStr = "\"" + dataStr + "\"";
        return prefix + (dataStr != null ? dataStr : "") + (suffix != null ? suffix : "");
    }

    public static Match read(FileByteBuffer reader) {
        int matchCode = reader.getInt();
        String data = null;
        switch (matchCode) {
            case matchExists:
                // no data
                return Match.matchExists();

            case matchEqual:
                data = ByteBufferUtils.readRequirementString(reader);
                return Match.matchEqual(data);

            case matchContains:
                data = ByteBufferUtils.readRequirementString(reader);
                return Match.matchContains(data);

            case matchBeginsWith:
                data = ByteBufferUtils.readRequirementString(reader);
                return Match.matchBeginsWith(data);

            case matchEndsWith:
                data = ByteBufferUtils.readRequirementString(reader);
                return Match.matchEndsWith(data);

            case matchLessThan:
                data = ByteBufferUtils.readRequirementString(reader);
                return Match.matchLessThan(data);

            case matchGreaterEqual:
                data = ByteBufferUtils.readRequirementString(reader);
                return Match.matchGreaterEqual(data);

            case matchLessEqual:
                data = ByteBufferUtils.readRequirementString(reader);
                return Match.matchLessEqual(data);

            case matchGreaterThan:
                data = ByteBufferUtils.readRequirementString(reader);
                return Match.matchGreaterThan(data);

            default:
                throw new CodeSignException("Unknown Requirement.MatchCode " + matchCode);
        }
    }

}
