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

import com.intellij.notification.NotificationType;
import org.robovm.compiler.util.update.UpdateCheckPlugin;
import org.robovm.compiler.util.update.UpdateChecker;
import org.robovm.idea.RoboVmPlugin;
import org.robovm.idea.actions.UpdateDialog;

public class RoboVmUpdateCheckPlugin implements UpdateCheckPlugin {

    @Override
    public boolean updateAvailable(UpdateChecker.UpdateBundle updateBundle) {
        RoboVmPlugin.displayBalloonNotification("RoboVM", "RoboVM is ready to <a href=\"update\">update</a>",
                    NotificationType.INFORMATION, (notification, hyperlinkEvent) -> {
                        notification.expire();

                        UpdateDialog dialog = new UpdateDialog(updateBundle);
                        dialog.show();
                    });

        // tell update checked that it was handled and information should not be shared
        return true;
    }
}
