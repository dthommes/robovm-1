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
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;
import org.robovm.compiler.util.update.UpdateChecker;

import java.util.concurrent.atomic.AtomicBoolean;

public class CheckForUpdateAction extends AnAction {
    private static AtomicBoolean busy = new AtomicBoolean(false);

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (!busy.get())
            ProgressManager.getInstance().run(new CheckForUpdateTask());
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(!busy.get());
    }

    /**
     * background task that check for udpate
     */
    private static class CheckForUpdateTask extends Task.Backgroundable {
        Throwable exceptionIfHappened;

        CheckForUpdateTask() {
            super(null, "RoboVM Checking for Update");
        }

        @Override
        public void run(@NotNull ProgressIndicator progress) {
            busy.set(true);
            progress.setText("Downloading....");
            progress.setIndeterminate(true);

            UpdateChecker.Update update = UpdateChecker.fetchUpdateSilent();

            progress.stop();
            ApplicationManager.getApplication().invokeLater(() -> {
                UpdateDialog dialog = new UpdateDialog(update);
                dialog.show();
            });
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
                Notifications.Bus.notify(new Notification( "RoboVM", "RoboVM check for update",
                        "Failed due error: " + exceptionIfHappened.getMessage(), NotificationType.ERROR));
            }

            busy.set(false);
        }
    }

}
