/*
 * Copyright (C) 2015 RoboVM AB
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
package org.robovm.llvm;

import org.robovm.llvm.binding.LLVM;
import org.robovm.llvm.binding.Linkage;
import org.robovm.llvm.binding.ValueRef;

/**
 * 
 */
public class Function {
    private ValueRef ref;

    Function(ValueRef ref) {
        this.ref = ref;
    }

    protected ValueRef getRef() {
        return ref;
    }
    
    public String getName() {
        return LLVM.GetValueName(getRef());
    }
    
    public Linkage getLinkage() {
        return LLVM.GetLinkage(getRef());
    }

    public void setLinkage(Linkage linkage) {
        LLVM.SetLinkage(getRef(), linkage);
    }

    public void addAttribute(Context ctx, Attribute attribute) {
        attribute.addToFunction(ctx.getRef(), getRef());
    }

    public void removeAttribute(Context ctx, Attribute attribute) {
        attribute.removeFromFunction(ctx.getRef(), getRef());
    }
}
