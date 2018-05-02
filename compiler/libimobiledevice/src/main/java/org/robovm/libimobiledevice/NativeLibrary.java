/*
 * Copyright (C) 2013 RoboVM AB
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
package org.robovm.libimobiledevice;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 */
public class NativeLibrary {
    private static boolean loaded = false;
    public static final boolean supportedPlatform;

    private static LibMobDevicePlatformLibraryProvider platformLibraryProvider;
    public interface LibMobDevicePlatformLibraryProvider {
        File getLibMobDeviceLibrary();
        File getDeveloperImagePath() throws IOException;
        default void registerLibMobDeviceProvider() {
            platformLibraryProvider = this;
        }
    }

    static {
        String osProp = System.getProperty("os.name").toLowerCase();
        String archProp = System.getProperty("os.arch").toLowerCase();
        String os;
        String arch;
        if (osProp.startsWith("mac") || osProp.startsWith("darwin")) {
            os = "macosx";
        } else {
            os = null;
        }
        if (archProp.matches("amd64|x86[-_]64")) {
            arch = "x86_64";
        } else {
            arch = null;
        }

        supportedPlatform = os != null && arch != null;
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        if (os.startsWith("mac") || os.startsWith("darwin")) {
            if (System.getenv("ROBOVM_FORCE_MACOSXLINUX") != null || System.getProperty("ROBOVM_FORCE_MACOSXLINUX") != null) {
                // has to be loaded via provider
                os = null;
            } else {
                os = "macosx";
            }
        } else {
            os = null;
        }

        if (arch.matches("amd64|x86[-_]64")) {
            arch = "x86_64";
        } else {
            arch = null;
        }

        File libFile = null;
        if (os != null && arch != null) {
            // MacOS case, use embedded library
            InputStream in = NativeLibrary.class.getResourceAsStream("binding/macosx/x86_64/librobovm-libimobiledevice.dylib");
            if (in == null) {
                throw new UnsatisfiedLinkError("Native library for " + os + "-" + arch + " not found");
            }
            OutputStream out = null;
            try {
                libFile = File.createTempFile("librobovm-llvm-x86_64", "dylib");
                libFile.deleteOnExit();
                out = new BufferedOutputStream(new FileOutputStream(libFile));
                copy(in, out);
            } catch (IOException e) {
                throw (Error) new UnsatisfiedLinkError(e.getMessage()).initCause(e);
            } finally {
                closeQuietly(in);
                closeQuietly(out);
            }
        } else if (platformLibraryProvider != null) {
            libFile = platformLibraryProvider.getLibMobDeviceLibrary();
        }

        if (libFile == null) {
            throw new Error("Unsupported os " + System.getProperty("os.name") + "[" + System.getProperty("os.arch") + "]");
        }

        Runtime.getRuntime().load(libFile.getAbsolutePath());
    }

    public static synchronized File getDeveloperImagePath() throws IOException {
        return platformLibraryProvider.getDeveloperImagePath();
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
    }
    
    private static void closeQuietly(Closeable in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ioe) {
        }
    }
}
