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
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.robovm.libimobiledevice.sweets.AppleDevice;

import java.util.EventListener;

public class DeviceContext {
  private final EventDispatcher<DeviceSelectionListener> myEventDispatcher =
    EventDispatcher.create(DeviceSelectionListener.class);

  private AppleDevice mySelectedDevice;

  public void addListener(DeviceSelectionListener l, @NotNull Disposable parentDisposable) {
    myEventDispatcher.addListener(l, parentDisposable);
  }

  public void fireDeviceSelected(@Nullable AppleDevice d) {
    mySelectedDevice = d;
    myEventDispatcher.getMulticaster().deviceSelected(d);
  }

  public AppleDevice getSelectedDevice() {
    return mySelectedDevice;
  }

  public interface DeviceSelectionListener extends EventListener {
    /** Callback invoked when the selected device changes in the current device context. */
    void deviceSelected(@Nullable AppleDevice device);
  }
}
