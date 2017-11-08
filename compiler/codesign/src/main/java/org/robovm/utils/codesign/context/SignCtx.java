package org.robovm.utils.codesign.context;

import org.robovm.utils.codesign.utils.P12Certificate;

import java.io.PrintStream;

/**
 * Context for signing package
 */
public class SignCtx extends Ctx{

    private final P12Certificate certificate;
    private String identifier;
    private String codesignAllocatePath;
    private byte[] entitlements;

    public SignCtx(PrintStream out, P12Certificate certificate, String codesignAllocatePath, byte[] entitlements) {
        super(out, "");
        this.certificate = certificate;
        if (codesignAllocatePath == null)
            this.codesignAllocatePath = System.getenv("CODESIGN_ALLOCATE");
        else
            this.codesignAllocatePath = codesignAllocatePath;
        this.entitlements = entitlements;
    }

    private SignCtx(SignCtx other, String indent) {
        super(other.out, indent);
        certificate = other.certificate;
        identifier = other.identifier;
        codesignAllocatePath = other.codesignAllocatePath;
        entitlements = other.entitlements;
    }

    public P12Certificate getCertificate() {
        return certificate;
    }

    public String getIdentifier() {
        return identifier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SignCtx push() {
        return new SignCtx(this, "|   " + indent);
    }

    public String getCodesignAllocatePath() {
        return codesignAllocatePath;
    }

    public byte[] getEntitlements() {
        return entitlements;
    }

    public SignCtx forApp(String identifier) {
        SignCtx res = new SignCtx(this, indent);
        res.identifier = identifier;
        return res;
    }
}
