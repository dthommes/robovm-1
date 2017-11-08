package org.robovm.compiler.util.platforms;

public final class SystemInfo {
    public enum OSInfo {
        linux,
        macosx,
        macosxlinux,
        windows,
        unknownOS
    }

    public enum ArchInfo {
        x86_64,
        x86,
        unknownArch
    }

    private static final SystemInfo systemInfo;

    public final OSInfo os;
    public final String osName;
    public final ArchInfo archName;
    public final String arch;
    public final String libExt;

    public SystemInfo(OSInfo os, String osName, ArchInfo archName, String arch, String libExt) {
        this.os = os;
        this.osName = osName;
        this.archName = archName;
        this.arch = arch;
        this.libExt = libExt;
    }

    static SystemInfo getSystemInfo() {
        return systemInfo;
    }

    static {
        String osProp = System.getProperty("os.name").toLowerCase();
        String archProp = System.getProperty("os.arch").toLowerCase();
        OSInfo os;
        ArchInfo arch;
        String ext = null;
        if (osProp.startsWith("mac") || osProp.startsWith("darwin")) {
            if (System.getenv("ROBOVM_FORCE_MACOSXLINUX") != null || System.getProperty("ROBOVM_FORCE_MACOSXLINUX") != null)
                os = OSInfo.macosxlinux;
            else
                os = OSInfo.macosx;
            ext = "dylib";
        } else if (osProp.startsWith("linux")) {
            os = OSInfo.linux;
            ext = "so";
        } else if (osProp.startsWith("windows")) {
            os = OSInfo.windows;
            ext = "dll";
        } else {
            os = OSInfo.unknownOS;
        }
        if (archProp.matches("amd64|x86[-_]64")) {
            arch = ArchInfo.x86_64;
        } else if (archProp.matches("i386|x86")) {
            arch = ArchInfo.x86;
        } else {
            arch = ArchInfo.unknownArch;
        }

        systemInfo = new SystemInfo(os, osProp, arch, archProp, ext);
    }
}
