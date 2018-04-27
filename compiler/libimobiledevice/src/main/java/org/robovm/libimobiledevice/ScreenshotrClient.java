package org.robovm.libimobiledevice;

import org.robovm.libimobiledevice.binding.ByteArray;
import org.robovm.libimobiledevice.binding.ByteArrayOut;
import org.robovm.libimobiledevice.binding.LibIMobileDevice;
import org.robovm.libimobiledevice.binding.LibIMobileDeviceConstants;
import org.robovm.libimobiledevice.binding.LockdowndError;
import org.robovm.libimobiledevice.binding.LockdowndServiceDescriptorStruct;
import org.robovm.libimobiledevice.binding.LongOut;
import org.robovm.libimobiledevice.binding.ScreenShotrClientRef;
import org.robovm.libimobiledevice.binding.ScreenShotrClientRefOut;
import org.robovm.libimobiledevice.binding.ScreenShotrError;
import org.robovm.libimobiledevice.util.MobileImageMounter;

/**
 * Client to capture screenshots
 * @author dkimitsa
 */
public class ScreenshotrClient implements AutoCloseable {
    public static final String SERVICE_NAME = LibIMobileDeviceConstants.SCREENSHOTR_SERVICE_NAME;

    protected ScreenShotrClientRef ref;

    /**
     * Creates a new {@link ScreenshotrClient} and makes a connection to
     * the {@code ccom.apple.syslog_relay} service on the device.
     *
     * @param device the device to connect to.
     * @param service the service descriptor returned by {@link LockdowndClient#startService(String)}.
     */
    public ScreenshotrClient(IDevice device, LockdowndServiceDescriptor service) {
        if (device == null) {
            throw new NullPointerException("device");
        }
        if (service == null) {
            throw new NullPointerException("service");
        }

        ScreenShotrClientRefOut refOut = new ScreenShotrClientRefOut();
        LockdowndServiceDescriptorStruct serviceStruct = new LockdowndServiceDescriptorStruct();
        serviceStruct.setPort((short) service.getPort());
        serviceStruct.setSslEnabled(service.isSslEnabled());
        try {
            checkResult(LibIMobileDevice.screenshotr_client_new(device.getRef(), serviceStruct, refOut));
            this.ref = refOut.getValue();
        } finally {
            serviceStruct.delete();
            refOut.delete();
        }
    }

    public byte[] captureScreenshotTiff() {
        ByteArrayOut imgDataOut = new ByteArrayOut();
        LongOut lengthOut = new LongOut();
        checkResult(LibIMobileDevice.screenshotr_take_screenshot(ref, imgDataOut, lengthOut));

        int length = (int) lengthOut.getValue();
        ByteArray plistBin = imgDataOut.getValue();
        if (length == 0 || plistBin == null) {
            return null;
        }
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = plistBin.get(i);
        }
        return data;
    }


    protected final void checkDisposed() {
        if (ref == null) {
            throw new LibIMobileDeviceException("Already disposed");
        }
    }

    public synchronized void dispose() {
        checkDisposed();
        LibIMobileDevice.screenshotr_client_free(ref);
        ref = null;
    }

    public void close() throws Exception {
        dispose();
    }

    private static void checkResult(ScreenShotrError result) {
        if (result != ScreenShotrError.SCREENSHOTR_E_SUCCESS) {
            throw new LibIMobileDeviceException(result.swigValue(), result.name());
        }
    }

}
