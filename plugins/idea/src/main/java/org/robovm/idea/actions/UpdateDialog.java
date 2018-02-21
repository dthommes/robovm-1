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

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.robovm.compiler.util.update.UpdateChecker;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class UpdateDialog extends DialogWrapper {
    private static final String LINK_PLUGIN_UPDATE = "updatePlugin";

    private JPanel panel;
    private JEditorPane message;

    private String updateUrl;

    public UpdateDialog(UpdateChecker.Update update) {
        super(false);
        init();


        String text = "<html><head>" +
                UIUtil.getCssFontDeclaration(UIUtil.getLabelFont(), null, null, null) +
                "</head><body>";
        if (update != null) {
            setTitle("RoboVM Updates Are Available");
            text += update.getUpdateText();

            updateUrl = update.getUpdateUrlForKey("idea");
            if (updateUrl != null) {
                text += " <a href=" + LINK_PLUGIN_UPDATE + ">Open page</a></b>";
            }
            text += "<br>";

            if (update.getUpdateWhatsNew() != null) {
                text += "<b>What's new</b><br>" + update.getUpdateWhatsNew().replace("\n", "<br>");
            }
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
                    Desktop.getDesktop().browse(URI.create(updateUrl));
                } catch (IOException ignored) {
                }

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
}
