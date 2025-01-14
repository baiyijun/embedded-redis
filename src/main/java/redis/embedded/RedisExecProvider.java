package redis.embedded;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import redis.embedded.util.Architecture;
import redis.embedded.util.JarUtil;
import redis.embedded.util.OS;
import redis.embedded.util.OsArchitecture;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class RedisExecProvider {

    public static final String redisVersion = "7.0.15";
    //(windows-version)https://github.com/zkteco-home/redis-windows/blob/master/redis-server.exe
    public static final String redisVersionWindows = "7.2.5";
    private final Map<OsArchitecture, String> executables = Maps.newHashMap();


    private String dataPath = null;

    private RedisExecProvider() {
        initExecutables();
    }

    public static RedisExecProvider defaultProvider() {
        return new RedisExecProvider();
    }

    private void initExecutables() {
        executables.put(OsArchitecture.UNIX_x86, "redis-server-" + redisVersion + "-linux-386");
        executables.put(OsArchitecture.UNIX_x86_64, "redis-server-" + redisVersion + "-linux-amd64");
        executables.put(OsArchitecture.UNIX_arm64, "redis-server-" + redisVersion + "-linux-arm64");

        executables.put(OsArchitecture.MAC_OS_X_x86_64, "redis-server-" + redisVersion + "-darwin-amd64");
        executables.put(OsArchitecture.MAC_OS_X_arm64, "redis-server-" + redisVersion + "-darwin-arm64");

        executables.put(OsArchitecture.WIN_x64_86, "redis-server-" + redisVersionWindows + "-windows-amd64.exe");

    }

    public RedisExecProvider setDataDirectory(String dataPath) {
        Preconditions.checkNotNull(dataPath);
        this.dataPath = dataPath;
        return this;
    }

    public String getDataPath() {
        return dataPath;
    }


    public RedisExecProvider override(OS os, String executable) {
        Preconditions.checkNotNull(executable);
        for (Architecture arch : Architecture.values()) {
            override(os, arch, executable);
        }
        return this;
    }

    public RedisExecProvider override(OS os, Architecture arch, String executable) {
        Preconditions.checkNotNull(executable);
        executables.put(new OsArchitecture(os, arch), executable);
        return this;
    }

    public File get() throws IOException {
        OsArchitecture osArch = OsArchitecture.detect();

        if (!executables.containsKey(osArch)) {
            throw new IllegalArgumentException("No Redis executable found for " + osArch);
        }

        String executablePath = executables.get(osArch);
        if (dataPath == null) {
            dataPath = Files.createTempDir().getAbsolutePath();
        }
        return fileExists(executablePath) ? new File(executablePath) : JarUtil.extractExecutableFromJar(dataPath, executablePath);

    }

    private boolean fileExists(String executablePath) {
        return new File(executablePath).exists();
    }
}
