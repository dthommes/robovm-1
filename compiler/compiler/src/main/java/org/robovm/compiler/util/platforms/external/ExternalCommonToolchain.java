package org.robovm.compiler.util.platforms.external;

import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.compiler.util.platforms.ToolchainUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Toolchain for platforms where tools, certificates, xcode exports are to be crossbuilt and provided
 * such as Linux, Windows
 * @author dkimitsa
 */
public class ExternalCommonToolchain extends ToolchainUtil.Contract{
    private final String URL_XCODE_EXPORT_HELP = "https://github.com/mobivm/robovm";

    // suffix for executable, such as for windows tools to be extended with ".exe"
    private final String exeSuffix;

    // path to expected folder with xcode file export
    private String xcodePath;

    private ExternalCommonToolchain(String platform, String exeSuffix) {
        super(platform);
        this.exeSuffix = exeSuffix;
    }

    public static ExternalCommonToolchain Windows() {
        return new ExternalCommonToolchain("Windows", ".exe");
    }

    public static ExternalCommonToolchain Linux() {
        return new ExternalCommonToolchain("Linux", "");
    }

    // MacOS but using tools Linux way, e.g. without XCode
    public static ExternalCommonToolchain DarwinLinux() {
        return new ExternalCommonToolchain("DarwinLinux", "");
    }

    @Override
    protected String findXcodePath() throws IOException {
        if (!isXcodeInstalled()) {
            throw new IllegalArgumentException("Xcode files not found! You have to export XCode files as described at " +
                    URL_XCODE_EXPORT_HELP);

        }
        return buildXcodePath();
    }

    @Override
    protected boolean isXcodeInstalled() {
        File xcodePath = new File (buildXcodePath());
        return xcodePath.exists() && xcodePath.isDirectory();
    }

    @Override
    protected void pngcrush(Config config, File inFile, File outFile) throws IOException {
        super.pngcrush(config, inFile, outFile);
    }

    @Override
    protected void textureatlas(Config config, File inDir, File outDir) throws IOException {
        super.textureatlas(config, inDir, outDir);
    }

    @Override
    protected void actool(Config config, File partialInfoPlist, File inDir, File outDir) throws IOException {
        super.actool(config, partialInfoPlist, inDir, outDir);
    }

    @Override
    protected void ibtool(Config config, File partialInfoPlist, File inFile, File outFile) throws IOException {
        super.ibtool(config, partialInfoPlist, inFile, outFile);
    }

    @Override
    protected void compileStrings(Config config, File inFile, File outFile) throws IOException {
        super.compileStrings(config, inFile, outFile);
    }

    @Override
    protected String nm(File file) throws IOException {
        return super.nm(file);
    }

    @Override
    protected String otool(File file) throws IOException {
        return super.otool(file);
    }

    @Override
    protected void lipo(Config config, File outFile, List<File> inFiles) throws IOException {
        super.lipo(config, outFile, inFiles);
    }

    @Override
    protected void lipoRemoveArchs(Config config, File file, File inFile, Arch... archs) throws IOException {
        super.lipoRemoveArchs(config, file, inFile, archs);
    }

    @Override
    protected String lipoInfo(Config config, File inFile) throws IOException {
        return super.lipoInfo(config, inFile);
    }

    @Override
    protected String file(File file) throws IOException {
        return super.file(file);
    }

    @Override
    protected void packageApplication(Config config, File appDir, File outFile) throws IOException {
        super.packageApplication(config, appDir, outFile);
    }

    @Override
    public void link(Config config, List<String> args, List<File> objectFiles, List<String> libs, File outFile) throws IOException {
        super.link(config, args, objectFiles, libs, outFile);
    }

    @Override
    public void codesign(Config config, SigningIdentity identity, File entitlementsPList, boolean preserveMetadata, boolean verbose, boolean allocate, File target) throws IOException {
        super.codesign(config, identity, entitlementsPList, preserveMetadata, verbose, allocate, target);
    }


    //
    // private tools
    //
    private String buildXcodePath() {
        if (xcodePath == null)
            xcodePath =  System.getProperty("user.home") + "/.robovm/platform.windows/Xcode.app/Developer";
        return xcodePath;
    }
}
