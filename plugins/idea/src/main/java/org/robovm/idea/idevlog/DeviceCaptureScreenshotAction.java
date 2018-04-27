package org.robovm.idea.idevlog;

import com.google.common.io.Files;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;
import org.robovm.libimobiledevice.sweets.AppleDevice;
import org.robovm.libimobiledevice.sweets.AppleDeviceCenter;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

final class DeviceCaptureScreenshotAction extends AnAction {
    private static AtomicBoolean busy = new AtomicBoolean(false);
    private DeviceContext deviceContext;

    DeviceCaptureScreenshotAction(DeviceContext deviceContext) {
        super("Screenshot", "Capture screenshot from connected device", AllIcons.Actions.Preview);
        this.deviceContext = deviceContext;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(!busy.get() && deviceContext.getSelectedDevice() != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (!busy.get() && deviceContext.getSelectedDevice() != null) {
            ProgressManager.getInstance().run(new CaptureTask(deviceContext.getSelectedDevice()));
        }
    }


    /**
     * background task that captures screenshot
     */
    private class CaptureTask extends Task.Backgroundable {
        private final AppleDevice device;
        Throwable exceptionIfHappened;
        CaptureTask(AppleDevice selectedDevice) {
            super(null, "RoboVM Capturing Screenshot");
            device = selectedDevice;
        }

        @Override
        public void run(@NotNull ProgressIndicator progress) {
            busy.set(true);

            try {
                File outDir = new File(new File(System.getProperty("user.home")), "Desktop");
                if (!outDir.exists() || !outDir.isDirectory())
                    throw new Error("Can't locate user's Destop dir, failed at " + outDir);
                byte[] bytes = AppleDeviceCenter.captureScreenshot(device);
                String fileName = "Screenshot_" + System.currentTimeMillis() + ".tiff";
                File outFile = new File(outDir, fileName);
                Files.write(bytes, outFile);
                Notifications.Bus.notify(new Notification( "RoboVM", "RoboVM Device Screenshots",
                        "Sceenshot saved to desktop as " + fileName, NotificationType.INFORMATION));
            } catch (Exception e) {
                exceptionIfHappened = e;
            }
        }

        @Override
        public void onThrowable(@NotNull Throwable error) {
            exceptionIfHappened = error;
        }

        @Override
        public void onFinished() {
            super.onFinished();

            // single exit point
            if (exceptionIfHappened != null) {
                Notifications.Bus.notify(new Notification( "RoboVM", "RoboVM Device Screenshots",
                        "Failed due error: " + exceptionIfHappened.getMessage(), NotificationType.ERROR));
            }

            busy.set(false);
        }
    }

}
