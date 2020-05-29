package org.robovm.compiler.config;

public final class Lib {
    private final String value;
    private final boolean force;

    public Lib(String value, boolean force) {
        this.value = value;
        this.force = force;
    }

    public String getValue() {
        return value;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public String toString() {
        return "Lib [value=" + value + ", force=" + force + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (force ? 1231 : 1237);
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
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
        Lib other = (Lib) obj;
        if (force != other.force) {
            return false;
        }
        if (value == null) {
            return other.value == null;
        } else return value.equals(other.value);
    }
}
