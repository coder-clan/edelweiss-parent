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
    /**
     * guard by this.
     */
    private int machineId;
    /**
     * guard by this.
     * Equals to (this.machineId << machineIdShiftBits)
     */
    private int machineData;

    /**
     * guard by this. Timestamp, unit: second, won't generate Id after this point of time.
     */
    private long machineIdExpiringTime;
    /**
     * guard by this.
     */
    private long lastUsedTime;
    /**
     * guard by this.
     * per-machine sequence value.
     */
    private int sequence = 0;

    private SnowFlakeIdGenerator() {
    }

    private static final SnowFlakeIdGenerator INSTANCE = new SnowFlakeIdGenerator();

    /**
     * @return a singleton instance of SnowFlakeIdGenerator.
     */
    public static SnowFlakeIdGenerator instance() {
        return INSTANCE;
    }

    ;

    /**
     * Set machine ID. Set it to -1 will make {@link #generateId()} throws IllegalStateException.
     *
     * @param machineId machine ID should be unique at any time.
     */
    public synchronized void setMachineId(int machineId, long machineIdExpiringTime) {
        if (machineId > Constants.maxMachineId) {
            throw new IllegalArgumentException("machineId must be within the range[0," + Constants.maxMachineId + "].");
        }
        this.machineId = machineId;
        this.machineData = machineId << Constants.machineIdShiftBits;
        this.machineIdExpiringTime = machineIdExpiringTime;
    }

    @Override
    public long generateId() throws IllegalStateException {
        // this call is expensive, don't put it into the following synchronized block
        long currentTime = System.currentTimeMillis();

        synchronized (this) {

            if (currentTime > this.machineIdExpiringTime * 1000) {
                throw new IllegalStateException("The SnowFlakeIdGenerator hasn't gotten a valid machine id.");
            }

            // Wait until current currentTime is greater than or equal to this.lastTime
            // System Clock may be changed. e.g. NTP update the system clock backward.
            while (currentTime < lastUsedTime) {
                log.warn("Time ran backward! currentTime={}, lastUsedTime={}", currentTime, lastUsedTime);
                Thread.yield();
                currentTime = System.currentTimeMillis();
            }

            if (currentTime > lastUsedTime) {
                // reset sequence;
                sequence = 0;
                this.lastUsedTime = currentTime;
            }

            // if (currentTime - epoch > maxTimestamp) == true, the returning ID will be a negative number.
            // returning negative IDs is better than throwing an exception.
            long id = ((currentTime - Constants.EPOCH) << Constants.timestampShiftBits) | machineData | (sequence++);

            // sequence exceeds max value
            if (sequence > Constants.maxSequenceValue) {
                // increase this.lastTime to make the next ID can only be generated at (or after) the next millisecond.
                lastUsedTime += 1L;
                sequence = 0;
            }

            return id;
        }
    }
}
