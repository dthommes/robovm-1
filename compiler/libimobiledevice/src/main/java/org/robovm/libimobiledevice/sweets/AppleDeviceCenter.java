package org.robovm.libimobiledevice.sweets;

import com.dd.plist.NSDictionary;
import org.robovm.libimobiledevice.IDevice;
import org.robovm.libimobiledevice.LockdowndClient;
import org.robovm.libimobiledevice.util.Screenshotr;
import org.robovm.libimobiledevice.util.SyslogRelay;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Simple usage utility that wrap around libmobiledevice
 * @author dkimitsa
 */
public final class AppleDeviceCenter  {
    private static final Logger LOG = Logger.getLogger(AppleDeviceCenter.class.getSimpleName());

    /**
     * interface to receive notifications when device added/removed
     */
    public interface DeviceChangeListener {
        void deviceAdded(AppleDevice d);
        void deviceRemoved(AppleDevice d);
    }

    /**
     * interface to receive system logs from device
     */
    public interface DeviceLogListener {
        void deviceLogStatusUpdate(AppleDevice d, String line);
        void deviceLogReceived(AppleDevice d, String line);
        void deviceLogFailedWithError(AppleDevice d, Throwable t);
    }


    public static AppleDevice[] getConnectedDevices() {
        return instance.getConnectedDevices();
    }

    public static void addDeviceChangeListener(DeviceChangeListener l) {
        instance.addDeviceChangeListener(l);
    }

    public static void removeDeviceChangeListener(DeviceChangeListener l) {
        instance.removeDeviceChangeListener(l);
    }

    public static void addDeviceLogListener(AppleDevice d, DeviceLogListener l) {
        instance.addDeviceLogListener(d, l);
    }

    public static void removeDeviceLogListener(AppleDevice d, DeviceLogListener l) {
        instance.removeDeviceLogListener(d, l);
    }

    public static byte[] captureScreenshot(AppleDevice d) throws Exception {
        return instance.captureScreenshot(d);
    }

    //
    // Implementation bellow
    //

    private static IMPL instance = new IMPL();
    private static class IMPL implements IDevice.EventListener {

        List<AppleDevice> devices = new LinkedList<>();
        CompositeNotifier<DeviceChangeListener> deviceChangeListener = new CompositeNotifier<>(DeviceChangeListener.class);
        Map<String, DeviceContext> contexts = new Hashtable<>();


        private IMPL() {
            init();
        }

        private synchronized void init() {
            try {
                IDevice.addEventListener(this);
                String[] udids = IDevice.listUdids();
                for (String u : udids) {
                    deviceAdded(u);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        private synchronized AppleDevice[] getConnectedDevices() {
            AppleDevice[] res = new AppleDevice[devices.size()];
            return devices.toArray(res);
        }

        private void addDeviceChangeListener(DeviceChangeListener l) {
            deviceChangeListener.add(l);
        }

        private synchronized void removeDeviceChangeListener(DeviceChangeListener l) {
            deviceChangeListener.remove(l);
        }

        private synchronized void addDeviceLogListener(AppleDevice d, DeviceLogListener l) {
            DeviceContext c = contexts.get(d.getUdid());
            if (c != null) {
                c.addDeviceLogListener(l);
            }
        }

        private synchronized void removeDeviceLogListener(AppleDevice d, DeviceLogListener l) {
            DeviceContext c = contexts.get(d.getUdid());
            if (c != null) {
                c.removeDeviceLogListener(l);
            }
        }

        @Override
        public void deviceAdded(String udid) {
            LOG.info("Device connected: " + udid);

            synchronized (this) {
                if (!contexts.containsKey(udid)) {
                    DeviceContext c = new DeviceContext(udid);
                    contexts.put(udid, c);
                    devices.add(c.device);
                    deviceChangeListener.proxy.deviceAdded(c.device);
                }
            }
        }

        @Override
        public void deviceRemoved(String udid) {
            LOG.info("Device disconnected: " + udid);

            synchronized (this) {
                DeviceContext c = contexts.remove(udid);
                if (c != null) {
                    devices.remove(c.device);
                    deviceChangeListener.proxy.deviceRemoved(c.device);
                    c.shutdown();
                }
            }
        }

        public byte[] captureScreenshot(AppleDevice d) throws Exception {
            DeviceContext c = contexts.get(d.getUdid());
            if (c != null) {
                return Screenshotr.captureScreenshotTiff(c.idevice);
            } else {
                throw new Error("Invalid device " + d.getUdid());
            }
        }
    }


    /**
     * state class for devices
     */
    private static class DeviceContext implements SyslogRelay.Listener {
        IDevice idevice;
        final AppleDevice device;
        final CompositeNotifier<DeviceLogListener> deviceLogListeners = new CompositeNotifier<>(DeviceLogListener.class);
        SyslogRelay syslogReceiver;

        DeviceContext(String udid) {
            idevice = new IDevice(udid);
            AppleDevice.Info deviceInfo;

            // creating device object
            try (LockdowndClient client = new LockdowndClient(idevice, this.getClass().getSimpleName(), false)) {
                NSDictionary dict = (NSDictionary) client.getValue(null, null);
                deviceInfo = new AppleDevice.Info(dict);
            } catch (Throwable e) {
                e.printStackTrace();

                // use dummy device info
                deviceInfo = new AppleDevice.Info();
            }


            device = new AppleDevice(udid, deviceInfo);
        }

        synchronized void shutdown() {
            if (syslogReceiver != null) {
                syslogReceiver.shutdown();
                syslogReceiver = null;
            }

            if (idevice != null) {
                idevice.close();
            }
        }

        boolean isShutdown() {
            return idevice == null;
        }

        synchronized void addDeviceLogListener(DeviceLogListener l) {
            if (isShutdown())
                return;

            deviceLogListeners.add(l);

            // start receiving logs
            if (syslogReceiver == null) {
                syslogReceiver = new SyslogRelay(idevice, this);
            }
        }

        synchronized void removeDeviceLogListener(DeviceLogListener l) {
            if (isShutdown())
                return;

            deviceLogListeners.remove(l);

            if (syslogReceiver != null && deviceLogListeners.items.isEmpty()) {
                syslogReceiver.shutdown();
                syslogReceiver = null;
            }
        }

        @Override
        public void syslogStatusChanged(IDevice i, String status) {
            if (!isShutdown()) {
                deviceLogListeners.proxy.deviceLogStatusUpdate(device, status);
            }
        }

        @Override
        public void syslogLineReceived(IDevice i, String line) {
            if (!isShutdown()) {
                deviceLogListeners.proxy.deviceLogReceived(device, line);
            }
        }

        @Override
        public void syslogFailed(IDevice i, Throwable th) {
            if (!isShutdown()) {
                deviceLogListeners.proxy.deviceLogFailedWithError(device, th);
            }
        }
    }


    /**
     * Composite event notifier, based on IntellijIdea platform/util/src/com/intellij/util/EventDispatcher.java
     * (simplified)
     */
    private static class CompositeNotifier<I> {
        private final I proxy;
        private final List<I> items = new CopyOnWriteArrayList<>();

        private CompositeNotifier(Class<I> prototype) {
            InvocationHandler handler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
                    if (method.getDeclaringClass().getName().equals("java.lang.Object")) {
                        String methodName = method.getName();
                        switch (methodName) {
                            case "toString":
                                return "Multicaster";
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0] ? Boolean.TRUE : Boolean.FALSE;
                            default:
                                LOG.severe("Incorrect Object's method invoked for proxy:" + methodName);
                                return null;
                        }
                    } else {
                        synchronized (items) {
                            method.setAccessible(true);
                            for (I i : items) {
                                try {
                                    method.invoke(i, args);
                                } catch (Exception e) {
                                    e.printStackTrace();;
                                }
                            }
                        }
                        return null;
                    }
                }
            };

            //noinspection unchecked
            proxy = (I)Proxy.newProxyInstance(prototype.getClassLoader(), new Class[]{prototype}, handler);
        }

        private void add(I i) {
            items.add(i);
        }

        private void remove(I i) {
            items.remove(i);
        }

        private I notifier() {
            return proxy;
        }
    }
}
