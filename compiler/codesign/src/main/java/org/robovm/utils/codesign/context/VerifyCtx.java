package org.robovm.utils.codesign.context;


import java.io.PrintStream;

/**
 * Context for signature verification
 */
public class VerifyCtx extends Ctx{

    public VerifyCtx(PrintStream out) {
        super(out, "");
    }

    private VerifyCtx(VerifyCtx other, String indent) {
        super(other.out, indent);
    }

    @SuppressWarnings("unchecked")
    @Override
    public VerifyCtx push() {
        return new VerifyCtx(this, "|   " + indent);
    }
}
