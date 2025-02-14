package redis.embedded.util;

import redis.embedded.exceptions.OsDetectionException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class OSDetector {
    
    public static OS getOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return OS.WINDOWS;
        }
        else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return OS.UNIX;
        }
        else if ("Mac OS X".equalsIgnoreCase(osName)) {
            return OS.MAC_OS_X;
        }
        else {
            throw new OsDetectionException("Unrecognized OS: " + osName);
        }
    }
    
    public static Architecture getArchitecture() {
        OS os = getOS();
        switch (os) {
            case WINDOWS:
                return getWindowsArchitecture();
            case UNIX:
                return getUnixArchitecture();
            case MAC_OS_X:
                return getMacOSXArchitecture();
            default:
                throw new OsDetectionException("Unrecognized OS: " + os);
        }
    }
    
    private static Architecture getWindowsArchitecture() {
        String arch = System.getenv("PROCESSOR_ARCHITECTURE");
        String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
        
        if (arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64")) {
            return Architecture.x86_64;
        }
        else {
            return Architecture.x86;
        }
    }
    
    private static Architecture getUnixArchitecture() {
        try {
            Process proc = Runtime.getRuntime().exec("uname -m");
            try (BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String machine = input.readLine();
                switch (machine) {
                    case "aarch64":
                        return Architecture.arm64;
                    case "x86_64":
                        return Architecture.x86_64;
                    default:
                        throw new OsDetectionException("unsupported architecture: " + machine);
                }
            }
        } catch (Exception e) {
            if (e instanceof OsDetectionException) {
                throw (OsDetectionException) e;
            }
            throw new OsDetectionException(e);
        }
    }
    
    private static Architecture getMacOSXArchitecture() {
        try {
            Process proc = Runtime.getRuntime().exec("uname -m");
            try (BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String machine = input.readLine();
                switch (machine) {
                    case "arm64":
                        return Architecture.arm64;
                    case "x86_64":
                        return Architecture.x86_64;
                    default:
                        throw new OsDetectionException("unsupported architecture: " + machine);
                }
            }
        } catch (Exception e) {
            if (e instanceof OsDetectionException) {
                throw (OsDetectionException) e;
            }
            throw new OsDetectionException(e);
        }
    }
}
