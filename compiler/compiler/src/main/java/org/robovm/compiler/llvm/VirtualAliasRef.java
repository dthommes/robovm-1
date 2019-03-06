package org.robovm.compiler.llvm;

import java.util.Objects;

/**
 *
 * @version $Id$
 */
public class VirtualAliasRef extends Value {
    private final VirtualAlias alias;

    VirtualAliasRef(VirtualAlias alias) {
        this.alias = alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualAliasRef that = (VirtualAliasRef) o;
        return Objects.equals(alias, that.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias);
    }

    @Override
    public String toString() {
        return alias.getValue().toString();
    }

    @Override
    public Type getType() {
        return alias.getType();
    }
}
