package org.robovm.compiler.util.platforms.darwin;

import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.compiler.util.platforms.ToolchainUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Implements contract for MacOS
 * In general it just performs calls to old ToolchainUtils as it was long time ago
 * @author dkimitsa
 */
public class DarwinToolchain extends ToolchainUtil.Contract {
    public DarwinToolchain() {
        super("MacOSX");
    }

    @Override
    public String findXcodePath() throws IOException {
        return DarwinToolchainUtil.findXcodePath();
    }

    @Override
    public boolean isXcodeInstalled() {
        return DarwinToolchainUtil.isXcodeInstalled();
    }

    @Override
    public void pngcrush(Config config, File inFile, File outFile) throws IOException {
        DarwinToolchainUtil.pngcrush(config, inFile, outFile);
    }

    @Override
    public void textureatlas(Config config, File inDir, File outDir) throws IOException {
        DarwinToolchainUtil.textureatlas(config, inDir, outDir);
    }

    @Override
    public void actool(Config config, File partialInfoPlist, File inDir, File outDir) throws IOException {
        DarwinToolchainUtil.actool(config, partialInfoPlist, inDir, outDir);
    }

    @Override
    public void ibtool(Config config, File partialInfoPlist, File inFile, File outFile) throws IOException {
        DarwinToolchainUtil.ibtool(config, partialInfoPlist, inFile, outFile);
    }

    @Override
    public void compileStrings(Config config, File inFile, File outFile) throws IOException {
        DarwinToolchainUtil.compileStrings(config, inFile, outFile);
    }

    @Override
    public String nm(File file) throws IOException {
        return DarwinToolchainUtil.nm(file);
    }

    @Override
    public String otool(File file) throws IOException {
        return DarwinToolchainUtil.otool(file);
    }

    @Override
    public void lipo(Config config, File outFile, List<File> inFiles) throws IOException {
        DarwinToolchainUtil.lipo(config, outFile, inFiles);
    }

    @Override
    public void lipoRemoveArchs(Config config, File file, File inFile, Arch ...archs) throws IOException {
        DarwinToolchainUtil.lipoRemoveArchs(config, file, inFile, archs);
    }

    @Override
    public String lipoInfo(Config config, File inFile) throws IOException {
        return DarwinToolchainUtil.lipoInfo(config, inFile);
    }

    @Override
    public String file(File file) throws IOException {
        return DarwinToolchainUtil.file(file);
    }

    @Override
    public void packageApplication(Config config, File appDir, File outFile) throws IOException {
        DarwinToolchainUtil.packageApplication(config, appDir, outFile);
    }

    @Override
    public void link(Config config, List<String> args, List<File> objectFiles, List<String> libs, File outFile) throws IOException {
        DarwinToolchainUtil.link(config, args, objectFiles, libs, outFile);
    }

    @Override
    public void codesign(Config config, SigningIdentity identity, File entitlementsPList, boolean preserveMetadata, boolean verbose, boolean allocate, File target) throws IOException {
        DarwinToolchainUtil.codesign(config, identity, entitlementsPList, preserveMetadata, verbose, allocate,
                target);
    }
}
