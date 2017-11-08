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
package org.robovm.compiler.target.ios;

import org.robovm.compiler.config.Arch;
import org.robovm.compiler.util.platforms.ToolchainUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Simulator device types, consisting of the device type id and SDK version as
 * listed by xcrun simctl devices -j list.
 */
public class DeviceType implements Comparable<DeviceType> {
    public static final String PREFIX = "com.apple.CoreSimulator.SimDeviceType.";
    public static final String PREFERRED_IPHONE_SIM_NAME = PREFIX + "iPhone 6";
    public static final String PREFERRED_IPAD_SIM_NAME = PREFIX + "iPad Air";
    
    public static final String[] ONLY_32BIT_DEVICES = {"iPhone 4", "iPhone 4s", "iPhone 5", "iPhone 5c", "iPad 2"};

    public static enum DeviceFamily {
        iPhone,
        iPad
    }

    private final String deviceName;
    private final String udid;
    private final String state;
    private final SDK sdk;
    private final Set<Arch> archs;

    public DeviceType(String deviceName, String udid, String state, SDK sdk, Set<Arch> archs) {
        this.deviceName = deviceName;
        this.udid = udid;
        this.state = state;
        this.sdk = sdk;
        this.archs = archs;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public SDK getSdk() {
        return sdk;
    }

    public Set<Arch> getArchs() {
        return Collections.unmodifiableSet(archs);
    }

    /**
     * @return id as understood by the AppCompiler -simdevicetype flag
     */
    public String getSimpleDeviceTypeId() {
        return getSimpleDeviceName() + ", " + sdk.getVersion();
    }

    public String getSimpleDeviceName() {
        return deviceName + " (" + udid + ")";
    }

    public DeviceFamily getFamily() {
        if (deviceName.contains("iPhone")) {
            return DeviceFamily.iPhone;
        } else {
            return DeviceFamily.iPad;
        }
    }
    
    public String getUdid() {
		return udid;
	}
    
    public String getState() {
    	return state;
    }

    public static List<DeviceType> listDeviceTypes() {
        return ToolchainUtil.listSimulatorDeviceTypes();
    }

    @Override
    public int compareTo(DeviceType that) {
        int c = this.sdk.compareTo(that.sdk);
        if (c == 0) {
            c = this.getFamily().compareTo(that.getFamily());
            if (c == 0) {
                c = this.deviceName.compareToIgnoreCase(that.deviceName);
            }
        }
        return c;
    }

    private static List<DeviceType> filter(List<DeviceType> deviceTypes, Arch arch,
            DeviceFamily family, String deviceName, String sdkVersion) {

        deviceName = deviceName == null ? null : deviceName.toLowerCase().replaceAll("-", " ");

        List<DeviceType> result = new ArrayList<>();
        for (DeviceType type : deviceTypes) {
            if (arch == null || type.getArchs().contains(arch)) {
                if (family == null || family == type.getFamily()) {
                    if (deviceName == null || type.getSimpleDeviceName().toLowerCase().contains(deviceName)) {
                        if (sdkVersion == null || type.getSdk().getVersion().equals(sdkVersion)) {
                            result.add(type);
                        }
                    }
                }
            }
        }
        return result;
    }

    public static List<String> getSimpleDeviceTypeIds() {
        List<String> result = new ArrayList<>();
        for (DeviceType type : listDeviceTypes()) {
            result.add(type.getSimpleDeviceTypeId());
        }
        return result;
    }

    public static DeviceType getDeviceType(String displayName) {
        List<DeviceType> types = listDeviceTypes();
        if (displayName == null) {
            return null;
        }
        for (DeviceType type : types) {
            if (displayName.equals(type.getSimpleDeviceTypeId())) {
                return type;
            }
        }
        return null;
    }

    public static DeviceType getBestDeviceType() {
        return getBestDeviceType(null, null, null, null);
    }

    public static DeviceType getBestDeviceType(DeviceFamily family) {
        return getBestDeviceType(null, family, null, null);
    }

    /**
     * Returns the best {@link DeviceType} matching the parameters. If multiple
     * device types match the parameters the first one with the highest SDK
     * number will be returned. If no device name and no {@link DeviceFamily} is
     * specified this method will default to {@link DeviceFamily#iPhone}.
     */
    public static DeviceType getBestDeviceType(Arch arch, DeviceFamily family,
            String deviceName, String sdkVersion) {

        if (deviceName == null && family == null) {
            family = DeviceFamily.iPhone;
        }
        String preferredDeciveName = PREFERRED_IPHONE_SIM_NAME;
        if (family == DeviceFamily.iPad) {
            preferredDeciveName = PREFERRED_IPAD_SIM_NAME;
        }

        DeviceType best = null;
        for (DeviceType type : filter(listDeviceTypes(), arch, family, deviceName, sdkVersion)) {
            if (best == null) {
                best = type;
            } else if (type.getSdk().compareTo(best.getSdk()) > 0 ||
                    type.getSdk().compareTo(best.getSdk()) == 0 && type.getDeviceName().equals(preferredDeciveName)) {
                best = type;
            }
        }
        if (best == null) {
            throw new IllegalArgumentException("Unable to find a matching device "
                    + "[arch=" + arch + ", family=" + family
                    + ", name=" + deviceName + ", sdk=" + sdkVersion + "]");
        }
        return best;
    }

    @Override
    public String toString() {
        return "DeviceType [deviceName=" + deviceName + ", sdk=" + sdk + ", archs=" + archs + "]";
    }

	
}
