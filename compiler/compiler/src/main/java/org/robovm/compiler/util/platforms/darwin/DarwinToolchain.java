package org.robovm.compiler.util.platforms.darwin;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.log.Logger;
import org.robovm.compiler.target.ios.DeviceType;
import org.robovm.compiler.target.ios.SDK;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.compiler.util.Executor;
import org.robovm.compiler.util.platforms.ToolchainUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public File getLlvmLibrary() {
        throw new Error("INTERNAL ERROR: LLVM library shall be embeded for MacOSX. @" + platform);
    }

    @Override
    public File getLibMobDeviceLibrary() {
        throw new Error("INTERNAL ERROR: iLibMobDevice library shall be embeded for MacOSX. @" + platform);
    }
    @Override
    protected String findXcodePath() throws IOException {
        return DarwinToolchainUtil.findXcodePath();
    }

    @Override
    protected boolean isXcodeInstalled() {
        return DarwinToolchainUtil.isXcodeInstalled();
    }

    @Override
    protected boolean isToolchainInstalled() {
        return isXcodeInstalled();
    }

    @Override
    protected void pngcrush(Config config, File inFile, File outFile) throws IOException {
        DarwinToolchainUtil.pngcrush(config, inFile, outFile);
    }

    @Override
    protected void textureatlas(Config config, File inDir, File outDir) throws IOException {
        DarwinToolchainUtil.textureatlas(config, inDir, outDir);
    }

    @Override
    protected void actool(Config config, File partialInfoPlist, File inDir, File outDir) throws IOException {
        DarwinToolchainUtil.actool(config, partialInfoPlist, inDir, outDir);
    }

    @Override
    protected void ibtool(Config config, File partialInfoPlist, File inFile, File outFile) throws IOException {
        DarwinToolchainUtil.ibtool(config, partialInfoPlist, inFile, outFile);
    }

    @Override
    protected void compileStrings(Config config, File inFile, File outFile) throws IOException {
        DarwinToolchainUtil.compileStrings(config, inFile, outFile);
    }

    @Override
    protected String otool(File file) throws IOException {
        return DarwinToolchainUtil.otool(file);
    }

    @Override
    protected void lipo(Config config, File outFile, List<File> inFiles) throws IOException {
        DarwinToolchainUtil.lipo(config, outFile, inFiles);
    }

    @Override
    protected void lipoRemoveArchs(Config config, File inFile, File outFile, String ...archs) throws IOException {
        DarwinToolchainUtil.lipoRemoveArchs(config, inFile, outFile, archs);
    }

    @Override
    protected String lipoInfo(Config config, File inFile) throws IOException {
        return DarwinToolchainUtil.lipoInfo(config, inFile);
    }

    @Override
    protected String file(File file) throws IOException {
        return DarwinToolchainUtil.file(file);
    }

    @Override
    protected void packageApplication(Config config, File appDir, File outFile) throws IOException {
        DarwinToolchainUtil.packageApplication(config, appDir, outFile);
    }

    @Override
    protected void link(Config config, List<String> args, List<File> objectFiles, List<String> libs, File outFile) throws IOException {
        DarwinToolchainUtil.link(config, args, objectFiles, libs, outFile);
    }

    @Override
    protected void codesign(Config config, SigningIdentity identity, File entitlementsPList, boolean preserveMetadata, boolean verbose, boolean allocate, File target) throws IOException {
        DarwinToolchainUtil.codesign(config, identity, entitlementsPList, preserveMetadata, verbose, allocate,
                target);
    }

    @Override
    protected File getProvisioningProfileDir() {
        return new File(new File(System.getProperty("user.home")), "Library/MobileDevice/Provisioning Profiles");
    }

    @Override
    protected List<DeviceType> listSimulatorDeviceTypes() {
        try {
            List<SDK> sdks = SDK.listSimulatorSDKs();
            Map<String, SDK> sdkMap = new HashMap<>();
            for (SDK sdk : sdks) {
                sdkMap.put(sdk.getVersion(), sdk);
            }

            String capture = new Executor(Logger.NULL_LOGGER, "xcrun").args(
                    "simctl", "list", "devices", "-j").execCapture();
            List<DeviceType> types = new ArrayList<DeviceType>();

            JSONParser parser = new JSONParser();
            JSONObject deviceList = (JSONObject)((JSONObject) parser.parse(capture)).get("devices");

            Iterator iter=deviceList.entrySet().iterator();
            while(iter.hasNext()){
                Map.Entry entry=(Map.Entry)iter.next();
                String sdkMapKey = entry.getKey().toString().replace("iOS ","");
                JSONArray devices = (JSONArray) entry.getValue();
                for (Object obj : devices) {
                    JSONObject device = (JSONObject) obj;
                    SDK sdk = sdkMap.get(sdkMapKey);
                    final String deviceName = device.get("name").toString();

                    if (!device.get("availability").toString().contains("unavailable") && sdk != null) {
                        Set<Arch> archs = new HashSet<>();
                        archs.add(Arch.x86);
                        if (!Arrays.asList(DeviceType.ONLY_32BIT_DEVICES).contains(deviceName)) {
                            archs.add(Arch.x86_64);
                        }

                        types.add(new DeviceType(deviceName, device.get("udid").toString(),
                                device.get("state").toString(), sdk, archs));
                    }
                }
            }

            // Sort. Make sure that devices that have an id which is a prefix of
            // another id comes before in the list.
            Collections.sort(types);
            return types;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected List<SigningIdentity> listSigningIdentity() {
        try {
            return parse(new Executor(Logger.NULL_LOGGER, "security")
                    .args("find-identity", "-v", "-p", "codesigning").execCapture());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void dsymutil(Logger logger, File dsymDir, File exePath) throws IOException {
        DarwinToolchainUtil.dsymutil(logger, dsymDir, exePath);
    }

    @Override
    protected void strip(Config config, File exePath) throws IOException {
        DarwinToolchainUtil.strip(config, exePath);
    }

    //
    // internal tools
    //

    // intentionally made public static for tests
    public static List<SigningIdentity> parse(String securityOutput) {
        /* Output from security looks like this:
         *   [... ommitted output ...]
         *   Valid identities only
         *   1) 433D4A1CD97F77226F67959905A2840265A92D31 "iPhone Developer: Rolf Hudson (HS5OW37HQP)" (CSSMERR_TP_CERT_REVOKED)
         *   2) F8E60167BD74A2E9FC39B239E58CCD73BE1112E6 "iPhone Developer: Rolf Hudson (HS5OW37HQP)"
         *   3) AC2EC9D4D26889649DE4196FBFD54BF5924169F9 "iPhone Distribution: Acme Inc"
         *     3 valid identities found
         */
        ArrayList<SigningIdentity> ids = new ArrayList<SigningIdentity>();
        Pattern pattern = Pattern.compile("^\\d+\\)\\s+([0-9A-F]+)\\s+\"([^\"]*)\"\\s*(.*)");
        for (String line : securityOutput.split("\n")) {
            line = line.trim();
            Matcher matcher = pattern.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String name = matcher.group(2);
            String fingerprint = matcher.group(1);
            String flags = matcher.group(3);
            // See cssmerr.h for possible CSSMERR_TP_CERT_* constants.
            if (flags == null || !flags.contains("CSSMERR_TP_CERT_")) {
                ids.add(new SigningIdentity(name, fingerprint));
            }
        }
        Collections.sort(ids);
        return ids;
    }
}
