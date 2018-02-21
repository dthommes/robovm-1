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
package org.robovm.idea.utils;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import org.robovm.compiler.util.update.UpdateCheckPlugin;
import org.robovm.compiler.util.update.UpdateChecker;
import org.robovm.idea.actions.UpdateDialog;

public class RoboVmUpdateCheckPlugin implements UpdateCheckPlugin {
    private final NotificationGroup NOTIFICATIONS = new NotificationGroup("RoboVM updates",
            NotificationDisplayType.STICKY_BALLOON, true);

    @Override
    public boolean updateAvailable(UpdateChecker.Update update) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notify = NOTIFICATIONS.createNotification("RoboVM", "RoboVM is ready to <a href=\"update\">update</a>",
                    NotificationType.INFORMATION, (notification, hyperlinkEvent) -> {
                        notification.expire();

                        UpdateDialog dialog = new UpdateDialog(update);
                        dialog.show();
                    });
            notify.notify(null);
        });

        // tell update checked that it was handled and information should not be shared
        return true;
    }
}
