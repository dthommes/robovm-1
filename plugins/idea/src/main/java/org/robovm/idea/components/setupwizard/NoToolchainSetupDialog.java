package org.robovm.idea.components.setupwizard;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import org.robovm.compiler.Version;
import org.robovm.compiler.util.platforms.external.ExternalCommonToolchain;
import org.robovm.compiler.util.platforms.external.ExternalCommonToolchainInstaller;
import org.robovm.compiler.util.update.UpdateChecker;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class NoToolchainSetupDialog extends JDialog {
    private JPanel panel;
    private JLabel infoText;
    private JLabel statusLabel;
    private JButton nextButton;
    private JButton actionButton;
    private JButton tutorialButton;
    private JProgressBar installProgress;

    private Thread taskThread;
    private UpdateChecker.UpdateBundle componentsUpdate;
    private Runnable onClickAction;

    public NoToolchainSetupDialog() {
        setContentPane(panel);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("RoboVM Setup");

        installProgress.setVisible(false);

        onClickAction = this::nothingToDo;
        actionButton.addActionListener(e -> onClickAction.run());
        tutorialButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(ExternalCommonToolchain.getHelpUrl()));
            } catch (Throwable ignored) {
            }
        });
        nextButton.addActionListener(e -> dispose());

        setResizable(false);
        pack();
        setLocationRelativeTo(null);


        // lets start checking for update
        checkForUpdate();
    }

    /**
     * just a placeholder for do nothing handler
     */
    private void nothingToDo(){
        throw new RuntimeException("This click should not happen !");
    }

    private void checkForUpdate() {
        // common UI setup
        installProgress.setVisible(true);
        installProgress.setIndeterminate(true);


        // just checking for update
        statusLabel.setText("Checking for updates...");
        actionButton.setText("Re-check");
        actionButton.setEnabled(false);
        onClickAction = this::nothingToDo;

        taskThread = new Thread(() -> {
            UpdateChecker.UpdateBundle update = null;
            Throwable failedDue = null;
            try {
                update = UpdateChecker.fetchUpdateSilent();
            } catch (Throwable e) {
                failedDue = e;
            }

            UpdateChecker.UpdateBundle finalUpdate = update;
            Throwable finalFailedDue = failedDue;
            ApplicationManager.getApplication().invokeLater(() -> processOperationResult("Check for update", finalUpdate, finalFailedDue));
        });

        nextButton.setEnabled(false);
        taskThread.start();
    }


    private void installComponents() {
        // common UI setup
        statusLabel.setText("");
        installProgress.setVisible(true);
        installProgress.setIndeterminate(false);

        // just checking for update
        actionButton.setText("Cancel");
        actionButton.setEnabled(true);
        onClickAction = this::cancelInstallComponents;

        taskThread = new Thread(() -> {
            InstallListener listener = new InstallListener();
            ExternalCommonToolchainInstaller installer = new ExternalCommonToolchainInstaller(listener);
            try {
                installer.install();
            } catch (Throwable e) {
                listener.installerFailed(e);
            }

            // result should be delivered through callback
        });

        nextButton.setEnabled(false);
        taskThread.start();
    }

    private void cancelInstallComponents() {
        // interrupt operation
        actionButton.setEnabled(false);
        onClickAction = this::nothingToDo;
        synchronized (this) {
            if (taskThread != null) {
                taskThread.interrupt();
            }
        }
    }

    private class InstallListener implements ExternalCommonToolchainInstaller.ProgressListener {
        @Override
        public void installerOnProgress(String message, float progress) {
            ApplicationManager.getApplication().invokeLater(() -> {
                statusLabel.setText(message);
                installProgress.setValue((int) (installProgress.getMaximum() * progress));
            });
        }

        @Override
        public void installerFinished() {
            // just to process in single place
            installerFailed(null);
        }

        @Override
        public void installerFailed(Throwable e) {
            ApplicationManager.getApplication().invokeLater(() -> {
                // if operation completed -- recheck for updates -- should be none
                if (e == null)
                    checkForUpdate();
                else
                    processOperationResult("Installation", componentsUpdate,  e);
            });
        }
    }

    /**
     * common code to process check for updates/install operation results
     * @param operationPrefix -- prefix of operation to be displayed in text
     * @param update -- stored or received update object to display current status
     * @param e -- exception object if any happened
     */
    private void processOperationResult(String operationPrefix, UpdateChecker.UpdateBundle update, Throwable e) {
        // drop thread reference
        synchronized (this) {
            taskThread = null;
        }

        String statusText = "<html>";
        if (e != null) {
            statusText += "<font color='red'>" + operationPrefix + " failed!</font><br>";
        }

        UpdateChecker.Update xcodeUpdate = update != null ? update.getXcodeUpdate() : null;
        Version xcodeVersion = ExternalCommonToolchain.getXcodeVersion();
        boolean xcodeInstalled =  xcodeVersion != null;
        // check for version as install operation can partially fail and some parts could be installed
        boolean xcodeUpdateAvailable = xcodeUpdate != null && (xcodeVersion == null ||
                xcodeVersion.getVersionCode() != xcodeUpdate.getVersion().getVersionCode());
        statusText += buildToolUpdateText("iOS SDK", xcodeInstalled, xcodeUpdateAvailable,
                update == null && e != null) + "<br>";

        UpdateChecker.Update toolchainUpdate = update != null ? update.getToolchainUpdate() : null;
        Version toolchainVersion = ExternalCommonToolchain.getToolchainVersion();
        boolean toolchainInstalled =  toolchainVersion != null;
        // check for version as install operation can partially fail and some parts could be installed
        boolean toolchainUpdateAvailable = toolchainUpdate != null && (toolchainVersion == null ||
                toolchainVersion.getVersionCode() != toolchainUpdate.getVersion().getVersionCode());
        statusText += buildToolUpdateText("Toolchain", toolchainInstalled, toolchainUpdateAvailable,
                update == null && e != null);
        statusText += "</html>";

        // hide progress
        installProgress.setVisible(false);

        // update status text
        statusLabel.setText(statusText);
        statusLabel.setVisible(true);

        componentsUpdate = update;
        if (xcodeUpdate != null ||toolchainUpdate != null) {
            // if (e == null) this happens during check of update
            // if (e != null) this happens during install
            // in both case next pending operation is install
            // switching to install mode
            actionButton.setText("Install");
            actionButton.setEnabled(true);
            onClickAction = this::installComponents;
        } else {
            // there is no update -- either up to date or check for update failed
            if (e != null) {
                // check for update failed
                actionButton.setText("Check");
                actionButton.setEnabled(true);
                onClickAction = this::checkForUpdate;
            } else {
                // all components are up to date, hide button
                onClickAction = this::nothingToDo;
                actionButton.setVisible(false);
            }
        }

        // show message box as there would be not enough space in status label to fit long error messages
        if (e != null) {
            String message = e.getMessage();
            if (message == null && e.getCause() != null)
                message = e.getCause().getMessage();
            if (message == null)
                message = "Exception: " + e.getClass().getSimpleName();
            Messages.showErrorDialog(this, message, operationPrefix + " failed!");
        }

        // enable next button if toolchain and xcode versions were installed
        nextButton.setEnabled(xcodeVersion != null && toolchainVersion != null);
    }

    private String buildToolUpdateText(String title, boolean installed, boolean updateAvailable, boolean updateFailed) {
        String res = title +  ": ";
        String msg;
        if (installed) {
            if (updateFailed) {
                msg = "installed";
            } else {
                msg = updateAvailable ? "update available" : "up to date";
            }
        } else {
            if (updateAvailable)
                msg = "not installed";
            else
                msg = "install not available, please update RoboVM plugin";
        }

        if (!installed || updateAvailable) {
            res += "<font color='red'>"+ msg + "</font>";
        } else {
            res += msg;
        }

        return res;
    }
}
