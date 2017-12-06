package org.robovm.idea.components.setupwizard;

import com.intellij.ide.util.PropertiesComponent;
import org.robovm.compiler.util.platforms.ToolchainUtil;
import org.robovm.compiler.util.platforms.external.ExternalCommonToolchain;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class NoXcodeSetupDialog extends JDialog {
    private static final String ROBOVM_SKIP_TOOLCHAIN_SETUP_WIZARD = "robovm.skipToolChainSetupWizard";

    private JPanel panel;
    private JLabel infoText;
    private JButton nextButton;
    private JButton howToInstallButton;
    private JLabel errorLabel;
    private JCheckBox doNotShowAgain;
    private Thread updateThread;

    public NoXcodeSetupDialog() {
        setContentPane(panel);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("RoboVM Setup");
        if (ExternalCommonToolchain.isSupported()) {
            // this platform is supported, and packages can be downloaded
            infoText.setText(getInfoText(true, true));

            // tell user that next button actually acts as skip
            nextButton.setText("skip");

            // update what is missing and start the process
            validateInput();
            updateThread = new Thread(new Runnable() {
                boolean done = false;
                @Override
                public void run() {
                    while(!done && !Thread.interrupted()) {
                        SwingUtilities.invokeLater(() -> done = validateInput());
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                            done = true;
                        }
                    }
                }
            });
            updateThread.start();

            //
            howToInstallButton.addActionListener(e -> {
                try {
                    Desktop.getDesktop().browse(new URI(ExternalCommonToolchain.getHelpUrl()));
                } catch (Throwable ignored) {
                }
            });
        } else {
            infoText.setText(getInfoText(false, false));
            howToInstallButton.setVisible(false);
            errorLabel.setVisible(false);
            doNotShowAgain.setVisible(false);
        }

        nextButton.addActionListener(e -> {
            if (updateThread != null) {
                try {
                    updateThread.join();
                } catch (InterruptedException ignored) {
                }
                updateThread = null;
            }

            // set don't show
            if (doNotShowAgain.isVisible())
                PropertiesComponent.getInstance().setValue(ROBOVM_SKIP_TOOLCHAIN_SETUP_WIZARD, doNotShowAgain.isSelected());

            dispose();
        });

        pack();
        setLocationRelativeTo(null);
    }

    private int lastValidateCode = -1;
    private boolean validateInput() {
        int code = ToolchainUtil.isXcodeInstalled() ? 0 : 1;
        code |= ToolchainUtil.isToolchainInstalled() ? 0 : 2;

        if (code != lastValidateCode) {
            // rebuild label
            lastValidateCode = code;
            if (lastValidateCode != 0) {
                String errorText = "<html>";
                if (!ToolchainUtil.isToolchainInstalled()) {
                    errorText += "No platform toolchain utils found";
                }
                if (!ToolchainUtil.isXcodeInstalled()) {
                    if (!ToolchainUtil.isToolchainInstalled())
                        errorText += "<br>";
                    errorText += "No XCode files found";
                }
                errorText += "</html>";
                errorLabel.setText(errorText);
            }

            errorLabel.setVisible(lastValidateCode != 0);
            if (lastValidateCode == 0) {
                // fully valid case
                nextButton.setText("Next");
                // no need to show download hint anymore
                infoText.setText(getInfoText(true, false));
            }
        }

        return lastValidateCode == 0;
    }

    private String getInfoText(boolean supported, boolean withDownload) {
        if (supported) {
            String info = "<html><strong>Experimental:</strong>" + "" +
                    "<br><br>You can compile for iOS device on Windows or Linux, but you have to install missing toolchain/xcode files." +
                    "<br><br>Toolset for Windows/Linux is not fully compliant with MacOSX ones and there are known issues with resource compiler as it is not fully supported yet." +
                    "<br><br>Please read Tutorial page by clicking \"How to install\" button to find out how to setup <strong>keychain and mobile provisioning</strong> locations.";
            if (withDownload) {
                info += "<br><br>To enable compilation please manually deploy toolchain for <strong>" +
                    ExternalCommonToolchain.getPlatformId() + "</strong> platform." +
                    "<br>This screen will be updated automatically.";
            }
            info += "</html>";
            return info;
        } else {
            return "<html>To compile for iOS simulators or devices, you will need a Mac running <strong>Mac OS X 10.x</strong> and <strong>Xcode 6 or higher</strong>. " +
                    "<br><br>You can also compile code on Windows or Linux with known limitations. On this platform, but you will not be able to compile it via RoboVM."+
                    "<br><br>You can still view and edit the code on Windows or Linux, but you will not be able to compile it via RoboVM.</html>";
        }
    }

    private void createUIComponents() {
    }


    public static boolean shallShowDialog() {
        // show this dialog second time if there is a problem with toolchain and it is not disabled
        return !PropertiesComponent.getInstance().getBoolean(ROBOVM_SKIP_TOOLCHAIN_SETUP_WIZARD, false) &&
                (!ToolchainUtil.isToolchainInstalled() || !ToolchainUtil.isXcodeInstalled());
    }
}
