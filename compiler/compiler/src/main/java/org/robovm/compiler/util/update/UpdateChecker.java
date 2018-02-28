/*
 * Copyright 2016 Justin Shapcott.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.robovm.compiler.util.update;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.robovm.compiler.Version;
import org.robovm.compiler.util.platforms.external.ExternalCommonToolchain;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Utility class that performs checks for new versions available and returns information object if any available
 */
public class UpdateChecker {
    // TODO: revert back
//    private final static String VERSION_CHECK_URL = "http://robovm.mobidevelop.com/version";
    private final static String VERSION_CHECK_URL = "https://github.com/dkimitsa/configuration-hub/raw/master/robovm/version.json";
    private final static String VERSION_CHECK_SNAPSHOT_URL = "https://github.com/dkimitsa/configuration-hub/raw/master/robovm/version.json";


    private final Version currentAppVersion;
    private final Version currentToolchainVersion;
    private final Version currentXcodeVersion;
    private final String platformId;
    private final long platformVersion;

    public UpdateChecker(Version currentAppVersion) {
        this(currentAppVersion, null, 0, null, null);
    }

    public UpdateChecker(Version currentAppVersion, String platformId, long platformVersion, Version currentToolchainVersion, Version currentXcodeVersion) {
        this.currentAppVersion = currentAppVersion;
        this.currentToolchainVersion = currentToolchainVersion;
        this.currentXcodeVersion = currentXcodeVersion;
        this.platformId = platformId;
        this.platformVersion = platformVersion;
    }

    /**
     * downloads version json and check if there is update. If app is subject for update it will
     * ask high level plugins first (using ServiceLoader)
     * if it was handled there (e.g. balloon was shown) it will return null
     * @return Update object if high level plugin doesn't handle this update notification null otherwise
     */
    public static UpdateBundle checkForUpdates() {
        return checkForUpdates(false);
    }

    private static UpdateBundle checkForUpdates(boolean silent) {
        UpdateChecker checker;
        if (ExternalCommonToolchain.isSupported()) {
            checker = new UpdateChecker(Version.getBuildVersion(),
                    ExternalCommonToolchain.getPlatformId(),
                    ExternalCommonToolchain.getPlatformToolchainVersion(),
                    ExternalCommonToolchain.getToolchainVersion(),
                    ExternalCommonToolchain.getXcodeVersion());

        } else {
            checker = new UpdateChecker(Version.getBuildVersion());
        }

        if (silent)
            return checker.updateCheck(true, true, true);
        else
            return checker.updateCheck(false, false, false);
    }

    /**
     * Same as checkForUpdates but will just gather update  data without any try to process in high level plugins
     * @return vo is there is an update null otherwise
     */
    public static UpdateBundle fetchUpdateSilent() {
        return checkForUpdates(true);
    }

    /**
     * Performs an update check. If a newer version of RoboVM is available a
     * message will be printed to the log. The update check is also used to
     * gather some anonymous usage statistics.
     * @param forced to skip time constrain checks
     * @param silent true to deliver to high level plugins
     * @param withException if true exception will be thrown otherwise just nil returned
     */
    public UpdateBundle updateCheck(boolean forced, boolean silent, boolean withException) {
        UpdateBundle updateBundle = null;
        Update appUpdate = null;
        Update toolchainUpdate = null;
        Update xcodeUpdate = null;
        try {
            if (!forced && !isTimeForUpdate())
                return null;

            updateLastUpdateCheckTime();

            // check for app update
            JSONObject versionJson = fetchVersionJson();
            appUpdate = parseVersionJson(versionJson);

            if (platformId != null) {
                // check for toolchain update
                toolchainUpdate = parseToolchainUpdate(versionJson);

                // check for xcode update
                xcodeUpdate = parseXcodeUpdate(versionJson);
            }

            // pack everything
            if (appUpdate != null || toolchainUpdate != null || xcodeUpdate != null)
                updateBundle = new UpdateBundle(appUpdate, toolchainUpdate, xcodeUpdate);

            // try to deliver this update to high level UI plugins
            if (!silent && updateBundle != null)
                updateBundle = handleInPlugins(updateBundle);


        } catch (Throwable t) {
            if (withException) {
                throw new RuntimeException(t);
            }
        }

        return updateBundle;
    }

    /**
     * delivers update to plugins registered as service
     * @param updateBundle vo
     * @return vo or null if update has to be suppressed due plugin
     */
    private UpdateBundle handleInPlugins(UpdateBundle updateBundle) {
        ServiceLoader<UpdateCheckPlugin> pluginLoader = ServiceLoader.load(UpdateCheckPlugin.class, getClass().getClassLoader());
        boolean suppress = false;
        for (UpdateCheckPlugin plugin : pluginLoader) {
            suppress |= plugin.updateAvailable(updateBundle);
        }
        return suppress ? updateBundle : null;
    }

    Update parseVersionJson(JSONObject versionJson) {
        Update update = parseReleaseVersionJson(versionJson);
        if (update == null && currentAppVersion.isSnapshot())
            update = parseSnapshotVersionJson(versionJson);

        return update;
    }

    private Update parseReleaseVersionJson(JSONObject versionJson) {
        // get release version to check about
        Version releaseVersion = null;
        String whatsNew = null;
        Map updateUrls = null;
        JSONObject releaseJson = (JSONObject) versionJson.get("release");
        if (releaseJson != null) {
            releaseVersion = new Version((String) releaseJson.get("version"));
            whatsNew = (String) releaseJson.get("description");
            updateUrls = (JSONObject) releaseJson.get("url");
        } else {
            // old style one line json ?
            String v = (String) versionJson.get("version");
            if (v != null)
                releaseVersion = new Version(v);
        }

        String updateText = null;
        if (releaseVersion != null) {
            // release to release update, version has to be higher
            if (currentAppVersion.isSnapshot()) {
                // running snapshot version, if release version same or newver it is good to go
                if (releaseVersion.getVersionCode() >= currentAppVersion.getVersionCode()) {
                    updateText = String.format("A stable version of RoboVM is available. "
                            + "Current version: %s. New version: %s.", currentAppVersion.getVersionString(),
                            releaseVersion.getVersionString());
                }
            } else if (releaseVersion.getVersionCode() > currentAppVersion.getVersionCode()) {
                updateText = String.format("A new version of RoboVM is available. "
                        + "Current version: %s. New version: %s.", currentAppVersion.getVersionString(),
                        releaseVersion.getVersionString());
            }
        }

        return (updateText != null) ? new Update(releaseVersion, updateText, whatsNew, updateUrls) : null;
    }

    private Update parseSnapshotVersionJson(JSONObject versionJson) {
        // checking for snapshot updates
        Update update = null;
        JSONObject snapshotJson = (JSONObject) versionJson.get("snapshot");
        if (snapshotJson != null) {
            Version snapshotVersion = new Version((String) snapshotJson.get("version"), (long)snapshotJson.get("build.timestamp"));
            String whatsNew = (String) snapshotJson.get("description");
            Map updateUrls = (JSONObject) snapshotJson.get("url");
            String updateText = null;

            if (snapshotVersion.getVersionCode() > currentAppVersion.getVersionCode() ||
                    (snapshotVersion.getVersionCode() == currentAppVersion.getVersionCode() &&
                    snapshotVersion.getBuildTimeStamp() > currentAppVersion.getBuildTimeStamp())) {
                // new snapshot or snapshot is same but build time is newer
                updateText = String.format("A new snapshot of RoboVM is available. New version: %s/%d.",
                        snapshotVersion.getVersionString(), snapshotVersion.getBuildTimeStamp());
            }

            if (updateText != null)
                update = new Update(snapshotVersion, updateText, whatsNew, updateUrls);
        }

        return update;
    }


    private Update parseToolchainUpdate(JSONObject versionJson) {
        // checking for toolchain updates
        Update update = null;

        Object o = versionJson.get("toolchain");
        List<JSONObject> versions = null;
        if (o != null && o instanceof JSONObject) {
            // only one version directly without array
            versions = new ArrayList<>();
            versions.add((JSONObject) o);
        } else if (o != null && o instanceof JSONArray) {
            //noinspection unchecked
            versions = (List<JSONObject>) o;
        }

        if (versions != null) {
            // find out supported version
            for (JSONObject toolchainJson : versions) {
                if (!toolchainJson.containsKey(platformId))
                    continue;
                Version toolchainVersion = new Version((String) toolchainJson.get("version"));

                // check if version is compatible (a.b.* == c.d.*)
                if (toolchainVersion.getVersionCode() / 1000 != platformVersion / 1000)
                    continue;

                if (currentToolchainVersion != null && toolchainVersion.getVersionCode() <= currentToolchainVersion.getVersionCode())
                    continue;

                // version found
                String updateText;
                if (currentToolchainVersion != null) {
                    updateText = String.format("A new version of Toolchain is available. Current version: %s. New version: %s.",
                            currentToolchainVersion.getVersionString(), toolchainVersion.getVersionString());
                } else {
                    updateText = String.format("A new version of Toolchain is available: %s.",
                            toolchainVersion.getVersionString());
                }
                Map<String, String> updateUrls = new HashMap<>();
                updateUrls.put(platformId, (String) toolchainJson.get(platformId));
                update = new Update(toolchainVersion, updateText, (String) toolchainJson.get("description"), updateUrls);
            }
        }

        return update;
    }

    private Update parseXcodeUpdate(JSONObject versionJson) {
        // checking for xcode updates
        Update update = null;
        JSONObject xcodeJson = (JSONObject) versionJson.get("xcode");
        if (xcodeJson != null) {
            Version xcodeVersion = new Version((String) xcodeJson.get("version"));
            if (currentXcodeVersion == null || xcodeVersion.getVersionCode() > currentXcodeVersion.getVersionCode()) {
                // version found
                String updateText;
                if (currentXcodeVersion != null) {
                    updateText = String.format("A new version of Xcode files is available. Current version: %s. New version: %s.",
                            currentXcodeVersion.getVersionString(), xcodeVersion.getVersionString());
                } else {
                    updateText = String.format("A new version of Xcode files is available: %s.",
                            xcodeVersion.getVersionString());
                }
                Map<String, String> updateUrls = new HashMap<>();
                updateUrls.put("xcode", (String) xcodeJson.get("url"));
                update = new Update(xcodeVersion, updateText, (String) xcodeJson.get("description"), updateUrls);
            }
        }

        return update;
    }

    private boolean isTimeForUpdate() {
        long lastCheckTime = getLastUpdateCheckTime();
        // Only check for an update once every 6 hours
        return System.currentTimeMillis() - lastCheckTime >= 6 * 60 * 60 * 1000;
    }

    private long getLastUpdateCheckTime() {
        try {
            File timeFile = new File(new File(System.getProperty("user.home"), ".robovm"), "last-update-check");
            return timeFile.exists() ? Long.parseLong(FileUtils.readFileToString(timeFile, "UTF-8").trim()) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    private void updateLastUpdateCheckTime() throws IOException {
        File timeFile = new File(new File(System.getProperty("user.home"), ".robovm"), "last-update-check");
        timeFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(timeFile, String.valueOf(System.currentTimeMillis()), "UTF-8");
    }

    private JSONObject fetchVersionJson() throws UnsupportedEncodingException, InterruptedException {
        String osName = System.getProperty("os.name", "Unknown");
        String osArch = System.getProperty("os.arch", "Unknown");
        String osVersion = System.getProperty("os.version", "Unknown");

        String url = currentAppVersion.isSnapshot() ? VERSION_CHECK_SNAPSHOT_URL : VERSION_CHECK_URL;
        final String address = url + "?"
                + "version=" + URLEncoder.encode(Version.getVersion(), "UTF-8") + "&"
                + "osName=" + URLEncoder.encode(osName, "UTF-8") + "&"
                + "osArch=" + URLEncoder.encode(osArch, "UTF-8") + "&"
                + "osVersion=" + URLEncoder.encode(osVersion, "UTF-8");

        final JSONObject[] result = new JSONObject[1];
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(address);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setUseCaches(false);
                    conn.setConnectTimeout(5 * 1000);
                    conn.setReadTimeout(5 * 1000);
                    conn.setAllowUserInteraction(false);
                    conn.setInstanceFollowRedirects(true);
                    int responseCode = conn.getResponseCode();
                    try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
                        result[0] = (JSONObject) JSONValue.parseWithException(IOUtils.toString(in, "UTF-8"));
                    }
                } catch (Exception ignored) {
                }
            }
        });

        t.start();
        t.join(5 * 1000); // Wait for a maximum of 5 seconds
        return result[0];
    }


    public static class Update {
        private final Version version;
        private final String updateText;
        private final String updateWhatsNew;
        private final Map updateUrls;

        Update(Version version, String updateText, String updateWhatsNew, Map updateUrls) {
            this.version = version;
            this.updateText = updateText;
            this.updateWhatsNew = updateWhatsNew;
            this.updateUrls = updateUrls;
        }

        public Version getVersion() {
            return version;
        }

        public String getUpdateText() {
            return updateText;
        }

        public String getUpdateWhatsNew() {
            return updateWhatsNew;
        }

        public String getUpdateUrlForKey(String key) {
            String url = null;
            if (updateUrls != null) {
                try {
                    url = (String)updateUrls.get(key);
                } catch (Throwable ignored) {
                }
            }
            return url;
        }

        @Override
        public String toString() {
            String res = updateText;
            if (updateWhatsNew != null)
                res += "\nWhat's new:\n" + updateWhatsNew;
            return res;
        }
    }

    public static class UpdateBundle {
        private final Update appUpdate;
        private final Update toolchainUpdate;
        private final Update xcodeUpdate;

        public UpdateBundle(Update appUpdate, Update toolchainUpdate, Update xcodeUpdate) {
            this.appUpdate = appUpdate;
            this.toolchainUpdate = toolchainUpdate;
            this.xcodeUpdate = xcodeUpdate;
        }

        public Update getAppUpdate() {
            return appUpdate;
        }

        public Update getToolchainUpdate() {
            return toolchainUpdate;
        }

        public Update getXcodeUpdate() {
            return xcodeUpdate;
        }

        @Override
        public String toString() {
            return "UpdateBundle{" +
                    "appUpdate=" + appUpdate +
                    ", toolchainUpdate=" + toolchainUpdate +
                    ", xcodeUpdate=" + xcodeUpdate +
                    '}';
        }
    }
}
