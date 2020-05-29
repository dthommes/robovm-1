package org.robovm.compiler.config;

import org.robovm.compiler.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Home {
    private File binDir;
    private File libVmDir;
    private File rtPath;
    private Map<Cacerts, File> cacertsPath;
    private boolean dev = false;

    public Home(File homeDir) {
        this(homeDir, true);
    }

    protected Home(File homeDir, boolean validate) {
        if (validate) {
            validate(homeDir);
        }
        binDir = new File(homeDir, "bin");
        libVmDir = new File(homeDir, "lib/vm");
        rtPath = new File(homeDir, "lib/robovm-rt.jar");
        cacertsPath = new HashMap<>();
        cacertsPath.put(Cacerts.full, new File(homeDir, "lib/robovm-cacerts-full.jar"));
    }

    private Home(File devDir, File binDir, File libVmDir, File rtPath) {
        this.binDir = binDir;
        this.libVmDir = libVmDir;
        this.rtPath = rtPath;
        cacertsPath = new HashMap<>();
        cacertsPath.put(Cacerts.full, new File(devDir,
                "cacerts/full/target/robovm-cacerts-full-" + Version.getVersion() + ".jar"));
        this.dev = true;
    }

    public boolean isDev() {
        return dev;
    }

    public File getBinDir() {
        return binDir;
    }

    public File getLibVmDir() {
        return libVmDir;
    }

    public File getRtPath() {
        return rtPath;
    }

    public File getCacertsPath(Cacerts cacerts) {
        return cacertsPath.get(cacerts);
    }

    public static Home find() {
        // Check if ROBOVM_DEV_ROOT has been set. If set it should be
        // pointing at the root of a complete RoboVM source tree.
        if (System.getenv("ROBOVM_DEV_ROOT") != null) {
            File dir = new File(System.getenv("ROBOVM_DEV_ROOT"));
            return validateDevRootDir(dir);
        }
        if (System.getProperty("ROBOVM_DEV_ROOT") != null) {
            File dir = new File(System.getProperty("ROBOVM_DEV_ROOT"));
            return validateDevRootDir(dir);
        }

        if (System.getenv("ROBOVM_HOME") != null) {
            File dir = new File(System.getenv("ROBOVM_HOME"));
            return new Home(dir);
        }

        List<File> candidates = new ArrayList<>();
        File userHome = new File(System.getProperty("user.home"));
        candidates.add(new File(userHome, "Applications/robovm"));
        candidates.add(new File(userHome, ".robovm/home"));
        candidates.add(new File("/usr/local/lib/robovm"));
        candidates.add(new File("/opt/robovm"));
        candidates.add(new File("/usr/lib/robovm"));

        for (File dir : candidates) {
            if (dir.exists()) {
                return new Home(dir);
            }
        }

        throw new IllegalArgumentException("ROBOVM_HOME not set and no RoboVM "
                + "installation found in " + candidates);
    }

    public static void validate(File dir) {
        String error = "Path " + dir + " is not a valid RoboVM install directory: ";
        // Check for required dirs and match the compiler version with our
        // version.
        if (!dir.exists()) {
            throw new IllegalArgumentException(error + "no such path");
        }

        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(error + "not a directory");
        }

        File libDir = new File(dir, "lib");
        if (!libDir.exists() || !libDir.isDirectory()) {
            throw new IllegalArgumentException(error + "lib/ missing or invalid");
        }
        File binDir = new File(dir, "bin");
        if (!binDir.exists() || !binDir.isDirectory()) {
            throw new IllegalArgumentException(error + "bin/ missing or invalid");
        }
        File libVmDir = new File(libDir, "vm");
        if (!libVmDir.exists() || !libVmDir.isDirectory()) {
            throw new IllegalArgumentException(error + "lib/vm/ missing or invalid");
        }
        File rtJarFile = new File(libDir, "robovm-rt.jar");
        if (!rtJarFile.exists() || !rtJarFile.isFile()) {
            throw new IllegalArgumentException(error
                    + "lib/robovm-rt.jar missing or invalid");
        }

        // Compare the version of this compiler with the version of the
        // robovm-rt.jar in the home dir. They have to match.
        try {
            String thisVersion = Version.getVersion();
            String thatVersion = Config.getImplementationVersion(rtJarFile);
            if (thisVersion == null || !thisVersion.equals(thatVersion)) {
                throw new IllegalArgumentException(error + "version mismatch (expected: "
                        + thisVersion + ", was: " + thatVersion + ")");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(error
                    + "failed to get version of rt jar", e);
        }
    }

    private static Home validateDevRootDir(File dir) {
        String error = "Path " + dir + " is not a valid RoboVM source tree: ";
        // Check for required dirs.
        if (!dir.exists()) {
            throw new IllegalArgumentException(error + "no such path");
        }

        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(error + "not a directory");
        }

        File vmBinariesDir = new File(dir, "vm/target/binaries");
        if (!vmBinariesDir.exists() || !vmBinariesDir.isDirectory()) {
            throw new IllegalArgumentException(error + "vm/target/binaries/ missing or invalid");
        }
        File binDir = new File(dir, "bin");
        if (!binDir.exists() || !binDir.isDirectory()) {
            throw new IllegalArgumentException(error + "bin/ missing or invalid");
        }

        String rtJarName = "robovm-rt-" + Version.getVersion() + ".jar";
        File rtJar = new File(dir, "rt/target/" + rtJarName);
        File rtClasses = new File(dir, "rt/target/classes/");
        File rtSource = rtJar;
        if (!rtJar.exists() || rtJar.isDirectory()) {
            if (!rtClasses.exists() || rtClasses.isFile()) {
                throw new IllegalArgumentException(error
                        + "rt/target/" + rtJarName + " missing or invalid");
            } else {
                rtSource = rtClasses;
            }
        }

        return new Home(dir, binDir, vmBinariesDir, rtSource);
    }
}
