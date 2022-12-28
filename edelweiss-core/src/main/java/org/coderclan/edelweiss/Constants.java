package org.coderclan.edelweiss;

/**
 * @author aray(dot)chou(dot)cn(at)gmail(dot)com
 */
public final class Constants {
    public static final long EPOCH = 1654094343806L;
    /**
     * 10 bits for machineId, 12 bits for per-machine sequence number
     */
    public static final int TIMESTAMP_SHIFT_BITS = 10 + 12;

    /**
     * 12 bits for per-machine sequence number
     */
    public static final int MACHINE_ID_SHIFT_BITS = 12;
    public static final long MAX_TIMESTAMP = (1L << 41) - 1;
    public static final int MAX_MACHINE_ID = (1 << 10) - 1;
    public static final int MAX_SEQUENCE_VALUE = (1 << 12) - 1;
    /**
     * The instance ID should be designed to live a bit logger than it expected to solve the problems: un-accurate system clock.
     */
    public static final double DEFAULT_INSTANCE_ID_TTL_MARGIN_RATE = 0.2;

    private Constants() {
    }
}
