package org.robovm.libimobiledevice.sweets;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;

public class AppleDevice {
    private final String udid;
    private final Info info;
    private State state;

    public AppleDevice(String udid, Info info) {
        this.udid = udid;
        this.info = info;
        setState(State.ONLINE);
    }

    public String getUdid() {
        return udid;
    }

    public String getSerialNumber() {
        // return last 8 chares of udid
        if (udid.length() > 8)
            return "*" + udid.substring(udid.length() - 8);
        return udid;
    }

    public State getState() {
        return state;
    }

    public void setState(State s) {
        state = s;
    }

    public String getName() {
        return info.deviceName;
    }

    public String getProductVersion() {
        return info.productVersion;
    }

    public String getBuildVersion() {
        return info.buildVersion;
    }

    public enum State {
        DISCONNECTED,
        ONLINE
    }


    public static class Info {
        private final String deviceName;
        private final String productVersion;
        private final String buildVersion;
        private final boolean hasData;

        Info(NSDictionary info) {
            deviceName = valueOrEmpty(info.get("DeviceName"));
            productVersion = valueOrEmpty(info.get("ProductVersion"));
            buildVersion = valueOrEmpty(info.get("BuildVersion"));
            hasData = true;
        }

        Info() {
            deviceName = "";
            productVersion = "";
            buildVersion = "";
            hasData = false;
        }

        private String valueOrEmpty(NSObject o) {
            return o != null ? o.toString() : "";
        }
    }
}
