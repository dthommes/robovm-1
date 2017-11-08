package org.robovm.utils.codesign.macho.Requirements;

import org.ietf.jgss.Oid;
import org.robovm.utils.codesign.exceptions.CodeSignException;
import org.robovm.utils.codesign.utils.ByteBufferUtils;
import org.robovm.utils.codesign.utils.FileByteBuffer;

import java.nio.ByteBuffer;

/**
 * represent rule expression operations
 * allows both to parse from ByteBuffer and build own
 */
public abstract class Expression {

    // unconditionally false
    public static Expression opFalse() {
        return new SingleStatementExpression(opFalse, "false");
    }

    // unconditionally true
    public static Expression opTrue() {
        return new SingleStatementExpression(opTrue, "true");
    }

    // match canonical code [string]
    public static Expression opIdent(String ident) {
        return new OneStringExpression(opIdent, "identifier", ident);
    }

    // signed by Apple as Apple's product
    public static Expression opAppleAnchor() {
        return new SingleStatementExpression(opAppleAnchor, "anchor apple");
    }

    // match anchor [cert hash]
    public static Expression opAnchorHash(int slot, byte[] hash) {
        return new CertificateHashExpression(opAnchorHash, slot, hash);
    }

    // *legacy* - use opInfoKeyField [key; value]
    public static Expression opInfoKeyValue(String key, String value) {
        return new SimpleKeyEqualValueExpression(opInfoKeyField, "info", key, value);
    }

    // binary prefix expr AND expr [expr; expr]
    public static Expression opAnd(Expression one, Expression two) {
        return new TwoExpExpression(opAnd, "and", one, two, slAnd);
    }

    // binary prefix expr OR expr [expr; expr]
    public static Expression opOr(Expression one, Expression two) {
        return new TwoExpExpression(opOr, "or", one, two, slOr);
    }

    // match hash of CodeDirectory directly [cd hash]
    public static Expression opCDHash(byte[] hash) {
        return new OneByteArrayExpression(opCDHash, "cdhash", hash);
    }

    // logical inverse [expr]
    public static Expression opNot(Expression exp) {
        return new OneExpExpression(opNot, "!", exp);
    }

    // Info.plist key field [string; match suffix]
    public static Expression opInfoKeyField(String field, Match match) {
        return new SimpleKeyMatchExpression(opInfoKeyField, "info", field, match);
    }

    // Certificate field [cert index; field name; match suffix]
    public static Expression opCertField(int slot, String field, Match match) {
        return new CertificateExpression(opCertField, slot, field, null, match);
    }

    // require trust settings to approve one particular cert [cert index]
    public static Expression opTrustedCert(int slot) {
        return new CertificateTrustedExpression(opTrustedCert, slot);
    }

    // require trust settings to approve the cert chain
    public static Expression opTrustedCerts() {
        return new SingleStatementExpression(opTrustedCerts, "anchor trusted");
    }

    // Certificate component by OID [cert index; oid; match suffix]
    public static Expression opCertGeneric(int slot, Oid oid, Match match) {
        return new CertificateExpression(opCertGeneric, slot, "field", oid, match);
    }

    public static Expression opAppleGenericAnchor() {
        return new SingleStatementExpression(opAppleGenericAnchor, "anchor apple generic");
    }

    // entitlement dictionary field [string; match suffix]
    public static Expression opEntitlementField(String field, Match match) {
        return new SimpleKeyMatchExpression(opEntitlementField, "entitlement", field, match);
    }

    // Certificate policy by OID [cert index; oid; match suffix]
    public static Expression opCertPolicy(int slot, Oid oid, Match match) {
        return new CertificateExpression(opCertPolicy, slot, "policy", oid, match);
    }

    // named anchor type
    public static Expression opNamedAnchor(String anchor) {
        return new OneStringExpression(opNamedAnchor, "anchor apple", anchor);
    }

    // named subroutine
    public static Expression opNamedCode(String subroutine) {
        return new OneStringExpression(opNamedCode, "(" + subroutine + ")", subroutine, true);
    }

    // platform constraint [integer]
    public static Expression opPlatform(int platform) {
        return new OneIntExpression(opPlatform, platform, "platform = ", null);
    }

    public abstract void write(ByteBufferUtils.Writer writer);


    //
    // implementation details bellow
    //

    // syntax level for toString ops
    private final static int slPrimary = 0;    // syntax primary
    private final static int slAnd = 1;        // conjunctive
    private final static int slOr = 2;         // disjunctive
    private final static int slTop = 3;         // where we start

    // opcode constants
    private final static int opFalse = 0;                   // unconditionally false
    private final static int opTrue = 1;                    // unconditionally true
    private final static int opIdent = 2;                   // match canonical code [string]
    private final static int opAppleAnchor = 3;             // signed by Apple as Apple's product
    private final static int opAnchorHash = 4;              // match anchor [cert hash]
    private final static int opInfoKeyValue = 5;            // *legacy* - use opInfoKeyField [key; value]
    private final static int opAnd = 6;                     // binary prefix expr AND expr [expr; expr]
    private final static int opOr = 7;                      // binary prefix expr OR expr [expr; expr]
    private final static int opCDHash = 8;                  // match hash of CodeDirectory directly [cd hash]
    private final static int opNot = 9;                     // logical inverse [expr]
    private final static int opInfoKeyField = 10;           // Info.plist key field [string; match suffix]
    private final static int opCertField = 11;              // Certificate field [cert index; field name; match suffix]
    private final static int opTrustedCert = 12;            // require trust settings to approve one particular cert [cert index]
    private final static int opTrustedCerts = 13;           // require trust settings to approve the cert chain
    private final static int opCertGeneric = 14;            // Certificate component by OID [cert index; oid; match suffix]
    private final static int opAppleGenericAnchor = 15;     // signed by Apple in any capacity
    private final static int opEntitlementField = 16;       // entitlement dictionary field [string; match suffix]
    private final static int opCertPolicy = 17;             // Certificate policy by OID [cert index; oid; match suffix]
    private final static int opNamedAnchor = 18;            // named anchor type
    private final static int opNamedCode = 19;              // named subroutine
    private final static int opPlatform = 20;               // platform constraint [integer]

    protected abstract String toString(int syntaxLevel);

    @Override
    public String toString() {
        return toString(slTop);
    }

    public static Expression read(FileByteBuffer reader) {
        int opCode = reader.getInt();
        switch (opCode) {
            case opFalse:
                return opFalse();

            case opTrue:
                return opTrue();

            case opIdent:
                return opIdent(ByteBufferUtils.readRequirementString(reader));

            case opAppleAnchor:
                return opAppleAnchor();

            case opAnchorHash:
                return opAnchorHash(reader.getInt(), ByteBufferUtils.readRequirementBytes(reader));

            case opInfoKeyValue:
                return opInfoKeyValue(ByteBufferUtils.readRequirementString(reader), ByteBufferUtils.readRequirementString(reader));

            case opAnd:
                return opAnd(read(reader), read(reader));

            case opOr:
                return opOr(read(reader), read(reader));

            case opCDHash:
                return opCDHash(ByteBufferUtils.readRequirementBytes(reader));

            case opNot:
                return opNot(read(reader));

            case opInfoKeyField:
                return opInfoKeyField(ByteBufferUtils.readRequirementString(reader), Match.read(reader));

            case opCertField:
                return opCertField(reader.getInt(), ByteBufferUtils.readRequirementString(reader), Match.read(reader));

            case opTrustedCert:
                return opTrustedCert(reader.getInt());

            case opTrustedCerts:
                return opTrustedCerts();

            case opCertGeneric:
                return opCertGeneric(reader.getInt(),ByteBufferUtils.readRequirementOid(reader), Match.read(reader));

            case opAppleGenericAnchor:
                return opAppleGenericAnchor();

            case opEntitlementField:
                return opEntitlementField(ByteBufferUtils.readRequirementString(reader), Match.read(reader));

            case opCertPolicy:
                return opCertPolicy(reader.getInt(), ByteBufferUtils.readRequirementOid(reader), Match.read(reader));

            case opNamedAnchor:
                return opNamedAnchor(ByteBufferUtils.readRequirementString(reader));

            case opNamedCode:
                return opNamedCode(ByteBufferUtils.readRequirementString(reader));

            case opPlatform:
                return opPlatform(reader.getInt());

            default:
                throw new CodeSignException("Unknown Requirement.Expression.opCode " + opCode);
        }
    }

    private static class SingleStatementExpression extends Expression {
        private final int opCode;
        private final String statement;

        SingleStatementExpression(int opCode, String statement) {
            this.opCode = opCode;
            this.statement = statement;
        }

        @Override
        public void write(ByteBufferUtils.Writer writer) {
            writer.putInt( opCode);
        }

        @Override
        protected String toString(int syntaxLevel) {
            return statement;
        }
    }

    /**
     * Expression with one expression operand
     */
    private static class OneExpExpression extends Expression {
        private final int opCode;
        private final String statement;
        private final Expression exr;

        OneExpExpression(int opCode, String statement, Expression exr) {
            this.opCode = opCode;
            this.statement = statement;
            this.exr = exr;
        }

        @Override
        public void write(ByteBufferUtils.Writer writer) {
            writer.putInt( opCode);
            exr.write(writer);
        }

        @Override
        protected String toString(int syntaxLevel) {
            return statement + " " + exr.toString(slPrimary);
        }
    }

    /**
     * Expression with one expression operand
     */
    private static class TwoExpExpression extends Expression {
        private final int opCode;
        private final String statement;
        private final Expression exr1;
        private final Expression exr2;
        private final int sl; // syntax level

        TwoExpExpression(int opCode, String statement, Expression exr1, Expression exr2, int sl) {
            this.opCode = opCode;
            this.statement = statement;
            this.exr1 = exr1;
            this.exr2 = exr2;
            this.sl = sl;
        }

        @Override
        public void write(ByteBufferUtils.Writer writer) {
            writer.putInt( opCode);
            exr1.write(writer);
            exr2.write(writer);
        }

        @Override
        protected String toString(int syntaxLevel) {
            return (syntaxLevel < sl ? "(" : "") +
                    exr1.toString(sl) + " " + statement + " " + exr2.toString(sl) +
                    (syntaxLevel < sl ? ")" : "");
        }
    }

    /**
     * Expression with one string operand, e.g. opIdent
     */
    private static class OneStringExpression extends Expression {
        private final int opCode;
        private final String statement;
        private final String exr;
        private final boolean statementAsToString;

        OneStringExpression(int opCode, String statement, String exr, boolean statementAsToString) {
            this.opCode = opCode;
            this.statement = statement;
            this.exr = exr;
            this.statementAsToString = statementAsToString;
        }

        OneStringExpression(int opCode, String statement, String exr) {
            this.opCode = opCode;
            this.statement = statement;
            this.exr = exr;
            this.statementAsToString = false;
        }

        @Override
        public void write(ByteBufferUtils.Writer writer) {
            writer.putInt( opCode);
            writer.writeRequirementString(exr);
        }

        @Override
        protected String toString(int syntaxLevel) {
            return statementAsToString ? statement  : statement + " " + exr;
        }
    }

    /**
     * Expression with one string operand, e.g. opIdent
     */
    private static class OneIntExpression extends Expression {
        private final int opCode;
        private final String prefix;
        private final String suffix;
        private final int exr;

        OneIntExpression(int opCode, int exr, String prefix, String suffix) {
            this.opCode = opCode;
            this.prefix = prefix;
            this.suffix = suffix;
            this.exr = exr;
        }

        @Override
        public void write(ByteBufferUtils.Writer writer) {
            writer.putInt( opCode);
            writer.putInt( exr);
        }

        @Override
        protected String toString(int syntaxLevel) {
            return prefix + exr + (suffix != null ? suffix : "");
        }
    }

    private static class SimpleKeyMatchExpression extends Expression {
        private final int opCode;
        private final String statement;
        private final String field;
        private final Match match;

        SimpleKeyMatchExpression(int opCode, String statement, String field, Match match) {
            this.opCode = opCode;
            this.statement = statement;
            this.field = field;
            this.match = match;
        }

        @Override
        public void write(ByteBufferUtils.Writer writer) {
            writer.putInt( opCode);
            writer.writeRequirementString(field);
            match.write(writer);
        }

        @Override
        protected String toString(int syntaxLevel) {
            return statement + "[" + field + "] " + match;
        }
    }

    private static class SimpleKeyEqualValueExpression extends Expression {
        private final int opCode;
        private final String statement;
        private final String key;
        private final String value;

        private SimpleKeyEqualValueExpression(int opCode, String statement, String key, String value) {
            this.opCode = opCode;
            this.statement = statement;
            this.key = key;
            this.value = value;
        }

        @Override
        public void write(ByteBufferUtils.Writer writer) {
            writer.putInt( opCode);
            writer.writeRequirementString(key);
            writer.writeRequirementString(value);
        }

        @Override
        protected String toString(int syntaxLevel) {
            return statement + "[" + key + "] = " + value;
        }
    }

    private static class CertificateExpression extends Expression {
        private final int opCode;
        private final int slot;
        private final String field;
        private final Oid oid;
        private final Match match;

        public CertificateExpression(int opCode, int slot, String field, Oid oid, Match match) {
            this.opCode = opCode;
            this.slot = slot;
            this.field = field;
            this.oid = oid;
            this.match = match;
        }

        @Override
        public void write(ByteBufferUtils.Writer writer) {
            writer.putInt( opCode);
            writer.putInt( slot);
            if (oid == null) {
                writer.writeRequirementString(field);
            } else {
                writer.writeRequirementOid(oid);
            }
            match.write(writer);
        }

        @Override
        protected String toString(int syntaxLevel) {
            return "certificate " + toCertSlot(slot) + "[" + field +
                    (oid != null ? ("." + oid) : "" ) +
                    "] " + match;
        }
    }

    private static class CertificateHashExpression extends Expression {
        private final int opCode;
        private final int slot;
        private final byte[] hash;

        public CertificateHashExpression(int opCode, int slot, byte[] hash) {
            this.opCode = opCode;
            this.slot = slot;
            this.hash = hash;
        }

        @Override
        public void write(ByteBufferUtils.Writer writer) {
            writer.putInt( opCode);
            writer.putInt( slot);
            writer.writeRequirementBytes(hash);
        }

        @Override
        protected String toString(int syntaxLevel) {
            return "certificate " + toCertSlot(slot) + " = " + "H\"" + ByteBufferUtils.byteArrayToHex(hash) + "\"";
        }
    }

    private static class OneByteArrayExpression extends Expression {
        private final int opCode;
        private final String statement;
        private final byte[] hash;

        public OneByteArrayExpression(int opCode, String statement, byte[] hash) {
            this.opCode = opCode;
            this.statement = statement;
            this.hash = hash;
        }

        @Override
        public void write(ByteBufferUtils.Writer writer) {
            writer.putInt( opCode);
            writer.writeRequirementBytes(hash);
        }

        @Override
        protected String toString(int syntaxLevel) {
            return statement + " " + "H\"" + ByteBufferUtils.byteArrayToHex(hash) + "\"";
        }
    }

    private static class CertificateTrustedExpression extends Expression {
        private final int opCode;
        private final int slot;

        CertificateTrustedExpression(int opCode, int slot) {
            this.opCode = opCode;
            this.slot = slot;
        }

        @Override
        public void write(ByteBufferUtils.Writer writer) {
            writer.putInt( opCode);
            writer.putInt( slot);
        }

        @Override
        protected String toString(int syntaxLevel) {
            return "certificate " + toCertSlot(slot) + " trusted";
        }
    }


    private static String toCertSlot(int slot) {
        if (slot == -1)
            return "root";
        else if (slot == 0)
            return "leaf";

        return Integer.toString(slot);
    }
}
