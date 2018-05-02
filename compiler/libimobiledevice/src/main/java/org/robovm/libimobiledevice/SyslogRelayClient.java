package org.robovm.libimobiledevice;

import org.robovm.libimobiledevice.binding.IntOut;
import org.robovm.libimobiledevice.binding.LibIMobileDevice;
import org.robovm.libimobiledevice.binding.LibIMobileDeviceConstants;
import org.robovm.libimobiledevice.binding.LockdowndServiceDescriptorStruct;
import org.robovm.libimobiledevice.binding.SyslogRelayClientRef;
import org.robovm.libimobiledevice.binding.SyslogRelayClientRefOut;
import org.robovm.libimobiledevice.binding.SyslogRelayError;

/**
 * Relays syslog messages
 * @author dkimitsa
 */
public class SyslogRelayClient implements AutoCloseable {
    public static final String SERVICE_NAME = LibIMobileDeviceConstants.SYSLOG_RELAY_SERVICE_NAME;

    protected SyslogRelayClientRef ref;

    /**
     * Creates a new {@link SyslogRelayClient} and makes a connection to
     * the {@code ccom.apple.syslog_relay} service on the device.
     *
     * @param device the device to connect to.
     * @param service the service descriptor returned by {@link LockdowndClient#startService(String)}.
     */
    public SyslogRelayClient(IDevice device, LockdowndServiceDescriptor service) {
        if (device == null) {
            throw new NullPointerException("device");
        }
        if (service == null) {
            throw new NullPointerException("service");
        }

        SyslogRelayClientRefOut refOut = new SyslogRelayClientRefOut();
        LockdowndServiceDescriptorStruct serviceStruct = new LockdowndServiceDescriptorStruct();
        serviceStruct.setPort((short) service.getPort());
        serviceStruct.setSslEnabled(service.isSslEnabled());
        try {
            checkResult(LibIMobileDevice.syslog_relay_client_new(device.getRef(), serviceStruct, refOut));
            this.ref = refOut.getValue();
        } finally {
            serviceStruct.delete();
            refOut.delete();
        }
    }

    /**
     * dkimitsa: it is a placeholder as callback is not used
     */
    @Deprecated
    public void startCapture() {
        checkResult(LibIMobileDevice.syslog_relay_start_capture(ref, LibIMobileDevice.get_global_syslog_relay_cb(), 0));
    }

    /**
     * dkimitsa: it is a placeholder as callback is not used
     */
    @Deprecated
    public void stopCapture() {
        checkResult(LibIMobileDevice.syslog_relay_stop_capture(ref));
    }


    public int receive(byte[] buffer, long timeout) {
        IntOut received = new IntOut();
        checkResult(LibIMobileDevice.syslog_relay_receive_with_timeout(ref, buffer, buffer.length, received, timeout));
        return received.getValue();
    }

    public int receive(byte[] buffer) {
        IntOut received = new IntOut();
        checkResult(LibIMobileDevice.syslog_relay_receive(ref, buffer, buffer.length, received));
        return received.getValue();
    }

    private void checkDisposed() {
        if (ref == null) {
            throw new LibIMobileDeviceException("Already disposed");
        }
    }

    public synchronized void dispose() {
        checkDisposed();
        LibIMobileDevice.syslog_relay_stop_capture(ref);
        LibIMobileDevice.syslog_relay_client_free(ref);
        ref = null;
    }

    public void close() {
        dispose();
    }

    private static void checkResult(SyslogRelayError result) {
        if (result != SyslogRelayError.SYSLOG_RELAY_E_SUCCESS) {
            throw new LibIMobileDeviceException(result.swigValue(), result.name());
        }
    }
}
