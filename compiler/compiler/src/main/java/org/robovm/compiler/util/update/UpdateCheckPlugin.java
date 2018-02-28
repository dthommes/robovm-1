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
package org.robovm.compiler.util.update;

/**
 * Interface that allows to handle update checks that are initiated by compiler and handle them at higher UI level
 * E.g. to display it as corresponding dialog box. Plugin has to be registered as corresponding service in
 * META-INF/services
 */
public interface UpdateCheckPlugin {
    /**
     * Called by UpdateCheck if there is an update available to process it in UI
     * @param updateBundle -- vo with update information
     * @return true if update information has to be suppressed (as already handled in UI or due other reasons)
     */
    boolean updateAvailable(UpdateChecker.UpdateBundle updateBundle);
}
