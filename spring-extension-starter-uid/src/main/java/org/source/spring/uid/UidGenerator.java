package org.source.spring.uid;


import org.source.utility.utils.Strings;
import org.springframework.util.Assert;

public class UidGenerator {
    private static final int ID_BITS = 63;
    private final long nodeId;
    private final long startTimestamp;
    private final int nodeIdMoveBits;
    private final int timestampMoveBits;
    private final long maxSequence;

    /**
     * @param nodeId         nodeId
     * @param startTimestamp 起始时间戳
     * @param nodeIdBits     服务节点ID占位数
     * @param sequenceBits   递增序列占位数
     */
    public UidGenerator(long nodeId, long startTimestamp, int nodeIdBits, int sequenceBits) {
        this.nodeId = nodeId;
        // 起始时间戳
        this.startTimestamp = startTimestamp;
        Assert.isTrue(nodeIdBits > 0 && nodeIdBits < 63, Strings.format("nodeIdBits:{}所占位数必须大于0,小于{}", nodeIdBits, ID_BITS));
        // 最大nodeId
        long maxNodeId = ~(-1L << nodeIdBits);
        Assert.isTrue(nodeId >= 0 && nodeId <= maxNodeId, Strings.format("nodeId:{} 必须大于等于0，小于等于maxNodeId:{}", nodeId, maxNodeId));
        // nodeId 左移位数
        Assert.isTrue(sequenceBits > 0 && sequenceBits < 63, Strings.format("sequenceBits:{}所占位数必须大于0,小于{}", sequenceBits, ID_BITS));
        this.nodeIdMoveBits = sequenceBits;
        // 时间戳 左移位数
        this.timestampMoveBits = nodeIdBits + sequenceBits;
        // 最大序列数
        this.maxSequence = ~(-1L << sequenceBits);
    }

    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;
    /**
     * 上次生成ID的时间截
     */
    private long lastTimestamp = -1L;

    // 线程安全的获得下一个 ID 的方法
    public synchronized long nextId() {
        long timestamp = currentTime();
        // 如果当前时间小于上一次ID生成的时间戳，可能生成重复ID
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(Strings.format("current timestamp:{} less then lastTimestamp:{}", timestamp, lastTimestamp));
        }
        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & this.maxSequence;
            // 毫秒内序列溢出 即 序列 > 4095
            if (sequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = blockTillNextMillis(lastTimestamp);
            }
        }
        // 时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }
        // 上次生成ID的时间截
        lastTimestamp = timestamp;
        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - this.startTimestamp) << this.timestampMoveBits)
                | (this.nodeId << this.nodeIdMoveBits)
                | sequence;
    }

    // 阻塞到下一个毫秒 即 直到获得新的时间戳
    protected long blockTillNextMillis(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }

    // 获得以毫秒为单位的当前时间
    protected long currentTime() {
        return System.currentTimeMillis();
    }

}