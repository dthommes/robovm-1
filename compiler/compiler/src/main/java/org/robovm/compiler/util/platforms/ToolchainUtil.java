package org.robovm.compiler.util.platforms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.OS;
import org.robovm.compiler.config.tools.TextureAtlas;
import org.robovm.compiler.log.ConsoleLogger;
import org.robovm.compiler.log.Logger;
import org.robovm.compiler.target.ios.IOSTarget;
import org.robovm.compiler.target.ios.SDK;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.compiler.util.Executor;
import org.robovm.compiler.util.platforms.darwin.DarwinToolchain;

/**
 * proxy wrapper to particular platform implementaiton
 * @author dkimitsa
 */
public class ToolchainUtil {
    private final static Contract impl;
    static {
        OS os = OS.getDefaultOS();
        if (os == OS.macosx)
            impl = new DarwinToolchain();
        else
            impl = new Contract("Unsupported OS - " + os);
    }

    public static String findXcodePath() throws IOException {
        return impl.findXcodePath();
    }

    public static boolean isXcodeInstalled() {
        return impl.isXcodeInstalled();
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

    public static String nm(File file) throws IOException {
        return impl.nm(file);
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


    /**
     * defines api for each platform
     */
    public static class Contract {
        protected final String platform;
        protected Contract(String platform) {
            this.platform = platform;
        }

        protected String findXcodePath() throws IOException {
            throw new RuntimeException("findXcodePath not implemented for " + platform);
        }

        protected boolean isXcodeInstalled() {
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

        protected String nm(File file) throws IOException {
            throw new RuntimeException("nm not implemented for " + platform);
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

        public void link(Config config, List<String> args, List<File> objectFiles, List<String> libs, File outFile) throws IOException {
            throw new RuntimeException("link not implemented for " + platform);
        }

        public void codesign(Config config, SigningIdentity identity, File entitlementsPList, boolean preserveMetadata, boolean verbose,
                             boolean allocate, File target) throws IOException {
            throw new RuntimeException("codesign not implemented for " + platform);
        }
    }
}
