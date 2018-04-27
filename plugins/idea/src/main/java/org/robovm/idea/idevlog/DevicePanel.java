/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.AccessibleContextUtil;
import org.jetbrains.annotations.NotNull;
import org.robovm.libimobiledevice.sweets.AppleDevice;
import org.robovm.libimobiledevice.sweets.AppleDeviceCenter;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class DevicePanel implements Disposable, AppleDeviceCenter.DeviceChangeListener {
    private static final Logger LOG = Logger.getInstance(DevicePanel.class);

    private JPanel myPanel;

    private final DeviceContext myDeviceContext;

    public boolean myIgnoreActionEvents;

    private JComboBox<AppleDevice> myDeviceCombo;
    private DeviceRenderer.DeviceComboBoxRenderer myDeviceRenderer;

    public DevicePanel(@NotNull Project project, @NotNull DeviceContext context) {
        myDeviceContext = context;
        Disposer.register(project, this);

        AppleDeviceCenter.addDeviceChangeListener(this);
        initializeDeviceCombo();
        updateDeviceCombo();
    }

    private void initializeDeviceCombo() {
        AccessibleContextUtil.setName(myDeviceCombo, "Devices");
        myDeviceCombo.addActionListener(actionEvent -> {
            if (myIgnoreActionEvents)
                return;

            Object sel = myDeviceCombo.getSelectedItem();
            AppleDevice device = (sel instanceof AppleDevice) ? (AppleDevice)sel : null;
            myDeviceContext.fireDeviceSelected(device);
        });

        myDeviceRenderer = new DeviceRenderer.DeviceComboBoxRenderer("No Connected Devices", false);
        myDeviceCombo.setRenderer(myDeviceRenderer);
        Dimension size = myDeviceCombo.getMinimumSize();
        myDeviceCombo.setMinimumSize(new Dimension(200, size.height));
    }


    public void selectDevice(AppleDevice device) {
        myDeviceCombo.setSelectedItem(device);
    }

    @Override
    public void dispose() {
        AppleDeviceCenter.removeDeviceChangeListener(this);
    }

    public JPanel getComponent() {
        return myPanel;
    }


    @Override
    public void deviceAdded(AppleDevice d) {
        LOG.info("Device added: " + d);
        UIUtil.invokeLaterIfNeeded(this::updateDeviceCombo);
    }

    @Override
    public void deviceRemoved(AppleDevice d) {
        LOG.info("Device disconnected: " + d);
        UIUtil.invokeLaterIfNeeded(this::updateDeviceCombo);
    }

    private void updateDeviceCombo() {
        myIgnoreActionEvents = true;

        boolean update = true;
        AppleDevice selected = (AppleDevice) myDeviceCombo.getSelectedItem();
        myDeviceCombo.removeAllItems();
        boolean shouldAddSelected = true;

        AppleDevice[] devices = AppleDeviceCenter.getConnectedDevices();

        // determine if there are duplicate devices, if so, show serial number
        myDeviceRenderer.setShowSerial(DeviceRenderer.shouldShowSerialNumbers(Arrays.asList(devices)));

        for (AppleDevice device : devices) {
            myDeviceCombo.addItem(device);
            // If we reattach an actual device into Studio, "device" will be the connected IDevice and "selected" will be the disconnected one.
            boolean isSelectedReattached = selected != null && selected.getSerialNumber().equals(device.getSerialNumber());
            if (selected == device || isSelectedReattached) {
                myDeviceCombo.setSelectedItem(device);
                shouldAddSelected = false;
                update = selected != device;
            }
        }

        if (selected != null && shouldAddSelected) {
            myDeviceCombo.addItem(selected);
            myDeviceCombo.setSelectedItem(selected);
        }

        if (update) {
            myDeviceContext.fireDeviceSelected((AppleDevice) myDeviceCombo.getSelectedItem());
        }

        myIgnoreActionEvents = false;
    }
}
