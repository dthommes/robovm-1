package org.robovm.utils.codesign;

import org.robovm.utils.codesign.bundle.Bundle;
import org.robovm.utils.codesign.context.SignCtx;
import org.robovm.utils.codesign.utils.P12Certificate;

import java.io.File;

public class CodeSign {

    /** entry point for signing */
    public static void sign(File target, P12Certificate cert, byte[] entitlements, String codesignAllocate) {
        Bundle s = Bundle.bundleForPath(target);
        SignCtx ctx = new SignCtx(System.out, cert, codesignAllocate, entitlements);
        s.sign(ctx);
    }
}
