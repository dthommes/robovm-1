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
import org.robovm.compiler.llvm.*;

public class DISubprogram extends NamedMetadata<DISpecializedMetadata> {
    private static final int DIFlagPrototyped = 1 << 8; // as in DebugInfo.h

    public DISubprogram(ModuleBuilder mb, String name, DIFile file, int line, DISubroutineType type, DICompileUnit cu,
                        NamedMetadata variables) {
        super(mb, new DISpecializedMetadata("DISubprogram", true)
                .put("name", name)
                .put("file", file)
                .put("line", line)
                .put("type", type)
                .put("isLocal", false)
                .put("isDefinition", true)
                .put("scopeLine", line)
                .put("flags", DIFlagPrototyped)
                .put("isOptimized", false)
                .put("unit", cu)
//                .put("variables", variables) // FIXME: it is strange it complains on variables
        );
    }

    public DebugMetadata toDebugMetadata() {
        return new DebugMetadata(this);
    }
}
