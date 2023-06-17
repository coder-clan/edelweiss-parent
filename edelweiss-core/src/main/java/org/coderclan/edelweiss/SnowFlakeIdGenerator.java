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
     * Guard by "this".
     */
    private int machineId;
    /**
     * Guard by "this".
     * Equals to (this.machineId << machineIdShiftBits)
     */
    private int machineData;

    /**
     * Guard by "this". Timestamp, unit: second, won't generate Id after this point of time.
     */
    private long machineIdExpiringTime;
    /**
     * Guard by "this".
     */
    private long lastUsedTime;
    /**
     * Guard by "this".
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

    /**
     * Set machine ID. Set it to -1 will make {@link #generateId()} throws IllegalStateException.
     *
     * @param machineId machine ID should be unique at any time.
     * @param machineIdExpiringTime machine ID will be expired after this time. unit: second.
     */
    public synchronized void setMachineId(int machineId, long machineIdExpiringTime) {
        if (machineId > Constants.MAX_MACHINE_ID) {
            throw new IllegalArgumentException("machineId must be within the range[0," + Constants.MAX_MACHINE_ID + "].");
        }
        if (this.machineId != machineId) {
            log.info("Machine ID changed into {} from {}.", machineId, this.machineId);
            this.machineId = machineId;
        }
        log.debug("machineIdExpiringTime extended to: {}", machineIdExpiringTime);
        this.machineData = machineId << Constants.MACHINE_ID_SHIFT_BITS;
        this.machineIdExpiringTime = machineIdExpiringTime;
    }

    @Override
    // Use Thread.sleep() instead of Object.wait(), because other threads can not execute too.
    @SuppressWarnings("java:S2276")
    public long generateId() throws IllegalStateException {
        // this call is expensive, don't put it into the following synchronized block
        long currentTime = System.currentTimeMillis();

        synchronized (this) {

            if (currentTime > this.machineIdExpiringTime * 1000) {
                throw new IllegalStateException("The SnowFlakeIdGenerator hasn't gotten a valid machine id.");
            }

            // Wait until current currentTime is greater than or equal to this.lastTime
            // System Clock may be changed. e.g. NTP update the system clock backward.
            //
            // Accuracy of NTP depends on latency of network.
            // Accuracy of NTP on Internet would be from 20ms to 500ms
            // Accuracy of NTP on Intranet would be less than 1ms
            // It's better to use local NTP Server.
            while (currentTime < lastUsedTime) {
                log.warn("Time ran backward! currentTime={}, lastUsedTime={}", currentTime, lastUsedTime);
                try {
                    Thread.sleep(lastUsedTime - currentTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                currentTime = System.currentTimeMillis();
            }

            if (currentTime > lastUsedTime) {
                // reset sequence
                sequence = 0;
                this.lastUsedTime = currentTime;
            }

            // if (currentTime - epoch > maxTimestamp) == true, the returning ID will be a negative number.
            // returning negative IDs is better than throwing an exception.
            long id = ((currentTime - Constants.EPOCH) << Constants.TIMESTAMP_SHIFT_BITS) | machineData | (sequence++);

            // sequence exceeds max value
            if (sequence > Constants.MAX_SEQUENCE_VALUE) {
                // increase this.lastTime to make the next ID can only be generated at (or after) the next millisecond.
                lastUsedTime += 1L;
                sequence = 0;
            }

            return id;
        }
    }
}
