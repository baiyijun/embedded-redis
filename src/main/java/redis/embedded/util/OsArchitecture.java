package redis.embedded.util;

import com.google.common.base.Preconditions;

public class OsArchitecture {
    
    public static final OsArchitecture UNIX_x86 = new OsArchitecture(OS.UNIX, Architecture.x86);
    public static final OsArchitecture UNIX_x86_64 = new OsArchitecture(OS.UNIX, Architecture.x86_64);
    public static final OsArchitecture UNIX_arm64 = new OsArchitecture(OS.UNIX, Architecture.arm64);
    
    public static final OsArchitecture MAC_OS_X_x86_64 = new OsArchitecture(OS.MAC_OS_X, Architecture.x86_64);
    public static final OsArchitecture MAC_OS_X_arm64 = new OsArchitecture(OS.MAC_OS_X, Architecture.arm64);
    public static final OsArchitecture WIN_x64_86 = new OsArchitecture(OS.WINDOWS, Architecture.x86_64);
    
    private final OS os;
    private final Architecture arch;
    
    public OsArchitecture(OS os, Architecture arch) {
        Preconditions.checkNotNull(os);
        Preconditions.checkNotNull(arch);
        
        this.os = os;
        this.arch = arch;
    }
    
    public static OsArchitecture detect() {
        OS os = OSDetector.getOS();
        Architecture arch = OSDetector.getArchitecture();
        return new OsArchitecture(os, arch);
    }
    
    public OS os() {
        return os;
    }
    
    public Architecture arch() {
        return arch;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        
        OsArchitecture that = (OsArchitecture) o;
        
        return arch == that.arch && os == that.os;
        
    }
    
    @Override
    public int hashCode() {
        int result = os.hashCode();
        result = 31 * result + arch.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s)", os.name(), arch.name());
    }
}
