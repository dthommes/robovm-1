package org.robovm.compiler.llvm;


/**
 * This is pure virtual entity, it is not being out to IR.
 * It was introduces to workaround "Alias must point to a definition" when alias is created to
 * external function which is not anymore allowed (check https://reviews.llvm.org/rL283063)
 *
 * this alias is registered in ModuleBuilder for specific name, and reference to it be fetched from MB by name
 * then reference can be used as generic alias reference. but it will substitute constant value this alias wrap
 * in place of reference. It works similar to macro in c/cpp
 *
 * @version $Id$
 */
public class VirtualAlias {
    private final String name;
    private final Constant value;

    public VirtualAlias(String name, Constant value) {

        this.name = name;
        this.value = value;
    }

    public VirtualAliasRef ref() {
        return new VirtualAliasRef(this);
    }
    
    public String getName() {
        return name;
    }
    
    Constant getValue() {
        return value;
    }

    Type getType() {
        return value.getType();
    }

    @Override
    public String toString() {
        return "virtual alias " + name + " = " + value.toString();
    }
}
