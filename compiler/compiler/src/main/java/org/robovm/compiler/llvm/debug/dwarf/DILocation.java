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
package org.robovm.compiler.llvm.debug.dwarf;

import org.robovm.compiler.ModuleBuilder;
import org.robovm.compiler.llvm.DebugMetadata;
import org.robovm.compiler.llvm.NamedMetadata;

public class DILocation extends NamedMetadata<DISpecializedMetadata> {
    public DILocation(ModuleBuilder mb, int lineNo, int colNo, DISubprogram scope) {
        super(mb, new DISpecializedMetadata("DILocation")
                .put("line", lineNo)
                .put("column", colNo)
                .put("scope", scope));
    }

    public DebugMetadata toDebugMetadata() {
        return new DebugMetadata(this);
    }
}
