package org.robovm.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindowManager;
import org.robovm.idea.RoboVmPlugin;

public class OpenDeviceSystemLogAction extends AnAction  {
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        if (anActionEvent.getProject() != null)
            ToolWindowManager.getInstance(anActionEvent.getProject()).getToolWindow(RoboVmPlugin.ROBOVM_TOOLWINDOW_DEVICE_LOG_ID).activate(null);
    }
}
