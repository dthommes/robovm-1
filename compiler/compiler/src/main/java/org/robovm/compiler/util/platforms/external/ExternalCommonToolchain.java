package org.robovm.compiler.util.platforms.external;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.OS;
import org.robovm.compiler.target.ios.DeviceType;
import org.robovm.compiler.target.ios.IOSTarget;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.compiler.util.Executor;
import org.robovm.compiler.util.platforms.SystemInfo;
import org.robovm.compiler.util.platforms.ToolchainUtil;
import org.robovm.utils.codesign.utils.P12Certificate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Toolchain for platforms where tools, certificates, xcode exports are to be crossbuilt and provided
 * such as Linux, Windows
 * @author dkimitsa
 */
public class ExternalCommonToolchain extends ToolchainUtil.Contract{
    private final String URL_XCODE_EXPORT_HELP = "https://github.com/mobivm/robovm";
    private final String URL_TOOLCHAIN_DOWNLOAD_HELP = "https://github.com/mobivm/robovm";

    // extension for executable, such as for windows tools to be extended with ".exe"
    private final String exeExt;

    // extension for shared libraries, such as for windows libs to be extended with ".dlls"
    private final String libExt;

    // path to expected folder with xcode file export
    private String xcodePath;
    // path to toolchain home
    private File toolChainPath;

    // shiny platform name to use in messages
    private final String shinyPlatformName;

    private ExternalCommonToolchain(String platform, String exeExt, String libExt) {
        super(platform);
        this.exeExt = exeExt;
        this.libExt = libExt;
        shinyPlatformName = ToolchainUtil.getSystemInfo().os + "[" + ToolchainUtil.getSystemInfo().arch + "]";
    }

    public static ExternalCommonToolchain Windows() {
        return new ExternalCommonToolchain("Windows", ".exe", ".dll");
    }

    public static ExternalCommonToolchain Linux() {
        return new ExternalCommonToolchain("Linux", "", ".so");
    }

    // MacOS but using tools Linux way, e.g. without XCode
    public static ExternalCommonToolchain DarwinLinux() {
        return new ExternalCommonToolchain("DarwinLinux", "", ".dylib");
    }

    @Override
    public File getLlvmLibrary() {
        validateToolchain();
        return new File(toolChainPath, "librobovm-llvm" + libExt);
    }

    @Override
    public File getLibMobDeviceLibrary() {
        validateToolchain();
        return new File(toolChainPath, "librobovm-libimobiledevice" + libExt);
    }

    @Override
    protected String findXcodePath() throws IOException {
        if (!isXcodeInstalled()) {
            throw new Error("Xcode files not found! You have to export XCode files as described at " +
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

    // TODO: just copy-paste from DarwinToolchainUtil
    @Override
    protected void actool(Config config, File partialInfoPlist, File inDir, File outDir) throws IOException {
        List<Object> opts = new ArrayList<>();

        String appIconSetName = null;
        String launchImagesName = null;

        final String appiconset = "appiconset";
        final String launchimage = "launchimage";

        for (String fileName : inDir.list()) {
            String ext = FilenameUtils.getExtension(fileName);
            if (ext.equals(appiconset)) {
                appIconSetName = FilenameUtils.getBaseName(fileName);
            } else if (ext.equals(launchimage)) {
                launchImagesName = FilenameUtils.getBaseName(fileName);
            }
        }
        if (appIconSetName != null || launchImagesName != null) {
            if (appIconSetName != null) {
                opts.add("--app-icon");
                opts.add(appIconSetName);
            }
            if (launchImagesName != null) {
                opts.add("--launch-image");
                opts.add(launchImagesName);
            }

            opts.add("--output-partial-info-plist");
            opts.add(partialInfoPlist);
        }

        opts.add("--platform");
        if (IOSTarget.isDeviceArch(config.getArch())) {
            opts.add("iphoneos");
        } else if (IOSTarget.isSimulatorArch(config.getArch())) {
            opts.add("iphonesimulator");
        }

        String minOSVersion = config.getOs().getMinVersion();
        if (config.getIosInfoPList() != null) {
            String v = config.getIosInfoPList().getMinimumOSVersion();
            if (v != null) {
                minOSVersion = v;
            }
        }

        new Executor(config.getLogger(), buildToolPath("actool")).args("--output-format", "human-readable-text", opts,
                "--minimum-deployment-target", minOSVersion, "--target-device", "iphone", "--target-device", "ipad",
                "--compress-pngs", "--compile", outDir, inDir).exec();
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
    protected void link(Config config, List<String> args, List<File> objectFiles, List<String> libs, File outFile) throws IOException {
        validateToolchain();

        // filter out unsupported oses
        if (config.getOs() == OS.ios && (config.getArch() == Arch.x86 || config.getArch() == Arch.x86_64)) {
            // simulator is not supported
            throw new Error("IOS simulator target is not supported in " + shinyPlatformName);
        } else if (config.getOs() == OS.macosx && ToolchainUtil.getSystemInfo().os != SystemInfo.OSInfo.macosxlinux) {
            // compile for macos is allowed only in case of forced MACOSLINUX, shall not happen
            throw new Error("SHALL NOT HAPPEN: MacOSX target shall be selected only under MacOSX platform, not available in " + shinyPlatformName);
        } else if (config.getOs() == OS.linux && ToolchainUtil.getSystemInfo().os != SystemInfo.OSInfo.linux) {
            // target is linux on not linux os -- shall not happen
            throw new Error("SHALL NOT HAPPEN: Linux console target shall be selected only under Linux platform, not available in " + shinyPlatformName);
        }

        boolean isDarwin = config.getOs().getFamily() == OS.Family.darwin;
        /*
         * The Xcode linker doesn't need paths with spaces to be quoted and will
         * fail if we do quote. The Xcode linker will crash if we pass more than
         * 65535 files in an objects file.
         *
         * The linker on Linux will fail if we don't quote paths with spaces.
         */
        List<File> objectsFiles = writeObjectsFiles(config, objectFiles, isDarwin ? 0xffff : Integer.MAX_VALUE,
                !isDarwin);

        List<String> opts = new ArrayList<String>();
        if (config.isDebug()) {
            opts.add("-g");
        }
        if (isDarwin) {
            opts.add("-arch");
            opts.add(config.getArch().getClangName());
            for (File objectsFile : objectsFiles) {
                opts.add("-Wl,-filelist," + objectsFile.getAbsolutePath());
            }
            /*
             * See #123, ignore ld: warning: pointer not aligned at address [infostruct] message with Xcode 8.3
             * unless we find a better solution
             */
            opts.add("-w");
        } else {
            opts.add(config.getArch().is32Bit() ? "-m32" : "-m64");
            for (File objectsFile : objectsFiles) {
                opts.add("@" + objectsFile.getAbsolutePath());
            }
        }
        opts.addAll(args);

        if (isDarwin) {
            // there is no clang but only linker so it is required to convert parameters to linkers style similar how it
            // does CLANG, call it with -v params to observe
            opts = convertParamsForLinker(opts);
            libs = convertLibsForLinker(libs);

            // also these to be added when linking directly with ld64
            opts.add("-demangle");
            opts.add("-dynamic");
            libs.add("-lc++");
        }

        String ccPath = isDarwin ? buildToolPath("arm-apple-darwin11-ld") : "g++";
        new Executor(config.getLogger(), ccPath).args("-o", outFile, opts, libs).exec();

    }

    @Override
    protected void codesign(Config config, SigningIdentity identity, File entitlementsPList, boolean preserveMetadata, boolean verbose, boolean allocate, File target) throws IOException {
        super.codesign(config, identity, entitlementsPList, preserveMetadata, verbose, allocate, target);
    }

    @Override
    protected File getProvisioningProfileDir() {
        return new File(new File(System.getProperty("user.home")), "/.robovm/platform/mobileprovision");
    }

    @Override
    protected List<DeviceType> listSimulatorDeviceTypes() {
        // no simulators on platforms other than MacOSX
        return Collections.emptyList();
    }

    @Override
    protected List<SigningIdentity> listSigningIdentity() {
        File keychainDir = new File(System.getProperty("user.home") + "/.robovm/platform/keychain");
        List<SigningIdentity> identities = new ArrayList<>();
        if (keychainDir.exists() && keychainDir.isDirectory()) {
            File[] files = keychainDir.listFiles();
            if (files == null)
                return Collections.emptyList();

            for (File f : files) {
                if (!f.isFile() || !f.getName().endsWith(".p12"))
                    continue;
                try {
                    P12Certificate p12 = P12Certificate.load(f);
                    identities.add(new SigningIdentity<>(p12.getCertificateName(), p12.getCertificateFingerprint(), p12));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return identities;
    }

    //
    // private tools
    //
    private String buildXcodePath() {
        if (xcodePath == null)
            xcodePath = System.getProperty("user.home") + "/.robovm/platform/Xcode.app/Developer";
        return xcodePath;
    }

    /**
     * validates toolchain and throws error if something is not ok
     */
    private void validateToolchain() {
        if (toolChainPath == null)
            toolChainPath = new File(System.getProperty("user.home") + "/.robovm/platform/toolchain_" + platform + "_"+ ToolchainUtil.getSystemInfo().arch);

        if (!toolChainPath.exists() || !toolChainPath.isDirectory()) {
            // toolchain not installed
            throw new Error("Toolchain is not installed for " + shinyPlatformName + ". Please download and install as described at " + URL_TOOLCHAIN_DOWNLOAD_HELP);
        }

        // TODO: check version and compatibility
    }

    private String buildToolPath(String toolName) {
        File toolFile = new File(toolChainPath, toolName + exeExt);
        return toolFile.getAbsolutePath();
    }

    // TODO: just copy-paste from DarwinToolchainUtil
    private static List<File> writeObjectsFiles(Config config, List<File> objectFiles, int maxObjectsPerFile,
                                                boolean quote) throws IOException {

        ArrayList<File> files = new ArrayList<>();
        for (int i = 0, start = 0; start < objectFiles.size(); i++, start += maxObjectsPerFile) {
            List<File> partition = objectFiles.subList(start, Math.min(objectFiles.size(), start + maxObjectsPerFile));
            List<String> paths = new ArrayList<>();
            for (File f : partition) {
                paths.add((quote ? "\"" : "") + f.getAbsolutePath() + (quote ? "\"" : ""));
            }

            File objectsFile = new File(config.getTmpDir(), "objects" + i);
            FileUtils.writeLines(objectsFile, paths, "\n");
            files.add(objectsFile);
        }

        return files;
    }


    private List<String> convertParamsForLinker(List<String> opts) {
        List<String> result = new ArrayList<>();
        Iterator<String> it = opts.iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext()) {
            String p = it.next();
            if (p.startsWith("-Wl,")) {
                String str = p.substring("-Wl,".length());
                // just in case there is a list of params
                // TODO: potentially problematic
                Collections.addAll(result, str.split(","));
            } else if (p.startsWith("-miphoneos-version-min=")) {
                result.add("-iphoneos_version_min");
                result.add(p.substring("-miphoneos-version-min=".length()));
            } else if (p.equals("-isysroot")) {
                result.add("-syslibroot");
            } else if (p.equals("-g") || (p.equals("-fPIC"))) {
                // remove parameters to compiler
                //noinspection UnnecessaryContinue
                continue;
            } else {
                // just copy
                result.add(p);
            }
        }

        return result;
    }

    private List<String> convertLibsForLinker(List<String> libs) {
        List<String> result = new ArrayList<>();
        Iterator<String> it = libs.iterator();
        while (it.hasNext()) {
            String p = it.next();
            if (p.equals("-Xlinker")) {
                result.add(it.next());
            } else {
                // just copy
                result.add(p);
            }
        }

        return result;
    }
}
