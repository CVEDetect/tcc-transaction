package org.mengyun.tcctransaction.storage.helper;

import org.mengyun.tcctransaction.api.Xid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.Collection;
import java.util.Map;

/**
 * Created by changming.xie on 9/15/16.
 */
public class RedisHelper {

    public static final String SEPARATOR = ":";
    public static final String DELETED_KEY_PREIFX = "DELETE:";
    public static final String DOMAIN_KEY_PREIFX = "_TCCDOMAIN:";
    public static final int DELETED_KEY_KEEP_TIME = 3 * 24 * 3600;
    public static final String LEFT_BIG_BRACKET = "{";
    public static final String RIGHT_BIG_BRACKET = "}";
    static final Logger log = LoggerFactory.getLogger(RedisHelper.class.getSimpleName());
    public static int SCAN_COUNT = 30;
    public static int SCAN_MIDDLE_COUNT = 1000;
    public static String SCAN_TEST_PATTERN = "*";
    public static String REDIS_SCAN_INIT_CURSOR = ShardOffset.SCAN_INIT_CURSOR;

    public static byte[] getDomainStoreRedisKey(String domain) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DOMAIN_KEY_PREIFX);
        if (!domain.endsWith(SEPARATOR)) {
            stringBuilder.append(SEPARATOR);
        }
        stringBuilder.append(domain);
        return stringBuilder.toString().getBytes();
    }

    public static byte[] getRedisKey(String keyPrefix, Xid xid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(keyPrefix);
        if (!keyPrefix.endsWith(SEPARATOR)) {
            stringBuilder.append(SEPARATOR);
        }
        stringBuilder.append(xid.toString());
        return stringBuilder.toString().getBytes();
    }

    /**
     * @param keyPrefix
     * @param xid
     * @return DELETE:{redisKey}
     */
    public static byte[] getDeletedRedisKey(String keyPrefix, Xid xid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DELETED_KEY_PREIFX);
        stringBuilder.append(LEFT_BIG_BRACKET);
        stringBuilder.append(keyPrefix);
        if (!keyPrefix.endsWith(SEPARATOR)) {
            stringBuilder.append(SEPARATOR);
        }
        stringBuilder.append(xid.toString());
        stringBuilder.append(RIGHT_BIG_BRACKET);
        return stringBuilder.toString().getBytes();
    }

    public static String getDeletedKeyPreifx(String domain) {
        return DELETED_KEY_PREIFX.concat(LEFT_BIG_BRACKET).concat(domain);
    }

    public static byte[] getRedisKey(String keyPrefix, String globalTransactionId, String branchQualifier) {
        return new StringBuilder().append(keyPrefix)
                .append(globalTransactionId)
                .append(":")
                .append(branchQualifier)
                .toString()
                .getBytes();
    }

    public static <T> T execute(JedisPool jedisPool, JedisCallback<T> callback) {
        try (Jedis jedis = jedisPool.getResource()) {
            return callback.doInJedis(jedis);
        }
    }

    public static <T> T execute(ShardedJedisPool jedisPool, ShardedJedisCallback<T> callback) {
        try (ShardedJedis jedis = jedisPool.getResource()) {
            return callback.doInJedis(jedis);
        }
    }

    public static ScanParams buildDefaultScanParams(String pattern, int count) {
        return new ScanParams().match(pattern).count(count);
    }

    public static boolean isSupportScanCommand(Jedis jedis) {
        try {
            ScanParams scanParams = buildDefaultScanParams(SCAN_TEST_PATTERN, SCAN_COUNT);
            jedis.scan(REDIS_SCAN_INIT_CURSOR, scanParams);
        } catch (JedisDataException e) {
            log.error(e.getMessage(), e);
            log.info("Redis **NOT** support scan command");
            return false;
        }

        log.info("Redis support scan command");
        return true;
    }

    static public boolean isSupportScanCommand(JedisPool pool) {
        return execute(pool, jedis -> isSupportScanCommand(jedis));
    }

    static public boolean isSupportScanCommand(ShardedJedisPool shardedJedisPool) {
        Collection<Jedis> allShards = shardedJedisPool.getResource().getAllShards();

        for (Jedis jedis : allShards) {
            try {
                jedis.connect();
                if (!isSupportScanCommand(jedis)) {
                    return false;
                }
            } finally {
                if (jedis.isConnected()) {
                    jedis.disconnect();
                }
            }
        }

        return true;
    }

    static public boolean isSupportScanCommand(JedisCluster jedisCluster) {
        Map<String, JedisPool> jedisPoolMap = jedisCluster.getClusterNodes();

        for (Map.Entry<String, JedisPool> entry : jedisPoolMap.entrySet()) {
            if (!isSupportScanCommand(entry.getValue())) {
                return false;
            }
        }

        return true;
    }

    public static ScanParams scanArgs(String pattern, int count) {
        return new ScanParams().match(pattern).count(count);
    }
}