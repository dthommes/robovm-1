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
package org.robovm.compiler.llvm;

import org.robovm.compiler.ModuleBuilder;
import org.robovm.compiler.llvm.debug.dwarf.DIMetadataValueList;

import java.util.Objects;

/**
 *
 * @version $Id$
 */
public class NamedMetadata<T extends Metadata> extends Metadata{
    private final String name;
    protected T node;

    public NamedMetadata(ModuleBuilder mb, String name, T node) {
        this.name = mb.registerNamedMetadata(this, name);
        this.node = node;
    }

    /**
     * auto assign index name
     */
    public NamedMetadata(ModuleBuilder mb, T node) {
        this.name = mb.registerNamedMetadata(this);
        this.node = node;
    }

    public String getName() {
        return name;
    }

    public Ref ref() {
        return new Ref(this);
    }

    private NamedMetadata<T> setNode(T node) {
        this.node = node;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('!');
        sb.append(name);
        sb.append(" = ");
        sb.append(node);
        return sb.toString();
    }

    public static NamedMetadata<DIMetadataValueList> withNamedTuple(ModuleBuilder mb, String name, Object ...values) {
        // create metadata with empty node to allow it having lower index that its children
        NamedMetadata<DIMetadataValueList> metadata;
        if (name == null)
            metadata =  new NamedMetadata<>(mb, null);
        else
            metadata =  new NamedMetadata<>(mb, name, null);

        DIMetadataValueList node = new DIMetadataValueList();
        node.add(values);
        return metadata.setNode(node);
    }

    public static NamedMetadata withTuple(ModuleBuilder mb, Object ...values) {
        return withNamedTuple(mb, null, values);
    }


    public static class Ref extends Metadata {
        private final String name;

        public Ref(NamedMetadata metadata) {
            this.name = metadata.getName();
        }

        public Ref(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Ref other = (Ref) obj;
            return name.equals(other.name);
        }

        @Override
        public String toString() {
            return "!" + name;
        }
    }
}
