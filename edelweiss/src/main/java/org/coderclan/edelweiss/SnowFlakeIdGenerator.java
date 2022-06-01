package org.coderclan.edelweiss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Thread Safe
 * <p>
 * https://en.wikipedia.org/wiki/Snowflake_ID
 * <p>
 * Snowflakes are 64 bits. (Only 63 are used to fit in a signed integer.) The first 41 bits are a timestamp,
 * representing milliseconds since the chosen epoch. The next 10 bits represent a machine ID, preventing clashes.
 * Twelve more bits represent a per-machine sequence number, to allow creation of multiple snowflakes in the same millisecond.
 *
 * @author aray(dot)chou(dot)cn(at)gmail(dot)com
 */
public class SnowFlakeIdGenerator implements IdGenerator {
    private static final Logger log = LoggerFactory.getLogger(SnowFlakeIdGenerator.class);
    private final static int timestampShiftBits = 10 + 12; // 10 bits for machineId, 12 bits for per-machine sequence number;
    private final static int machineIdShiftBits = 12; // 12 bits for per-machine sequence number;
    private final static long maxTimestamp = 2L ^ 41 - 1;
    private final static int maxMachineId = 2 ^ 10 - 1;
    private final static int maxSequenceValue = 2 ^ 12 - 1;
    private final static long epoch = 1654094343806L;
    private final int machineId;
    /**
     * Equals to (this.machineId << machineIdShiftBits)
     */
    private final int machineData;
    /**
     * guard by this.
     */
    private long lastTime;
    /**
     * guard by this.
     * per-machine sequence value.
     */
    private int sequence = 0;

    public SnowFlakeIdGenerator(int machineId) {
        if (machineId > maxMachineId) {
            throw new IllegalArgumentException("machineId must be within the range[0," + maxMachineId + "].");
        }
        this.machineId = machineId;
        this.machineData = machineId << machineIdShiftBits;
    }

    @Override
    public long generateId() {
        long time = System.currentTimeMillis();
        synchronized (this) {
            // sequence exceeds max value
            if (sequence > maxSequenceValue) {
                // increase this.lastTime to make current thread wait.
                lastTime += 1L;
                sequence = 0;
            }

            // Wait until current time is greater than or equal to this.lastTime
            // System Clock may be changed. e.g. NTP update the system clock backward.
            while (time < lastTime) {
                log.warn("");
                Thread.yield();
                time = System.currentTimeMillis();
            }

            if (time > lastTime) {
                // reset sequence;
                sequence = 0;
                this.lastTime = time;
            }

            // if (time - epoch > maxTimestamp) == true, the returning ID will be negative number.
            // returning negative IDs is better than throwing an exception.
            return ((time - epoch) << timestampShiftBits) | machineData | (sequence++);
        }
    }
}
