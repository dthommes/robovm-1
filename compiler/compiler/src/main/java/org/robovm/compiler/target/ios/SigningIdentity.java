/*
 * Copyright (C) 2013 RoboVM AB
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
package org.robovm.compiler.target.ios;

import org.robovm.compiler.util.platforms.ToolchainUtil;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a signing identity.
 */
public class SigningIdentity<T> implements Comparable<SigningIdentity> {

    private final String name;
    private final String fingerprint;
    private final T bundle;
    
    public SigningIdentity(String name, String fingerprint) {
        this.name = name;
        this.fingerprint = fingerprint;
        this.bundle = null;
    }

    public SigningIdentity(String name, String fingerprint, T bundle) {
        this.name = name;
        this.fingerprint = fingerprint;
        this.bundle = bundle;
    }

    public String getName() {
        return name;
    }
    
    public String getFingerprint() {
        return fingerprint;
    }

    public T getBundle() {
        return bundle;
    }

    @Override
    public int compareTo(SigningIdentity o) {
        return this.name.compareToIgnoreCase(o.name);
    }
    
    @Override
    public String toString() {
        return "SigningIdentity [name=" + name + ", fingerprint=" + fingerprint
                + "]";
    }

    public static SigningIdentity find(List<SigningIdentity> ids, String search) {
        if (search.startsWith("/") && search.endsWith("/")) {
            Pattern pattern = Pattern.compile(search.substring(1, search.length() - 1));
            for (SigningIdentity id : ids) {
                if (pattern.matcher(id.name).find()) {
                    return id;
                }
            }
        } else {
            for (SigningIdentity id : ids) {
                if (id.name.startsWith(search) || id.fingerprint.equals(search.toUpperCase())) {
                    return id;
                }
            }
        }
        throw new IllegalArgumentException("No signing identity found matching '" + search + "'");
    }
    

    public static List<SigningIdentity> list() {
        return ToolchainUtil.listSigningIdentity();
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(list());
        } else {
            System.out.println(find(list(), args[0]));
        }
    }
}
