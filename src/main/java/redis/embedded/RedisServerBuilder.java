package redis.embedded;

import cn.hutool.core.io.FileUtil;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import redis.embedded.exceptions.RedisBuildingException;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RedisServerBuilder {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String CONF_FILENAME = "embedded-redis-server";
    
    private File executable;
    private RedisExecProvider redisExecProvider = RedisExecProvider.defaultProvider();
    private String bind = "127.0.0.1";
    private int port = 6379;
    private int tlsPort = 0;
    private InetSocketAddress slaveOf;
    private String redisConf;
    
    private StringBuilder redisConfigBuilder;
    
    public RedisServerBuilder redisExecProvider(RedisExecProvider redisExecProvider) {
        this.redisExecProvider = redisExecProvider;
        return this;
    }
    
    public RedisServerBuilder bind(String bind) {
        this.bind = bind;
        return this;
    }
    
    public RedisServerBuilder port(int port) {
        this.port = port;
        return this;
    }
    
    public RedisServerBuilder tlsPort(int tlsPort) {
        this.tlsPort = tlsPort;
        return this;
    }
    
    public RedisServerBuilder slaveOf(String hostname, int port) {
        this.slaveOf = new InetSocketAddress(hostname, port);
        return this;
    }
    
    public RedisServerBuilder slaveOf(InetSocketAddress slaveOf) {
        this.slaveOf = slaveOf;
        return this;
    }
    
    public RedisServerBuilder configFile(String redisConf) {
        if (redisConfigBuilder != null) {
            throw new RedisBuildingException("Redis configuration is already partially build using setting(String) method!");
        }
        this.redisConf = redisConf;
        return this;
    }
    
    public RedisServerBuilder setting(String configLine) {
        if (redisConf != null) {
            throw new RedisBuildingException("Redis configuration is already set using redis conf file!");
        }
        
        if (redisConfigBuilder == null) {
            redisConfigBuilder = new StringBuilder();
        }
        
        redisConfigBuilder.append(configLine);
        redisConfigBuilder.append(LINE_SEPARATOR);
        return this;
    }
    
    public RedisServer build() {
        setting("bind " + bind);
        tryResolveConfAndExec();
        List<String> args = buildCommandArgs();
        return new RedisServer(args, port, tlsPort);
    }
    
    public void reset() {
        this.executable = null;
        this.redisConfigBuilder = null;
        this.slaveOf = null;
        this.redisConf = null;
    }
    
    private void tryResolveConfAndExec() {
        try {
            resolveConfAndExec();
        } catch (IOException e) {
            throw new RedisBuildingException("Could not build server instance", e);
        }
    }
    
    private void resolveConfAndExec() throws IOException {
        if (redisConf == null && redisConfigBuilder != null) {
            File redisConfigFile;
            if (redisExecProvider.getDataPath() != null) {
                File directory = new File(redisExecProvider.getDataPath());
                if (!directory.exists()) {
                    FileUtil.mkdir(directory);
                }
                redisConfigFile = FileUtil.touch(directory, resolveConfigName());
            }
            else {
                redisConfigFile = FileUtil.touch(FileUtil.getTmpDirPath(), resolveConfigName());
            }
            Files.asCharSink(redisConfigFile, StandardCharsets.UTF_8).write(redisConfigBuilder.toString());
            redisConf = redisConfigFile.getAbsolutePath();
        }
        
        try {
            executable = redisExecProvider.get();
        } catch (Exception e) {
            throw new RedisBuildingException("Failed to resolve executable", e);
        }
    }
    
    private String resolveConfigName() {
        return CONF_FILENAME + "_" + port + ".conf";
    }
    
    private List<String> buildCommandArgs() {
        List<String> args = new ArrayList<>();
        args.add(executable.getAbsolutePath());
        
        if (!Strings.isNullOrEmpty(redisConf)) {
            args.add(redisConf);
        }
        
        args.add("--port");
        args.add(Integer.toString(port));
        
        if (tlsPort > 0) {
            args.add("--tls-port");
            args.add(Integer.toString(tlsPort));
        }
        
        if (slaveOf != null) {
            args.add("--slaveof");
            args.add(slaveOf.getHostName());
            args.add(Integer.toString(slaveOf.getPort()));
        }
        
        return args;
    }
}
