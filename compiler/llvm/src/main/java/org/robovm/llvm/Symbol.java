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
package org.robovm.llvm;

import java.util.Comparator;

/**
 * 
 */
public class Symbol {
    private final String name;
    private final long address;
    private final long size;

    Symbol(String name, long address, long size) {
        this.name = name;
        this.address = address;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getAddress() {
        return address;
    }

    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return String.format("Symbol [name=%s, address=%s, size=%s]", name, address, size);
    }

    public static Comparator<Symbol> getAddressComparator() {
        return new Comparator<Symbol>() {
            @Override
            public int compare(Symbol o1, Symbol o2) {
                long diff = o1.getAddress() - o2.getAddress();
                if (diff != 0)
                    return diff < 0 ? -1 : 1;
                else
                    return 0;
            }
        };
    }
}
