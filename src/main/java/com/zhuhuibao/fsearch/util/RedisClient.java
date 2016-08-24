package com.zhuhuibao.fsearch.util;

import com.mchange.v2.ser.SerializableUtils;
import com.zhuhuibao.fsearch.G;
import com.zhuhuibao.fsearch.L;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author jianglz
 * @since 2016/8/22.
 */
public class RedisClient {

    private static String HOST = "127.0.0.1";
    private static int PORT = 6379;
    private static int EXPIRE = 1800;
    private static int MAX_IDLE = 200;
    private static int MAX_WAIT = 10000;
    private static int TIMEOUT = 10000;
    private static boolean TEST_ON_BORROW = true;

    private static JedisPool jedisPool = null;

    static {
        HOST = G.getConfig().getString("redis.host", HOST);
        PORT = G.getConfig().getInt("redis.port", PORT);
        EXPIRE = G.getConfig().getInt("redis.expire", EXPIRE);
        MAX_IDLE = G.getConfig().getInt("redis.maxIdle", MAX_IDLE);
        MAX_WAIT = G.getConfig().getInt("redis.maxWait", MAX_WAIT);
        TIMEOUT = G.getConfig().getInt("redis.timeout", TIMEOUT);
        TEST_ON_BORROW = G.getConfig().getBoolean("redis.testOnBorrow", TEST_ON_BORROW);
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxIdle(MAX_IDLE);
            config.setMaxWaitMillis(MAX_WAIT);
            config.setTestOnBorrow(TEST_ON_BORROW);
            jedisPool = new JedisPool(config, HOST, PORT, TIMEOUT);
        } catch (Exception e) {
            e.printStackTrace();
            L.error("redis init fail:>>>", e);
        }

    }

    /**
     * 获取Jedis实例
     *
     * @return
     */
    public synchronized static Jedis getJedis() {
        try {
            if (jedisPool != null) {
                return jedisPool.getResource();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            L.error("get jedis error:>>>", e);
            return null;
        }
    }

    /**
     * 释放jedis资源
     *
     * @param jedis jedis
     */
    public static void release(final Jedis jedis) {
        if (jedis != null) {
            jedisPool.returnResource(jedis);
        }
    }

    public Set list() {
        Set result = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            if (jedis != null) {
                result = jedis.keys("*");
                for (Object obj1 : result) {
                    System.out.println(obj1);
                }
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            L.error("redis get error:>>>", e);
        } finally {
            if (jedis != null) {
                release(jedis);
            }
        }
        return result;
    }


    public Object get(String key) {
        Object result = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();

            if (jedis != null) {
                String type = jedis.type(key);
                System.out.println(type);
                if(jedis.exists(key)){
                    byte[] bytes = jedis.get(key).getBytes();
                    System.out.println(SerializationUtil.deserialize(bytes));
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            L.error("redis get error:>>>", e);
        } finally {
            if (jedis != null) {
                release(jedis);
            }
        }
        return result;
    }

    public static void main(String[] args) {
        RedisClient client = new RedisClient();
        client.list();

        client.get("map_130300");
    }
}
