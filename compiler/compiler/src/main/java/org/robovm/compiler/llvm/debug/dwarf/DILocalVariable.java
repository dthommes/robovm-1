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
import org.robovm.compiler.llvm.NamedMetadata;

public class DILocalVariable extends NamedMetadata<DISpecializedMetadata> {
    /**
     * Creates Argument type local variable
     * @param mb to attach metadata to root of
     * @param name of variable
     * @param defLineNo line it is defined
     * @param argNo argument index
     * @param file it was defined
     * @param scope it was defined in (usually DISubprogram)
     * @param type referent to type metadata of var
     */
    public DILocalVariable(ModuleBuilder mb, String name, int defLineNo, int argNo, DIFile file,
                           DISubprogram scope, NamedMetadata type) {
        super(mb, new DISpecializedMetadata("DILocalVariable")
                .put("name", name)
                .put("arg", argNo)
                .put("scope", scope)
                .put("file", file)
                .put("line", defLineNo)
                .put("type", type));
    }

    // !12 = !DILocalVariable(name: "c", arg: 1, scope: !8, file: !1, line: 2, type: !11)

    /**
     * Constructor for auto variable (without argument no parameter_
     */
    public DILocalVariable(ModuleBuilder mb, String name, int defLineNo, DIFile file,
                           DISubprogram scope, NamedMetadata type) {
        super(mb, new DISpecializedMetadata("DILocalVariable")
                .put("name", name)
                .put("scope", scope)
                .put("file", file)
                .put("line", defLineNo)
                .put("type", type));
    }
}
