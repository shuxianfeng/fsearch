package com.movision.fsearch.util;

import com.movision.fsearch.L;
import com.movision.fsearch.G;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;

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

    public Set<String> keys(String keyword) {
        Set result = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            if (jedis != null) {
                result = jedis.keys(keyword);

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

                if (jedis.exists(key)) {

                    byte[] bytes = jedis.get(key.getBytes());
                    result = SerializationUtil.deserialize(bytes);

                } else{
                    L.error("key is not exist");
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

    public void set(String key, Object value) {
        Jedis jedis = null;
        try {
            jedis = getJedis();

            if (jedis != null) {

                jedis.set(key.getBytes(), SerializationUtil.serialize(value));
            }

        } catch (Exception e) {
            e.printStackTrace();
            L.error("redis get error:>>>", e);
        } finally {
            if (jedis != null) {
                release(jedis);
            }
        }
    }

    public void del(String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis();

            if (jedis != null) {

                jedis.del(key);
            }

        } catch (Exception e) {
            e.printStackTrace();
            L.error("redis get error:>>>", e);
        } finally {
            if (jedis != null) {
                release(jedis);
            }
        }
    }

    /**
     * flushdb删除当前选择数据库中的所有key
     */
    public static boolean clearDB() {
        Jedis jedis = null;
        try {
            jedis = getJedis();

            if (jedis != null) {

                jedis.flushDB();
            }

        } catch (Exception e) {
            e.printStackTrace();
            L.error("redis flush all error:>>>", e);
            return false;
        } finally {
            if (jedis != null) {
                release(jedis);
            }
        }
        return true;
    }

    /**
     * 删除所有数据库中的所有key
     */
    public static boolean clearAll() {
        Jedis jedis = null;
        try {
            jedis = getJedis();

            if (jedis != null) {

                jedis.flushAll();
            }

        } catch (Exception e) {
            e.printStackTrace();
            L.error("redis flush all error:>>>", e);
            return false;
        } finally {
            if (jedis != null) {
                release(jedis);
            }
        }
        return true;
    }


    //test
    public static void main(String[] args) {
        RedisClient client = new RedisClient();
        Set<String> keys = client.keys("map_*");
        System.out.println(keys);

//        client.set("AAAA", "你大爷还是你大爷");

//        Object value = client.get("AAAA");
//        System.out.println(value.toString());

//        client.del("AAAA");
//        System.out.println(client.get("AAAA"));


    }
}
