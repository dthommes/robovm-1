package org.robovm.libimobiledevice.util;

import org.jetbrains.annotations.NotNull;
import org.robovm.libimobiledevice.IDevice;
import org.robovm.libimobiledevice.LibIMobileDeviceException;
import org.robovm.libimobiledevice.LockdowndClient;
import org.robovm.libimobiledevice.LockdowndServiceDescriptor;
import org.robovm.libimobiledevice.SyslogRelayClient;
import org.robovm.libimobiledevice.binding.LockdowndError;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SyslogRelay implements Runnable {
    private final IDevice device;
    private Thread mThread = new Thread(this);
    private Listener listener;

    /**
     * Listener which gets called when there is syslog outputs
     */
    public interface Listener {
        void syslogStatusChanged(IDevice device, String status);
        void syslogLineReceived(IDevice device, String line);
        void syslogFailed(IDevice device, Throwable th);

        Listener EMPTY = new Listener() {
            @Override public void syslogStatusChanged(IDevice device, String status) {}
            @Override public void syslogLineReceived(IDevice device, String line) {}
            @Override public void syslogFailed(IDevice device, Throwable th) {}
        };
    }


    public SyslogRelay(IDevice device, Listener listener) {
        this.device = device;
        this.listener = listener;
        mThread.start();
    }

    @Override
    public void run() {
        try {
            doWork();
        } catch (InterruptedException e) {
            // canceled nothing to do
        } catch (Throwable t) {
            // unhandled exception
            listener.syslogFailed(device, t);
        }
    }

    private void doWork() throws Exception {
        // performing lockdown
        int lastErrorToSkip = LockdowndError.LOCKDOWN_E_SUCCESS.swigValue();
        while (!mThread.isInterrupted()) {
            try (LockdowndClient lockdowndClient = new LockdowndClient(device, getClass().getSimpleName(), true)) {
                if (lastErrorToSkip != LockdowndError.LOCKDOWN_E_SUCCESS.swigValue()) {
                    // reset status
                    listener.syslogStatusChanged(device, null);
                }

                // next step
                workWithLockdown(lockdowndClient);
            } catch (LibIMobileDeviceException e) {
                // it is an error that shall be skipped
                if (lastErrorToSkip != e.getErrorCode()) {

                    if (e.getErrorCode() == LockdowndError.LOCKDOWN_E_PASSWORD_PROTECTED.swigValue()) {
                        // device is locked
                        listener.syslogStatusChanged(device, "Device is locked, please unlock...");
                    } else if (e.getErrorCode() == LockdowndError.LOCKDOWN_E_PAIRING_DIALOG_RESPONSE_PENDING.swigValue()) {
                        // device is not paired
                        listener.syslogStatusChanged(device, "Device is not paired, please complete pairing");
                    } else if (e.getErrorCode() == LockdowndError.LOCKDOWN_E_INVALID_CONF.swigValue()) {
                        // device is not paired
                        listener.syslogStatusChanged(device, "Probably, device is not paired, please complete pairing");
                    } else
                        throw e;
                }

                lastErrorToSkip = e.getErrorCode();
                Thread.sleep(500);
            }
        }
    }

    private void workWithLockdown(LockdowndClient lockdowndClient) {
        LockdowndServiceDescriptor slrService = lockdowndClient.startService(SyslogRelayClient.SERVICE_NAME);
        try (SyslogRelayClient srlClient = new SyslogRelayClient(device, slrService)) {
            InputStream is = new SyslogRelayInputStream(srlClient);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while (!mThread.isInterrupted()) {
                try {
                    String line = br.readLine();

                    // device returns #0 after LF and it gets to string as string character. which looks ugly
                    if (line.length() > 0 && line.charAt(0) == 0)
                        line = line.substring(1);

                    listener.syslogLineReceived(device, line);
                } catch (Throwable ignored) {
                    // there also will go IOException from rb.readLine() which will happen in case thread is
                    // interrupted
                }
            }
        }
    }

    public void shutdown() {
        synchronized (this) {
            if (!mThread.isInterrupted()) {
                mThread.interrupt();
                listener = Listener.EMPTY;
            }
        }
    }

    /**
     * wraps receive from Syslog Relay service into IS that allows to use it
     * with all infrastructure
     */
    private static class SyslogRelayInputStream extends InputStream {
        private final SyslogRelayClient srlClient;

        SyslogRelayInputStream(SyslogRelayClient srlClient) {
            this.srlClient = srlClient;
        }

        @Override
        public int read() {
            byte[] bb = new byte[1];
            if (read(bb) < 0)
                return -1;
            return bb[0];
        }


        @Override
        public int read(@NotNull byte[] b) {
            int received = 0;
            while (!Thread.currentThread().isInterrupted() &&  received == 0)
                received = srlClient.receive(b, 1);

            if (Thread.currentThread().isInterrupted())
                received = -1;

            return received;
        }


        @Override
        public int read(@NotNull byte[] b, int off, int len) {
            int received;
            if (off == 0 && b.length == len) {
                // short way as it is full array read-out, can do it without allocating of buffer
                received =  read(b);
            } else {
                byte[] bb = new byte[len];
                received = read(bb);
                if (received > 0) {
                    System.arraycopy(bb, 0, b, off, received);
                }
            }

            return received;
        }
    }


    public static void main(String[] args) throws Exception {
        String[] udids = IDevice.listUdids();
        if (udids.length == 0) {
            System.err.println("No devices connected");
            return;
        }

        new SyslogRelay(new IDevice(udids[0]), new Listener() {
            @Override
            public void syslogStatusChanged(IDevice device, String status) {
                System.out.println(device + ":  " + status);
            }

            @Override
            public void syslogLineReceived(IDevice device, String line) {
                System.out.println(line);
            }

            @Override
            public void syslogFailed(IDevice device, Throwable th) {
                System.out.println("Failed:  " + th);
                th.printStackTrace();
            }
        });

        Thread.sleep(Long.MAX_VALUE);
    }

}
