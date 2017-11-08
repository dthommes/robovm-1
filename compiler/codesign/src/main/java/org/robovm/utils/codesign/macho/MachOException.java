package org.robovm.utils.codesign.macho;

/**
 * Exception for mach-o operations
 */
public class MachOException extends Exception {
    public MachOException() {
    }

    public MachOException(String message) {
        super(message);
    }

    public MachOException(String message, Throwable cause) {
        super(message, cause);
    }
}
