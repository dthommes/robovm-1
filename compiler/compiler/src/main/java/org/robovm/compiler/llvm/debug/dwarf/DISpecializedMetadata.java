/*
 * Copyright (C) 2014 RoboVM AB
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */
package org.robovm.compiler.llvm.debug.dwarf;

import org.robovm.compiler.llvm.Metadata;
import org.robovm.compiler.llvm.NamedMetadata;
import org.robovm.compiler.llvm.StringConstant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Specialized metadata nodes are custom data structures in metadata (as opposed to generic tuples).
 * Their fields are labelled, and can be specified in any order.
 * @version $Id$
 */
class DISpecializedMetadata extends Metadata {
    private final boolean distinct;
    private final String nodeName;
    private final Map<String, String> values = new LinkedHashMap<>();

    DISpecializedMetadata(String nodeName) {
        this.nodeName = nodeName;
        this.distinct = false;
    }

    DISpecializedMetadata(String nodeName, boolean distinct) {
        this.nodeName = nodeName;
        this.distinct = distinct;
    }

    DISpecializedMetadata put(String key, DwarfConst.DwarfConstEnum v) {
        values.put(key, v.toString());
        return this;
    }

    DISpecializedMetadata put(String key, String v) {
        values.put(key, "\"" + StringConstant.escape(v) + "\"");
        return this;
    }

    DISpecializedMetadata put(String key, int v) {
        values.put(key, String.valueOf(v));
        return this;
    }

    DISpecializedMetadata put(String key, boolean v) {
        values.put(key, String.valueOf(v));
        return this;
    }

    DISpecializedMetadata put(String key, NamedMetadata v) {
        values.put(key, v.ref().toString());
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (distinct) {
            sb.append("distinct ");
        }

        sb.append('!');
        sb.append(nodeName);
        sb.append('(');

        int i = 0;
        for (Map.Entry<String, String> e : values.entrySet()) {
            if (i++ > 0) {
                sb.append(", ");
            }

            sb.append(e.getKey());
            sb.append(": ");
            if (e.getValue() == null) {
                sb.append("null");
            } else {
                sb.append(e.getValue());
            }
        }
        sb.append(')');
        return sb.toString();
    }
}
