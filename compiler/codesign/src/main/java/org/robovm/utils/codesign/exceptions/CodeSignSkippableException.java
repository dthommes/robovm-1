package org.robovm.utils.codesign.exceptions;

/**
 * Exception that can be skipped in case of "skip error" is enabled in context
 */
public class CodeSignSkippableException extends CodeSignException{
    public CodeSignSkippableException(String message) {
        super(message);
    }

    public CodeSignSkippableException(String message, Throwable cause) {
        super(message, cause);
    }
}
