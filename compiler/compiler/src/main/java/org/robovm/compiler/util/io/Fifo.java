/*
 * Copyright (C) 2014 RoboVM AB
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */
package org.robovm.compiler.util.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * fifo related utility methods.
 */
public class Fifo {

    private InputStream is;
    private OutputStream os;
    private final File file;

    private Fifo(InputStream is, OutputStream os, File file) {
        this.is = is;
        this.os = os;
        this.file = file;
    }

    public InputStream getInputStream() {
        return is;
    }

    public OutputStream getOutputStream() {
        return os;
    }

    public File getFile() {
        return file;
    }

    /**
     * Creates a new fifo using {@code mkfifo}. The specified type argument will
     * be part of the fifo file name. The fifo will be created in the temporary
     * file directory used by {@link File#createTempFile(String, String)}.
     */
    public static Fifo mkfifo(String type) throws IOException {
        File f = File.createTempFile("robovm-" + type + "-", ".fifo");
        f.delete();
        ProcessBuilder pb = new ProcessBuilder("mkfifo", "-m", "600", f.getAbsolutePath());
        try {
            int exitValue = pb.start().waitFor();
            if (exitValue != 0) {
                throw new IOException("Failed to create " + type + " fifo");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return new Fifo(new OpenOnReadFileInputStream(f), new OpenOnWriteFileOutputStream(f), f);
    }

    /**
     * Creates a echo fido using PipedInputStream/PipedOutputStream
     */
    public static Fifo echofifo() throws IOException {
        PipedInputStream pipedIn = new PipedInputStream();
        PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
        return new Fifo(pipedIn, pipedOut, null);
    }
}
