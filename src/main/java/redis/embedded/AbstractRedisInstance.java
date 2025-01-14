package redis.embedded;

import cn.hutool.core.date.StopWatch;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.exceptions.EmbeddedRedisException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

abstract class AbstractRedisInstance
        implements Redis {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRedisInstance.class);
    
    private final int port;
    private final int tlsPort;
    protected List<String> args = Collections.emptyList();
    private volatile boolean active = false;
    private Process redisProcess;
    private ExecutorService executor;
    
    protected AbstractRedisInstance(int port, int tlsPort) {
        this.port = port;
        this.tlsPort = tlsPort;
    }
    
    protected AbstractRedisInstance(int port) {
        this(port, 0);
    }
    
    public boolean isActive() {
        return active;
    }
    
    public synchronized void start() throws EmbeddedRedisException {
        if (active) {
            throw new EmbeddedRedisException("This redis server instance is already running...");
        }
        final StopWatch watch = new StopWatch();
        watch.start();
        try {
            redisProcess = createRedisProcessBuilder().start();
            ProcessOutputLogger.logOutput(LOG, redisProcess, "redis");
            installExitHook();
            awaitRedisServerReady(watch);
            active = true;
        } catch (IOException e) {
            throw new EmbeddedRedisException("Failed to start Redis instance", e);
        } finally {
            watch.stop();
        }
    }
    
    private void installExitHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "RedisInstanceCleaner"));
    }
    
    
    private void awaitRedisServerReady(StopWatch watch) throws IOException {
        final long start = System.nanoTime();
        final long maxWaitNs = TimeUnit.NANOSECONDS.convert(10, TimeUnit.SECONDS);
        while (System.nanoTime() - start < maxWaitNs) {
            try {
                verifyReady();
                LOG.info("redis postmaster startup finished in {}", watch);
                return;
            } catch (Exception e) {
                LOG.trace("While waiting for server startup", e);
            }
            
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
    
    private void verifyReady() {
        final InetAddress localhost = InetAddress.getLoopbackAddress();
        try (Socket sock = new Socket()) {
            sock.setSoTimeout((int) Duration.ofMillis(500).toMillis());
            sock.connect(new InetSocketAddress(localhost, port), (int) Duration.ofMillis(500).toMillis());
        } catch (final IOException e) {
            throw new RuntimeException("Can't start redis server. Check logs for details");
        }
    }
    
    protected abstract String redisReadyPattern();
    
    private ProcessBuilder createRedisProcessBuilder() {
        File executable = new File(args.getFirst());
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(executable.getParentFile());
        return pb;
    }
    
    public synchronized void stop() throws EmbeddedRedisException {
        if (active) {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
            redisProcess.destroy();
            tryWaitFor();
            active = false;
        }
    }
    
    private void tryWaitFor() {
        try {
            redisProcess.waitFor();
        } catch (InterruptedException e) {
            throw new EmbeddedRedisException("Failed to stop redis instance", e);
        }
    }
    
    public List<Integer> ports() {
        return port > 0 ? Collections.singletonList(port) : Collections.emptyList();
    }
    
    public List<Integer> tlsPorts() {
        return tlsPort > 0 ? Collections.singletonList(tlsPort) : Collections.emptyList();
    }
    
    private static class PrintReaderRunnable
            implements Runnable {
        private final BufferedReader reader;
        
        private PrintReaderRunnable(BufferedReader reader) {
            this.reader = reader;
        }
        
        public void run() {
            try {
                readLines();
            } finally {
                IOUtils.closeQuietly(reader, null);
            }
        }
        
        public void readLines() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
