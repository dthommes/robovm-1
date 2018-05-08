package org.robovm.libimobiledevice.util;

import org.robovm.libimobiledevice.IDevice;
import org.robovm.libimobiledevice.LibIMobileDeviceException;
import org.robovm.libimobiledevice.LockdowndClient;
import org.robovm.libimobiledevice.LockdowndServiceDescriptor;
import org.robovm.libimobiledevice.NativeLibrary;
import org.robovm.libimobiledevice.ScreenshotrClient;
import org.robovm.libimobiledevice.binding.LockdowndError;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Simple utility wrap that captures screenshot
 * @author dkimitsa
 */
public final class Screenshotr  {
    private Screenshotr() {
    }

    public static byte[] captureScreenshotTiff(IDevice device) throws Exception {
        try (LockdowndClient lockdowndClient = new LockdowndClient(device, Screenshotr.class.getSimpleName(), true)) {
            LockdowndServiceDescriptor service;
            try {
                service = lockdowndClient.startService(ScreenshotrClient.SERVICE_NAME);
            } catch (LibIMobileDeviceException e) {
                if (e.getErrorCode() == LockdowndError.LOCKDOWN_E_INVALID_SERVICE.swigValue()) {
                    // This happens when the developer image hasn't been mounted.
                    // Mount and try again.
                    MobileImageMounter.mountDeveloperImage(device, lockdowndClient, null);
                    service = lockdowndClient.startService(ScreenshotrClient.SERVICE_NAME);
                } else {
                    throw e;
                }
            }

            try (ScreenshotrClient screenShotrclient = new ScreenshotrClient(device, service)) {
                return screenShotrclient.captureScreenshotTiff();
            }
        }
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

        // register dummy provider to resolve image path
        new NativeLibrary.LibMobDevicePlatformLibraryProvider() {
            @Override
            public File getLibMobDeviceLibrary() {
                return null;
            }

            @Override
            public File getDeveloperImagePath() throws IOException {
                return new File(getXcodePath(), "Platforms/iPhoneOS.platform/DeviceSupport");
            }
        }.registerLibMobDeviceProvider();

        IDevice device = new IDevice(udids[0]);
        byte[] image = captureScreenshotTiff(device);
        try (FileOutputStream os = new FileOutputStream(new File("screenshot.tiff"))){
            os.write(image);
        }
    }

    private static File getXcodePath()  {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile(Screenshotr.class.getSimpleName(), ".tmp");
            int ret = new ProcessBuilder("xcode-select", "-print-path")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.to(tmpFile))
                    .start().waitFor();
            if (ret != 0) {
                throw new IOException("xcode-select failed with error code: " + ret);
            }

            return new File(new String(java.nio.file.Files.readAllBytes(tmpFile.toPath()), "UTF-8").trim());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            tmpFile.delete();
        }
    }
}
