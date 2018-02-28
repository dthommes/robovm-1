package org.robovm.compiler.util.platforms.external;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.robovm.compiler.util.update.UpdateChecker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * class that responsible for installation of Toolchain and Xcode files
 */
public class ExternalCommonToolchainInstaller {

    /**
     * listener interface to deliver callbacks
     */
    public interface ProgressListener {
        void installerOnProgress(String message, float progress);
        void installerFinished();
        void installerFailed(Throwable e);
    }
    private final ProgressListener listener;
    private int lastProgressValueFilter = -1;
    private File installTmpFolder;


    public ExternalCommonToolchainInstaller(ProgressListener listener) {
        this.listener = listener;
    }


    public void install() {
        try {
            // checking for udpate even if it was checked just before
            UpdateChecker.UpdateBundle update = UpdateChecker.fetchUpdateSilent();
            deliverProgress("Re-checking for update", 0f);

            if (update != null && (update.getXcodeUpdate() != null || update.getToolchainUpdate() != null)) {
                // create a tmp folder for operations
                installTmpFolder = new File(System.getProperty("user.home") + "/.robovm/platform/tmp/install-" + UUID.randomUUID());
                if (!installTmpFolder.mkdirs())
                    throw new IOException("Unable to create tmp folder for installation");

                int filesCount = 0;
                int fileIdx = 0;

                UpdateChecker.Update toolchainUpdate = update.getToolchainUpdate();
                if (toolchainUpdate != null)
                    filesCount += 1;
                UpdateChecker.Update xcodeUpdate = update.getXcodeUpdate();
                if (xcodeUpdate != null)
                    filesCount += 1;

                // install toolchain
                if (toolchainUpdate != null) {
                    String prefix = "Toolchain";
                    if (filesCount > 1)
                        prefix = "(" + Integer.toString(fileIdx + 1) + "/" + filesCount + ") " + prefix;
                    String url = toolchainUpdate.getUpdateUrlForKey(ExternalCommonToolchain.getPlatformId());
                    installToolchain(url, prefix, 0f, 1f);
                    fileIdx += 1;
                }

                // install xcode files
                if (xcodeUpdate != null) {
                    String prefix = "iOS SDK";
                    if (filesCount > 1)
                        prefix = "(" + Integer.toString(fileIdx + 1) + "/" + filesCount + ") " + prefix;
                    String url = xcodeUpdate.getUpdateUrlForKey("xcode");
                    installXcodeFiles(url, prefix, 0f, 1f);
                    // fileIdx += 1;
                }
            }

            listener.installerFinished();
        } catch (Throwable e) {
            // something went wrong
            listener.installerFailed(e);
        } finally {
            if (installTmpFolder != null) {
                installTmpFolder.deleteOnExit();
            }
        }
    }

    private void installToolchain(String url, String progressPrefix, float progressStart, float progressLength) throws IOException, InterruptedException {
        // downloading file to temp location
        // assumption that time to download data is 90% and to extract is 10%

        // start downloading
        File downloadFile = new File(installTmpFolder, buildFileNameFromUrl("toolchain.download", url));
        downloadFile(url, downloadFile, progressPrefix + ": downloading",
                progressStart, progressLength * 0.9f);

        // unzipping to temp location
        File unzipFolder = new File(installTmpFolder, "toolchain.unzip");
        if (!unzipFolder.mkdirs())
            throw new IOException("Unable to create tmp folder for toolchain");
        unpack(downloadFile, unzipFolder, progressPrefix + ": unpacking", progressStart + progressLength * 0.9f,
                progressLength * 0.1f);

        // installing
        File installLocation = new File(System.getProperty("user.home") + "/.robovm/platform/" +
                ExternalCommonToolchain.getPlatformId());
        if (installLocation.exists()) {
            // remove old
            FileUtils.deleteDirectory(installLocation);
        }

        // move unzipped location to dir
        File srcInstallFolder = new File(unzipFolder, ExternalCommonToolchain.getPlatformId());
        FileUtils.moveDirectory(srcInstallFolder, installLocation);

        // re-init toolchain
        ExternalCommonToolchain.reInitToolchain();
    }

    private void installXcodeFiles(String url, String progressPrefix, float progressStart, float progressLength) throws IOException, InterruptedException {
        // downloading file to temp location
        // assumption that time to download data is 90% and to extract is 10%

        // start downloading
        File downloadFile = new File(installTmpFolder, buildFileNameFromUrl("xcode.download", url));
        downloadFile(url, downloadFile, progressPrefix + ": downloading",
                progressStart, progressLength * 0.9f);

        // unzipping to temp location
        File unzipFolder = new File(installTmpFolder, "xcode.unzip");
        if (!unzipFolder.mkdirs())
            throw new IOException("Unable to create tmp folder for xcode");
        unpack(downloadFile, unzipFolder, progressPrefix + ": unpacking", progressStart + progressLength * 0.9f,
                progressLength * 0.1f);

        // installing
        File installLocation = new File(System.getProperty("user.home") + "/.robovm/platform/Xcode.app");
        File deviceSupport = new File(installLocation, "/Developer/Platforms/iPhoneOS.platform/DeviceSupport");
        File deviceSupportBackup = null;
        if (installLocation.exists()) {
            // backup DeveloperDiskImages if any
            if (deviceSupport.exists()) {
                deviceSupportBackup = new File(installTmpFolder, "DeviceSupport");
                FileUtils.moveDirectory(deviceSupport, deviceSupportBackup);
            }

            // remove old
            FileUtils.deleteDirectory(installLocation);
        }

        // move unzipped location to dir
        File srcInstallFolder = new File(unzipFolder, "Xcode.app");
        FileUtils.moveDirectory(srcInstallFolder, installLocation);

        // restore device support if any
        if (deviceSupportBackup != null) {
            FileUtils.moveDirectory(deviceSupportBackup, deviceSupport);
        }

        // re-init xcode
        ExternalCommonToolchain.reInitXcode();
    }

    private void downloadFile(String  downloadUrl, File file, String progressText, float progressStart, float progressLength) throws IOException, InterruptedException {
        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setReadTimeout(25 * 1000);
        conn.setAllowUserInteraction(false);
        conn.setInstanceFollowRedirects(true);
        int respCode = conn.getResponseCode();
        if (respCode != HttpURLConnection.HTTP_OK)
            throw new IOException("Http resp code(" + respCode + ") != HTTP_OK");
        long downloadSize = conn.getContentLengthLong();
        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                OutputStream fos = new FileOutputStream(file)) {
            final byte data[] = new byte[8192];
            int count;
            long pos = 0;
            while((count = in.read(data)) > 0) {
                fos.write(data, 0, count);
                pos += count;

                if (downloadSize > 0)
                    deliverProgress(progressText, progressStart + pos * progressLength / downloadSize);

                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedException("Canceled");
            }
        }
    }

    private void unpack(File file, File unzipLocaiton, String progressText, float progressStart, float progressLength) throws IOException, InterruptedException {
        String achiever;
        if (file.getName().endsWith(".zip"))
            achiever = "zip";
        else if (file.getName().endsWith(".tar.gz"))
            achiever = "tar.gz";
        else
            achiever = guessAchiever(file);

        switch (achiever) {
            case "zip":
                unzip(file, unzipLocaiton, progressText, progressStart, progressLength);
                break;
            case "tar.gz":
                untargz(file, unzipLocaiton, progressText, progressStart, progressLength);
                break;
            default:
                throw new IOException("Unknown archive type: " + file);
        }
    }

    private void unzip(File file, File unzipLocaiton, String progressText, float progressStart, float progressLength) throws IOException, InterruptedException {
        try (ZipFile zf = new ZipFile(file)) {
            int idx = 0;
            Enumeration<? extends ZipEntry> it = zf.entries();
            while (it.hasMoreElements()) {
                ZipEntry e = it.nextElement();

                File destFile = new File(unzipLocaiton, e.getName());
                if (!e.isDirectory()) {
                    try (InputStream is = zf.getInputStream(e); OutputStream os = new FileOutputStream(destFile)) {
                        IOUtils.copy(is, os);
                    }
                } else {
                    if (!destFile.mkdirs())
                        throw new IOException("Failed to create " + destFile);
                }

                // deliver progress
                idx += 1;
                deliverProgress(progressText, progressStart + idx * progressLength / zf.size());

                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedException("Canceled");
            }
        }
    }


    private void untargz(File file, File unzipLocaiton, String progressText, float progressStart, float progressLength) throws IOException, InterruptedException {
        try (FileInputStream fis = new FileInputStream(file); TarArchiveInputStream in = new TarArchiveInputStream(new GZIPInputStream(fis))){
            // get FileChannel to know file read position to simulate progress
            FileChannel fc = fis.getChannel();
            TarArchiveEntry e;
            while ((e = (TarArchiveEntry) in.getNextEntry()) != null) {
                File destFile = new File(unzipLocaiton, e.getName());
                if (e.isDirectory()) {
                    File destFileFolder = new File(unzipLocaiton, e.getName());
                    if (!destFileFolder.mkdirs())
                        throw new IOException("Failed to create " + destFileFolder);
                } else {
                    try(OutputStream os = new FileOutputStream(destFile)) {
                        IOUtils.copy(in, os);
                    }
                }

                // warning: 0111 - is OCTAL notation
                //noinspection OctalInteger
                if ((e.getMode() & 0111) != 0) {
                    // could fail on Windows, so not checking for results
                    //noinspection ResultOfMethodCallIgnored
                    destFile.setExecutable(true);
                }

                // deliver progress
                deliverProgress(progressText, progressStart + fc.position() * progressLength / fc.size());

                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedException("Canceled");
            }
        }
    }


    /**
     * presaves app extention from url in case it was there
     */
    private String buildFileNameFromUrl(String fileName, String url) {
        String fileNameWithExt = fileName;
        if (url.endsWith(".zip"))
            fileNameWithExt += ".zip";
        else if (url.endsWith(".tar.gz"))
            fileNameWithExt += ".tar.gz";

        return fileNameWithExt;
    }

    /**
     * tries to identify archive type by reading two first bytes
     * @param file to archive
     * @return "zip", "tar.gz" or null
     */
    private String guessAchiever(File file) throws IOException {
        String result = null;
        try (FileInputStream is = new FileInputStream(file)) {
            byte[] magic = new byte[2];
            if (is.read(magic) != 2)
                throw new IOException("File to short: " + file);
            if (magic[0] == 0x50 && magic[1] == 0x4b)
                result = "zip";
            else if (magic[0] == 0x1f && magic[1] == (byte)0x8b)
                result = "tar.gz";
        }

        return result;
    }

    /**
     * single point of delivering progress callbacks
     * @param progressText to display
     * @param progress fraction progress value
     */
    private void deliverProgress(String progressText, float progress) {
        listener.installerOnProgress(progressText, progress);
    }
}
