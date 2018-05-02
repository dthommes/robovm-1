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
package org.robovm.libimobiledevice.util;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSString;
import org.robovm.libimobiledevice.AfcClient;
import org.robovm.libimobiledevice.IDevice;
import org.robovm.libimobiledevice.LockdowndClient;
import org.robovm.libimobiledevice.LockdowndServiceDescriptor;
import org.robovm.libimobiledevice.MobileImageMounterClient;
import org.robovm.libimobiledevice.NativeLibrary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Mounts developer image, moved to standalone class as this is share piece of code
 */
public class MobileImageMounter {
    private final Log logger;


    /**
     * interface to redirect log output
     */
    public interface Log {
        // there is Object[] instead of variable number of arguments to allow using it in Labda
        void log(String s, Object[] args);
    }

    private MobileImageMounter(Log logger) {
        this.logger = logger != null ? logger : (s, args) -> {};
    }

    static void mountDeveloperImage(IDevice device, LockdowndClient lockdowndClient, Log logger) throws Exception {
        new MobileImageMounter(logger).mount(device, lockdowndClient);
    }

    private void mount(IDevice device, LockdowndClient lockdowndClient) throws Exception {
        // Find the DeveloperDiskImage.dmg path that best matches the current device. Here's what
        // the paths look like:
        // Platforms/iPhoneOS.platform/DeviceSupport/5.0/DeveloperDiskImage.dmg
        // Platforms/iPhoneOS.platform/DeviceSupport/6.0/DeveloperDiskImage.dmg
        // Platforms/iPhoneOS.platform/DeviceSupport/6.1/DeveloperDiskImage.dmg
        // Platforms/iPhoneOS.platform/DeviceSupport/7.0/DeveloperDiskImage.dmg
        // Platforms/iPhoneOS.platform/DeviceSupport/7.0 (11A465)/DeveloperDiskImage.dmg
        // Platforms/iPhoneOS.platform/DeviceSupport/7.0.3 (11B508)/DeveloperDiskImage.dmg

        String productVersion = lockdowndClient.getValue(null, "ProductVersion").toString(); // E.g. 7.0.2
        String buildVersion = lockdowndClient.getValue(null, "BuildVersion").toString(); // E.g. 11B508
        File deviceSupport = NativeLibrary.getDeveloperImagePath();
        log("Looking up developer disk image for iOS version %s (%s) in %s", productVersion, buildVersion, deviceSupport);
        File devImage = findDeveloperImage(deviceSupport, productVersion, buildVersion);
        File devImageSig = new File(devImage.getParentFile(), devImage.getName() + ".signature");
        byte[] devImageSigBytes = Files.readAllBytes(devImageSig.toPath());

        LockdowndServiceDescriptor mimService = lockdowndClient.startService(MobileImageMounterClient.SERVICE_NAME);
        try (MobileImageMounterClient mimClient = new MobileImageMounterClient(device, mimService)) {

            log("Copying developer disk image %s to device", devImage);

            int majorVersion = Integer.parseInt(getProductVersionParts(productVersion)[0]);
            if (majorVersion >= 7) {
                // Use new upload method
                mimClient.uploadImage(devImage, null, devImageSigBytes);
            } else {
                LockdowndServiceDescriptor afcService = lockdowndClient.startService(AfcClient.SERVICE_NAME);
                try (AfcClient afcClient = new AfcClient(device, afcService)) {
                    afcClient.makeDirectory("/PublicStaging");
                    afcClient.fileCopy(devImage, "/PublicStaging/staging.dimage");
                }
            }

            log("Mounting developer disk image");
            NSDictionary result = mimClient.mountImage("/PublicStaging/staging.dimage", devImageSigBytes, null);
            NSString status = (NSString) result.objectForKey("Status");
            if (status == null || !"Complete".equals(status.toString())) {
                throw new IOException("Failed to mount " + devImage.getAbsolutePath() + " on the device.");
            }
        }
    }

    private void log(String s, Object ... args) {
        logger.log(s, args);
    }

    static File findDeveloperImage(File dsDir, String productVersion, String buildVersion)
            throws FileNotFoundException {

        String[] versionParts = getProductVersionParts(productVersion);

        String[] patterns = new String[]{
                // 7.0.3 (11B508)
                String.format("%s\\.%s\\.%s \\(%s\\)", versionParts[0], versionParts[1], versionParts[2], buildVersion),
                // 7.0.3 (*)
                String.format("%s\\.%s\\.%s \\(.*\\)", versionParts[0], versionParts[1], versionParts[2]),
                // 7.0.3
                String.format("%s\\.%s\\.%s", versionParts[0], versionParts[1], versionParts[2]),
                // 7.0 (11A465)
                String.format("%s\\.%s \\(%s\\)", versionParts[0], versionParts[1], buildVersion),
                // 7.0 (*)
                String.format("%s\\.%s \\(.*\\)", versionParts[0], versionParts[1]),
                // 7.0
                String.format("%s\\.%s", versionParts[0], versionParts[1])
        };

        File[] dirs = dsDir.listFiles();
        if (dirs != null) {
            for (String pattern : patterns) {
                for (File dir : dirs) {
                    if (dir.isDirectory() && dir.getName().matches(pattern)) {
                        File dmg = new File(dir, "DeveloperDiskImage.dmg");
                        File sig = new File(dir, dmg.getName() + ".signature");
                        if (dmg.isFile() && sig.isFile()) {
                            return dmg;
                        }
                    }
                }
            }
        }
        throw new FileNotFoundException("No DeveloperDiskImage.dmg found in "
                + dsDir.getAbsolutePath() + " for iOS version " + productVersion
                + " (" + buildVersion + ")");
    }

    /**
     * Splits productVersion and expand to 3 parts (e.g. 7.0 -> 7.0.0)
     */
    private static String[] getProductVersionParts(String productVersion) {
        String[] versionParts = Arrays.copyOf(productVersion.split("\\."), 3);
        for (int i = 0; i < versionParts.length; i++) {
            if (versionParts[i] == null) {
                versionParts[i] = "0";
            }
        }
        return versionParts;
    }

    public static void main(String[] args) throws Exception {
        // just for test
        String[] udids = IDevice.listUdids();
        if (udids.length == 0) {
            System.err.println("No device connected");
            return;
        }
        if (udids.length > 1) {
            System.err.println("More than 1 device connected ("
                    + Arrays.asList(udids) + "). Using " + udids[0]);
        }

        IDevice device = new IDevice(udids[0]);
        try (LockdowndClient lockdowndClient = new LockdowndClient(device, MobileImageMounter.class.getSimpleName(), true)) {
            mountDeveloperImage(device, lockdowndClient, (s, args1) -> {
                System.out.format(s, args1);
                System.out.println();
            });
        }
    }
}
