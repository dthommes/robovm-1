package org.robovm.utils.codesign.context;

import org.robovm.utils.codesign.exceptions.CodeSignException;
import org.robovm.utils.codesign.exceptions.CodeSignSkippableException;

import java.io.PrintStream;

/**
 * base context functionality that is used by both verification and signature
 */
public abstract class Ctx {
    protected final String indent;
    protected final PrintStream out;

    Ctx(PrintStream out, String intent) {
        this.out = out;
        this.indent = intent;
    }

    public void debug(Object s) {
        print(s.toString());
    }

    public void info(Object s) {
        print(s.toString());
    }

    public void error(Object s) {
        print(s.toString());
    }

    public void error(String s, Throwable e) {
        print(s);
    }

    public void onError(CodeSignException e) {
        throw e;
    }

    public void onError(CodeSignSkippableException e) {
        // skip for now
        print("FAILURE: " + e.getMessage(), "");
        print("", "");
    }

    public abstract <T extends Ctx> T push();

    private void print(String s) {
        print(s, indent);
    }

    private void print(String s, String ind) {
        for (String l : s.split("\n"))
            out.println(ind + l);
    }
}
