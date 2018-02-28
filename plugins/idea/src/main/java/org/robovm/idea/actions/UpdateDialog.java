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
package org.robovm.idea.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.robovm.compiler.util.platforms.external.ExternalCommonToolchainInstaller;
import org.robovm.compiler.util.update.UpdateChecker;
import org.robovm.idea.RoboVmPlugin;
import org.robovm.idea.components.setupwizard.NoToolchainSetupDialog;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateDialog extends DialogWrapper {
    private static final String LINK_PLUGIN_UPDATE = "updatePlugin";
    private static final String LINK_TOOLCHAIN_INSTALL = "upgradeToolchain";

    private JPanel panel;
    private JEditorPane message;

    private final UpdateChecker.UpdateBundle updateBundle;

    public UpdateDialog(UpdateChecker.UpdateBundle u) {
        super(false);
        init();
        this.updateBundle = u;


        String text = "<html><head>" +
                UIUtil.getCssFontDeclaration(UIUtil.getLabelFont(), null, null, null) +
                "</head><body>";
        if (updateBundle != null) {
            setTitle("RoboVM Updates Are Available");

            String updateText = "";
            if (updateBundle.getAppUpdate() != null) {
                UpdateChecker.Update appUpdate = updateBundle.getAppUpdate();
                updateText += appUpdate.getUpdateText();

                String updateUrl = appUpdate.getUpdateUrlForKey("idea");
                if (updateUrl != null) {
                    updateText += " <a href=" + LINK_PLUGIN_UPDATE + "><b>Open page</b></a>";
                }
                updateText += "<br>";

                if (appUpdate.getUpdateWhatsNew() != null) {
                    updateText += "<b>What's new</b><br>" + appUpdate.getUpdateWhatsNew().replace("\n", "<br>") + "<br>";
                }
            }

            if (updateBundle.getToolchainUpdate() != null || updateBundle.getXcodeUpdate() != null) {
                if (!updateText.isEmpty())
                    updateText += "<br>";

                UpdateChecker.Update toolchainUpdate = updateBundle.getToolchainUpdate();
                UpdateChecker.Update xcodeUpdate = updateBundle.getXcodeUpdate();

                updateText += "Platform files updates available. <a href=" + LINK_TOOLCHAIN_INSTALL + "><b>Install</b></a>";
                updateText += "<br>";

                if (toolchainUpdate != null) {
                    updateText += "<b>Toolchain:</b> " + toolchainUpdate.getUpdateText() + "<br>";
                    if (toolchainUpdate.getUpdateWhatsNew() != null)
                        updateText += toolchainUpdate.getUpdateWhatsNew().replace("\n", "<br>") + "<br>";
                }
                if (xcodeUpdate != null) {
                    updateText += "<b>Xcode:</b> " + xcodeUpdate.getUpdateText() + "<br>";
                    if (xcodeUpdate.getUpdateWhatsNew() != null)
                        updateText += xcodeUpdate.getUpdateWhatsNew().replace("\n", "<br>") + "<br>";
                }
            }

            text += updateText;
        } else {
            setTitle("RoboVM Updates");
            text += "You already have latest version of RoboVM installed";
        }
        text += "</html>";

        message.setBackground(UIUtil.getPanelBackground());
        message.setText(text);
        message.setCaretPosition(0);
        message.setEditable(false);
        message.setText(text);

        message.addHyperlinkListener(e -> {
            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
                return;

            if (e.getDescription().equals(LINK_PLUGIN_UPDATE)) {
                try {
                    // open link in external browser
                    assert updateBundle != null;
                    String updateUrl = updateBundle.getAppUpdate().getUpdateUrlForKey("idea");
                    Desktop.getDesktop().browse(URI.create(updateUrl));
                } catch (IOException ignored) {
                }

                close(0);
            } else if (e.getDescription().equals(LINK_TOOLCHAIN_INSTALL)) {
                if (!InstallTask.busy.get())
                    ProgressManager.getInstance().run(new InstallTask());

                close(0);
            }
        });
        setOKButtonText("Close");
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }

    /**
     * background task that performs component install
     */
    private static class InstallTask extends Task.Backgroundable {
        private static AtomicBoolean busy = new AtomicBoolean(false);

        InstallTask() {
            super(null, "RoboVM Components Installer");
        }

        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
            busy.set(true);
            ExternalCommonToolchainInstaller.ProgressListener listener;
            listener = new ExternalCommonToolchainInstaller.ProgressListener() {
                @Override
                public void installerOnProgress(String message, float fraction) {
                    progressIndicator.setText(message);
                    progressIndicator.setFraction(fraction);
                }

                @Override
                public void installerFinished() {
                    installerFailed(null);
                }

                @Override
                public void installerFailed(Throwable e) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        // if operation completed -- recheck for updates -- should be none
                        if (e == null) {
                            RoboVmPlugin.displayBalloonNotification("RoboVM", "Components were updated!",
                                    NotificationType.INFORMATION, null);
                        } else {

                            String message = e.getMessage();
                            if (message == null && e.getCause() != null)
                                message = e.getCause().getMessage();
                            if (message == null)
                                message = "Exception: " + e.getClass().getSimpleName();
                            RoboVmPlugin.displayBalloonNotification("RoboVM", "Installation failed", message,
                                    NotificationType.ERROR, null);
                        }
                    });
                }
            };

            ExternalCommonToolchainInstaller installer = new ExternalCommonToolchainInstaller(listener);
            try {
                installer.install();
            } catch (Throwable e) {
                listener.installerFailed(e);
            }

            // result should be delivered through callback
        }

        @Override
        public void onThrowable(@NotNull Throwable error) {
        }

        @Override
        public void onFinished() {
            super.onFinished();

            busy.set(false);
        }
    }

}
