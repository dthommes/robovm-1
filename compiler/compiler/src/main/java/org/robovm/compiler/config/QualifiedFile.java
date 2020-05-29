package org.robovm.compiler.config;

import org.simpleframework.xml.Text;

import java.io.File;

/**
 * Container for file entry with platform/arch constraints
 */
public final class QualifiedFile extends AbstractQualified {
    @Text
    File entry;

    protected QualifiedFile() {
    }

    public QualifiedFile(File file) {
        entry = file;
    }

    public File getEntry() {
        return entry;
    }

    @Override
    public String toString() {
        return entry + " " + super.toString();
    }
}
