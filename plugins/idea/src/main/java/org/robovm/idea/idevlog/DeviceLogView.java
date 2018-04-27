/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.robovm.idea.idevlog;

import com.intellij.diagnostic.logging.DefaultLogFormatter;
import com.intellij.diagnostic.logging.LogConsoleBase;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.robovm.libimobiledevice.sweets.AppleDevice;
import org.robovm.libimobiledevice.sweets.AppleDeviceCenter;

import javax.swing.*;
import java.awt.*;

import static javax.swing.BoxLayout.X_AXIS;

/**
 * A UI panel which wraps a console that prints output from Android's logging system.
 */
public class DeviceLogView implements Disposable {

    private final Project myProject;
    private final DeviceContext myDeviceContext;
    private final String myToolWindowId;
    private JBLoadingPanel loadingPanel;

    private volatile AppleDevice myDevice;
    private final DeviceLogConsole myLogConsole;
    private final AppleDeviceCenter.DeviceLogListener mySyslogReceiver;

    /**
     * Called internally when the device may have changed, or been significantly altered.
     */
    private void notifyDeviceUpdated() {
        UIUtil.invokeAndWaitIfNeeded((Runnable) () -> {
            if (myProject.isDisposed()) {
                return;
            }
            updateLogConsole();
        });
    }

    @NotNull
    public final Project getProject() {
        return myProject;
    }


    /**
     * Logcat view with device obtained from {@link DeviceContext}
     */
    public DeviceLogView(@NotNull final Project project,
                         @NotNull DeviceContext deviceContext,
                         @NotNull String toolWindowId) {
        myDeviceContext = deviceContext;
        myProject = project;
        myToolWindowId = toolWindowId;

        Disposer.register(myProject, this);

        myLogConsole = new DeviceLogConsole(project);
        mySyslogReceiver = new AppleDeviceCenter.DeviceLogListener() {
            @Override
            public void deviceLogReceived(AppleDevice d, String line) {
                if (myDevice == d)
                    myLogConsole.addLogLine(line);
            }

            @Override
            public void deviceLogStatusUpdate(AppleDevice d, String line) {
                if (myDevice == d) {
                    if (line != null) {
                        loadingPanel.setLoadingText(line);
                        loadingPanel.startLoading();
                    } else {
                        loadingPanel.stopLoading();
                    }
                }
            }

            @Override
            public void deviceLogFailedWithError(AppleDevice d, Throwable t) {
                if (myDevice == d)
                    Messages.showErrorDialog(t.getMessage(), "RoboVM Device Connection Error");

            }
        };

    }

    public void setup(@NotNull final ToolWindow toolWindow) {

        JPanel myPanel = new JPanel(new BorderLayout());
        loadingPanel = new JBLoadingPanel(new BorderLayout(), myProject);
        loadingPanel.add(myPanel, BorderLayout.CENTER);

        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(loadingPanel, "", false);
        content.setDisposer(this);
        content.setCloseable(false);
        content.setPreferredFocusableComponent(loadingPanel);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content, true);

        JPanel searchComponent = createSearchComponent();
        DevicePanel devicePanel = new DevicePanel(myProject, myDeviceContext);
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(devicePanel.getComponent(), BorderLayout.WEST);

        searchComponent.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        northPanel.add(searchComponent, BorderLayout.CENTER);
        myPanel.add(northPanel, BorderLayout.NORTH);

        ApplicationManager.getApplication().invokeLater(this::activate, myProject.getDisposed());

        JComponent consoleComponent = myLogConsole.getComponent();
        ConsoleView console = myLogConsole.getConsole();
        if (console != null) {
            ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("iOS Device System Log",
                    myLogConsole.getOrCreateActions(), false);
            toolbar.setTargetComponent(console.getComponent());
            JComponent tbComp1 = toolbar.getComponent();
            myPanel.add(tbComp1, BorderLayout.WEST);
        }

        myPanel.add(consoleComponent, BorderLayout.CENTER);

        Disposer.register(this, myLogConsole);
        myDeviceContext.addListener(device -> notifyDeviceUpdated(), this);
        updateLogConsole();
    }

    @NotNull
    private JPanel createSearchComponent() {
        final JPanel searchComponent = new JPanel();
        searchComponent.setLayout(new BoxLayout(searchComponent, X_AXIS));
        searchComponent.add(myLogConsole.getTextFilterComponent());

        return searchComponent;
    }

    private boolean isActive() {
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(myToolWindowId);
        return window.isVisible();
    }

    private void activate() {
        if (isActive()) {
            updateLogConsole();
        }
        if (myLogConsole != null) {
            myLogConsole.activate();
        }
    }

    private void updateLogConsole() {
        AppleDevice device = getSelectedDevice();
        if (myDevice != device) {
            loadingPanel.stopLoading();
            if (myDevice != null) {
                AppleDeviceCenter.removeDeviceLogListener(myDevice, mySyslogReceiver);
            }
            // We check for null, because myLogConsole.clear() depends on myLogConsole.getConsole() not being null
            if (myLogConsole.getConsole() != null) {
                myLogConsole.clear();
            }

            myDevice = device;
            AppleDeviceCenter.addDeviceLogListener(myDevice, mySyslogReceiver);
        }
    }

    @Nullable
    private AppleDevice getSelectedDevice() {
        if (myDeviceContext != null) {
            return myDeviceContext.getSelectedDevice();
        } else {
            return null;
        }
    }

    @Override
    public final void dispose() {
        if (myDevice != null) {
            AppleDeviceCenter.removeDeviceLogListener(myDevice, mySyslogReceiver);
        }
    }


    final class DeviceLogConsole extends LogConsoleBase {
        private final RegexFilterComponent myRegexFilterComponent = new RegexFilterComponent("LOG_FILTER_HISTORY", 5);

        DeviceLogConsole(Project project) {
            super(project, null, "", false, new DeviceLogFilterModel(),
                    GlobalSearchScope.allScope(project), new DefaultLogFormatter());
            ConsoleView console = getConsole();
            if (console instanceof ConsoleViewImpl) {
                ConsoleViewImpl c = ((ConsoleViewImpl) console);
                c.addCustomConsoleAction(new Separator());
                c.addCustomConsoleAction(new DeviceCaptureScreenshotAction(myDeviceContext));
                c.addCustomConsoleAction(new Separator());
            }
            myRegexFilterComponent.addRegexListener(filter -> {
                ((DeviceLogFilterModel)getFilterModel()).updateCustomPattern(filter.getPattern());
            });
        }

        @Override
        public boolean isActive() {
            return DeviceLogView.this.isActive();
        }

        @NotNull
        @Override
        protected Component getTextFilterComponent() {
            return myRegexFilterComponent;
        }

        private void addLogLine(@NotNull String line) {
            super.addMessage(line);
        }
    }
}
