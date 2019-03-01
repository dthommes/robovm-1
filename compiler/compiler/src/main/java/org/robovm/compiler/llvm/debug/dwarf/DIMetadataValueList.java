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

import org.robovm.compiler.llvm.IntegerConstant;
import org.robovm.compiler.llvm.Metadata;
import org.robovm.compiler.llvm.MetadataString;
import org.robovm.compiler.llvm.MetadataValue;
import org.robovm.compiler.llvm.NamedMetadata;
import org.robovm.compiler.llvm.Value;

import java.util.ArrayList;
import java.util.List;

/** Mutable value list */
public class DIMetadataValueList extends Metadata {
    private final List<MetadataValue> values = new ArrayList<>();

    public DIMetadataValueList() {
        super();
    }

    public DIMetadataValueList add(Value v) {
        values.add(new MetadataValue(v));
        return this;
    }

    public DIMetadataValueList add(int v) {
        values.add(new MetadataValue(new IntegerConstant(v)));
        return this;
    }

    public DIMetadataValueList add(String v) {
        values.add(new MetadataValue(new MetadataString(v)));
        return this;
    }

    public DIMetadataValueList add(Object[] objects) {
        for (Object v : objects) {
            if (v instanceof NamedMetadata)
                add(((NamedMetadata) v).ref());
            else if (v instanceof Value)
                add((Value)v);
            else if (v instanceof String)
                add((String)v);
            else if (v instanceof Integer)
                add((int)v);
            else if (v instanceof DwarfConst.DwarfConstEnum)
                add(v.toString());
            else
                throw new IllegalArgumentException(v.getClass().getSimpleName() + " is not supported!");
        }

        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("!{");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (values.get(i) == null) {
                sb.append("null");
            } else {
                sb.append(values.get(i));
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
