/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.robovm.idea.idevlog;

import com.google.common.collect.Sets;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.robovm.libimobiledevice.sweets.AppleDevice;

import javax.swing.*;
import java.util.List;
import java.util.Set;

public class DeviceRenderer {

    // Prevent instantiation
    private DeviceRenderer() {
    }


    public static void renderDeviceName(@NotNull AppleDevice d,
                                        @NotNull ColoredTextContainer component,
                                        boolean showSerialNumber) {
        String name = getDeviceName(d);

        component.append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        AppleDevice.State deviceState = d.getState();
        if (deviceState != AppleDevice.State.ONLINE) {
            String state = String.format("%1$s [%2$s] ", d.getSerialNumber(), d.getState());
            component.append(state, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
        } else if (showSerialNumber) {
            String state = String.format("%1$s ", d.getSerialNumber());
            component.append(state, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
        }

        if (deviceState != AppleDevice.State.DISCONNECTED) {
            component.append(" " + d.getProductVersion(), SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
    }

    @NotNull
    private static String getDeviceName(@NotNull AppleDevice d) {
        return d.getName();
    }

    public static boolean shouldShowSerialNumbers(@NotNull List<AppleDevice> devices) {
        Set<String> myNames = Sets.newHashSet();
        for (AppleDevice currentDevice : devices) {

            String currentName = getDeviceName(currentDevice);
            if (myNames.contains(currentName)) {
                return true;
            }
            myNames.add(currentName);
        }
        return false;
    }

    public static class DeviceComboBoxRenderer extends ColoredListCellRenderer<AppleDevice> {

        @NotNull
        private String myEmptyText;
        private boolean myShowSerial;

        public DeviceComboBoxRenderer(@NotNull String emptyText, boolean showSerial) {
            myEmptyText = emptyText;
            myShowSerial = showSerial;
        }

        public void setShowSerial(boolean showSerial) {
            myShowSerial = showSerial;
        }

        @Override
        protected void customizeCellRenderer(@NotNull JList list, AppleDevice value, int index, boolean selected, boolean hasFocus) {
            if (value != null)
                renderDeviceName((AppleDevice) value, this, myShowSerial);
            else
                append(myEmptyText, SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }
}

