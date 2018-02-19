/*
 * Copyright (C) 2012 RoboVM AB
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
package org.robovm.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

/**
 * Reads the compiler version from auto generated <code>version.properties</code> file.
 */
public class Version {
    private static String PROPERTIES_RESOURCE = "/META-INF/robovm/version.properties";

    // version number and build timestamp that is read from version.properties
    private static Version buildVersion = null;
    private static long buildTimeStamp = 0;


    /** version string as it passed to constructor */
    private String versionSting;

    /** version code including numbers only, e.g. 1.2.3 >  1 * 1000 * 1000 + 2 * 1000 + 3 */
    private long versionCode;

    /** version code including snapshot/beta etc status */
    private long versionCodeEx;

    /** type of version */
    private enum Type {
        REGULAR,
        SNAPSHOT,
        ALPHA,
        BETA,
        RC
    }
    Type type;

    public Version(String v) {
        versionSting = v;

        String buildPart;
        long buildType;
        if (v.endsWith("-SNAPSHOT")) {
            buildPart = "";
            v = v.substring(0, v.indexOf("-SNAPSHOT"));
            buildType = 0;
            type = Type.SNAPSHOT;
        } else if (v.contains("-alpha-")) {
            buildPart = v.substring(v.lastIndexOf('-') + 1);
            v = v.substring(0, v.indexOf("-alpha-"));
            buildType = 100;
            type = Type.ALPHA;
        } else if (v.contains("-beta-")) {
            buildPart = v.substring(v.lastIndexOf('-') + 1);
            v = v.substring(0, v.indexOf("-beta-"));
            buildType = 300;
            type = Type.BETA;
        } else if (v.contains("-rc-")) {
            buildPart = v.substring(v.lastIndexOf('-') + 1);
            v = v.substring(0, v.indexOf("-rc-"));
            buildType = 500;
            type = Type.RC;
        } else {
            buildPart = "1";
            buildType = 700;
            type = Type.REGULAR;
        }

        String[] parts = v.split("\\.");
        if (parts.length > 3) {
            throw new IllegalArgumentException("Illegal version number: " + v);
        }

        long major = parts.length > 0 ? Long.parseLong(parts[0]) : 0;
        long minor = parts.length > 1 ? Long.parseLong(parts[1]) : 0;
        long rev = parts.length > 2 ? Long.parseLong(parts[2]) : 0;
        long build = buildPart.isEmpty() ? 0 : Long.parseLong(buildPart);
        versionCodeEx = (((major * 1000 + minor) * 1000 + rev) * 1000) + build + buildType;
        versionCode = (major * 1000 + minor) * 1000 + rev;
    }

    /**
     * reads out build version information from property resource
     */
    private static void readBuildVersion() {
        InputStream is = null;
        try {
            is = Version.class.getResourceAsStream(PROPERTIES_RESOURCE);
            Properties props = new Properties();
            props.load(is);
            buildVersion = new Version(props.getProperty("version"));
            try {
                buildTimeStamp = Long.parseLong(props.getProperty("build.timestamp"));
            } catch (NumberFormatException ignored) {
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * Returns build version object
     * if it is not initialized -- initializes it by reading version property file from resources
     * also initializes build timestamp
     */
    public static Version getBuildVersion() {
        if (buildVersion == null) {
            readBuildVersion();
        }

        return buildVersion;
    }

    /**
     * Returns time stamp when this version was build
     *
     * @return the timestamp
     */
    public static long getBuildTimeStamp() {
        if (buildVersion == null) {
            readBuildVersion();
        }

        return buildTimeStamp;
    }

    /**
     * Returns the version number of the compiler by reading the <code>version.properties</code>
     * file.
     * 
     * @return the version.
     */
    public static String getVersion() {
        return getBuildVersion().versionSting;
    }

    /**
     * @return true if version is snapshot version
     */
    public boolean isSnapshot() {
        return type == Type.SNAPSHOT;
    }

    /**
     * Return packed version code a.b.c in way a * 1000 * 1000 + b * 1000 + c
     * @return version code
     */
    public long getVersionCode() {
        return versionCode;
    }

    /**
     * Return extended version same as packed but also includes beta/snapshot bits
     * @return version code
     */
    public long getVersionCodeEx() {
        return versionCodeEx;
    }

    /**
     * Returns <code>true</code> if this version is less than the specified
     * version number.
     */
    public static boolean isOlderThan(String otherVersion) {
        return getBuildVersion().versionCodeEx < new Version(otherVersion).versionCodeEx;
    }
    
    public static void main(String[] args) {
        System.out.println(new Version("1.0.0-alpha-01").versionCodeEx);
    }
}
