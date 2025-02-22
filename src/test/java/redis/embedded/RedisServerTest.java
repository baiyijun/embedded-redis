package redis.embedded;

import com.google.common.io.Resources;
import org.junit.Test;
import redis.embedded.exceptions.RedisBuildingException;
import redis.embedded.util.Architecture;
import redis.embedded.util.OS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RedisServerTest {

	private RedisServer redisServer;

	@Test
	public void testSimpleRun() throws Exception {
		redisServer = new RedisServer(6379);
		redisServer.start();
		Thread.sleep(20000L);
		redisServer.stop();
	}

	@Test(expected = RuntimeException.class)
	public void shouldNotAllowMultipleRunsWithoutStop() throws Exception {
		try {
			redisServer = new RedisServer(6379);
			redisServer.start();
			redisServer.start();
		} finally {
			redisServer.stop();
		}
	}

	@Test
	public void shouldAllowSubsequentRuns() throws Exception {
		redisServer = new RedisServer(6379);
		redisServer.start();
		redisServer.stop();

		redisServer.start();
		redisServer.stop();

		redisServer.start();
		redisServer.stop();
	}

	@Test
	public void testSimpleOperationsAfterRun() throws Exception {
		redisServer = new RedisServer(6379);
		redisServer.start();

		//JedisPool pool = null;
        //Jedis jedis = null;
        //try {
        //	pool = new JedisPool("localhost", 6379);
        //	jedis = pool.getResource();
        //	jedis.mset("abc", "1", "def", "2");
        //
        //	assertEquals("1", jedis.mget("abc").get(0));
        //	assertEquals("2", jedis.mget("def").get(0));
        //	assertNull(jedis.mget("xyz").get(0));
        //} finally {
        //	if (jedis != null)
        //		pool.returnResource(jedis);
        //	redisServer.stop();
        //}
	}

    @Test
    public void shouldIndicateInactiveBeforeStart() throws Exception {
        redisServer = new RedisServer(6379);
        assertFalse(redisServer.isActive());
    }

    @Test
    public void shouldIndicateActiveAfterStart() throws Exception {
        redisServer = new RedisServer(6379);
        redisServer.start();
        assertTrue(redisServer.isActive());
        redisServer.stop();
    }

    @Test
    public void shouldIndicateInactiveAfterStop() throws Exception {
        redisServer = new RedisServer(6379);
        redisServer.start();
        redisServer.stop();
        assertFalse(redisServer.isActive());
    }

    @Test
    public void shouldOverrideDefaultExecutable() throws Exception {
        RedisExecProvider customProvider = RedisExecProvider.defaultProvider()
                .override(OS.UNIX, Architecture.x86, Resources.getResource("redis-server-" + RedisExecProvider.redisVersion + "-linux-386").getFile())
                .override(OS.UNIX, Architecture.x86_64, Resources.getResource("redis-server-" + RedisExecProvider.redisVersion + "-linux-amd64").getFile())
                .override(OS.UNIX, Architecture.arm64, Resources.getResource("redis-server-" + RedisExecProvider.redisVersion + "-linux-arm64").getFile())
                .override(OS.MAC_OS_X, Architecture.x86_64, Resources.getResource("redis-server-" + RedisExecProvider.redisVersion + "-darwin-amd64").getFile())
                .override(OS.MAC_OS_X, Architecture.arm64, Resources.getResource("redis-server-" + RedisExecProvider.redisVersion + "-darwin-arm64").getFile());

        redisServer = new RedisServerBuilder()
                .redisExecProvider(customProvider)
                .build();
    }

    @Test(expected = RedisBuildingException.class)
    public void shouldFailWhenBadExecutableGiven() throws Exception {
        RedisExecProvider buggyProvider = RedisExecProvider.defaultProvider()
                .override(OS.UNIX, "some")
                .override(OS.MAC_OS_X, "some");

        redisServer = new RedisServerBuilder()
                .redisExecProvider(buggyProvider)
                .build();
    }

	@Test
	public void testAwaitRedisServerReady() throws Exception {
		String readyPattern =  RedisServer.builder().build().redisReadyPattern();

		assertReadyPattern(new BufferedReader(
						new InputStreamReader(getClass()
								.getClassLoader()
								.getResourceAsStream("redis-2.x-standalone-startup-output.txt"))),
				readyPattern);

		assertReadyPattern(new BufferedReader(
						new InputStreamReader(getClass()
								.getClassLoader()
								.getResourceAsStream("redis-3.x-standalone-startup-output.txt"))),
				readyPattern);

		assertReadyPattern(new BufferedReader(
						new InputStreamReader(getClass()
								.getClassLoader()
								.getResourceAsStream("redis-4.x-standalone-startup-output.txt"))),
				readyPattern);

		assertReadyPattern(new BufferedReader(
						new InputStreamReader(getClass()
								.getClassLoader()
								.getResourceAsStream("redis-6.x-standalone-startup-output.txt"))),
				readyPattern);
	}

	private void assertReadyPattern(BufferedReader reader, String readyPattern) throws IOException {
		String outputLine;
		do {
			outputLine = reader.readLine();
			assertNotNull(outputLine);
		} while (!outputLine.matches(readyPattern));
	}
}
