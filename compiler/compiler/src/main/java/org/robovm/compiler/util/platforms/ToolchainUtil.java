package org.robovm.compiler.util.platforms;

import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.target.ios.DeviceType;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.compiler.util.platforms.darwin.DarwinToolchain;
import org.robovm.compiler.util.platforms.external.ExternalCommonToolchain;
import org.robovm.libimobiledevice.NativeLibrary.LibMobDevicePlatformLibraryProvider;
import org.robovm.llvm.NativeLibrary.LLVMPlatformLibraryProvider;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * proxy wrapper to particular platform implementaiton
 * @author dkimitsa
 */
public class ToolchainUtil {
    private final static Contract impl;
    static {
        SystemInfo systemInfo = getSystemInfo();
        if (systemInfo.os == SystemInfo.OSInfo.macosx)
            impl = new DarwinToolchain();
        else if (systemInfo.os == SystemInfo.OSInfo.macosxlinux)
            impl = ExternalCommonToolchain.DarwinLinux();
        else if (systemInfo.os == SystemInfo.OSInfo.windows)
            impl = ExternalCommonToolchain.Linux();
        else if (systemInfo.os == SystemInfo.OSInfo.linux)
            impl = ExternalCommonToolchain.Windows();
        else
            impl = new Contract("Unsupported OS - " + systemInfo.osName);
    }

    public static SystemInfo getSystemInfo() {
        return SystemInfo.getSystemInfo();
    }

    public static String findXcodePath() throws IOException {
        return impl.findXcodePath();
    }

    public static boolean isXcodeInstalled() {
        return impl.isXcodeInstalled();
    }

    public static boolean isToolchainInstalled() {
        return impl.isToolchainInstalled();
    }

    public static void pngcrush(Config config, File inFile, File outFile) throws IOException {
        impl.pngcrush(config, inFile, outFile);
    }

    public static void textureatlas(Config config, File inDir, File outDir) throws IOException {
        impl.textureatlas(config, inDir, outDir);
    }

    public static void actool(Config config, File partialInfoPlist, File inDir, File outDir) throws IOException {
        impl.actool(config, partialInfoPlist, inDir, outDir);
    }

    public static void ibtool(Config config, File partialInfoPlist, File inFile, File outFile) throws IOException {
        impl.ibtool(config, partialInfoPlist, inFile, outFile);
    }

    public static void compileStrings(Config config, File inFile, File outFile) throws IOException {
        impl.compileStrings(config, inFile, outFile);
    }

    public static String otool(File file) throws IOException {
        return impl.otool(file);
    }

    public static void lipo(Config config, File outFile, List<File> inFiles) throws IOException {
        impl.lipo(config, outFile, inFiles);
    }
    
    public static void lipoRemoveArchs(Config config, File inFile, File outFile, Arch ... archs) throws IOException {
        impl.lipoRemoveArchs(config, inFile, outFile, archs);
    }
    
    public static String lipoInfo(Config config, File inFile) throws IOException {
        return impl.lipoInfo(config, inFile);
    }
    
    public static String file(File file) throws IOException {
        return impl.file(file);
    }

    public static void packageApplication(Config config, File appDir, File outFile) throws IOException {
        impl.packageApplication(config, appDir, outFile);
    }

    public static void link(Config config, List<String> args, List<File> objectFiles, List<String> libs, File outFile)
            throws IOException {
        impl.link(config, args, objectFiles, libs, outFile);
    }

    public static void codesign(Config config, SigningIdentity identity, File entitlementsPList, boolean preserveMetadata, boolean verbose,
                                boolean allocate, File target) throws IOException {
        impl.codesign(config, identity, entitlementsPList, preserveMetadata, verbose, allocate, target);
    }

    public static File getProvisioningProfileDir() {
        return impl.getProvisioningProfileDir();
    }

    public static List<DeviceType> listSimulatorDeviceTypes() {
        return impl.listSimulatorDeviceTypes();
    }

    public static List<SigningIdentity> listSigningIdentity() {
        return impl.listSigningIdentity();

    }

    public static void dsymutil(Config config, File dsymDir, File exePath) throws IOException {
        impl.dsymutil(config, dsymDir, exePath);
    }

    public static void strip(Config config,  File exePath) throws IOException {
        impl.strip(config, exePath);
    }

    /**
     * defines api for each platform
     */
    public static class Contract implements LLVMPlatformLibraryProvider, LibMobDevicePlatformLibraryProvider {
        protected final String platform;

        protected Contract(String platform) {
            this.platform = platform;
            registerLLVMProvider();
            registerLibMobDeviceProvider();
        }

        protected static Contract getImpl() {
            // this getter is required to expose impl to other contracts to allow finding out which contract is currently
            // running
            return impl;
        }

        @Override
        public File getLlvmLibrary() {
            throw new RuntimeException("getLlvmLibrary not implemented for " + platform);
        }

        @Override
        public File getLibMobDeviceLibrary() {
            throw new RuntimeException("getLibMobDeviceLibrary not implemented for " + platform);
        }

        protected String findXcodePath() throws IOException {
            throw new RuntimeException("findXcodePath not implemented for " + platform);
        }

        protected boolean isXcodeInstalled() {
            return false;
        }

        protected boolean isToolchainInstalled() {
            return false;
        }

        protected void pngcrush(Config config, File inFile, File outFile) throws IOException {
            throw new RuntimeException("pngcrush not implemented for " + platform);
        }

        protected void textureatlas(Config config, File inDir, File outDir) throws IOException {
            throw new RuntimeException("textureatlas not implemented for " + platform);
        }

        protected void actool(Config config, File partialInfoPlist, File inDir, File outDir) throws IOException {
            throw new RuntimeException("actool not implemented for " + platform);
        }

        protected void ibtool(Config config, File partialInfoPlist, File inFile, File outFile) throws IOException {
            throw new RuntimeException("ibtool not implemented for " + platform);
        }

        protected void compileStrings(Config config, File inFile, File outFile) throws IOException {
            throw new RuntimeException("compileStrings not implemented for " + platform);
        }

        protected String otool(File file) throws IOException {
            throw new RuntimeException("otool not implemented for " + platform);
        }

        protected void lipo(Config config, File outFile, List<File> inFiles) throws IOException {
            throw new RuntimeException("lipo not implemented for " + platform);
        }

        protected void lipoRemoveArchs(Config config, File file, File inFile, Arch ...archs) throws IOException {
            throw new RuntimeException("lipoRemoveArchs not implemented for " + platform);
        }

        protected String lipoInfo(Config config, File inFile) throws IOException {
            throw new RuntimeException("lipoInfo not implemented for " + platform);
        }

        protected String file(File file) throws IOException {
            throw new RuntimeException("file not implemented for " + platform);
        }

        protected void packageApplication(Config config, File appDir, File outFile) throws IOException {
            throw new RuntimeException("packageApplication not implemented for " + platform);
        }

        protected void link(Config config, List<String> args, List<File> objectFiles, List<String> libs, File outFile) throws IOException {
            throw new RuntimeException("link not implemented for " + platform);
        }

        protected void codesign(Config config, SigningIdentity identity, File entitlementsPList, boolean preserveMetadata, boolean verbose,
                             boolean allocate, File target) throws IOException {
            throw new RuntimeException("codesign not implemented for " + platform);
        }

        protected File getProvisioningProfileDir() {
            throw new RuntimeException("codesign not implemented for " + platform);
        }

        protected List<DeviceType> listSimulatorDeviceTypes() {
            throw new RuntimeException("listSimulatorDeviceTypes not implemented for " + platform);
        }

        protected List<SigningIdentity> listSigningIdentity() {
            throw new RuntimeException("listSigningIdentity not implemented for " + platform);
        }

        protected void dsymutil(Config config, File dsymDir, File exePath) throws IOException {
            throw new RuntimeException("dsymutil not implemented for " + platform);
        }

        protected void strip(Config config, File exePath) throws IOException {
            throw new RuntimeException("strip not implemented for " + platform);
        }
    }
}
