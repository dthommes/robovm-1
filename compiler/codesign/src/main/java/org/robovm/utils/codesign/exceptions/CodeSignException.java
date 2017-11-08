package org.robovm.utils.codesign.exceptions;

/**
 * Common exception related to codesign activities
 */
public class CodeSignException extends RuntimeException {
    public CodeSignException(String message) {
        super(message);
    }

    public CodeSignException(String message, Throwable cause) {
        super(message, cause);
    }
}
