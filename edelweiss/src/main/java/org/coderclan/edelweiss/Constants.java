package org.coderclan.edelweiss;

public final class Constants {
    public final static long EPOCH = 1654094343806L;
    public final static int timestampShiftBits = 10 + 12; // 10 bits for machineId, 12 bits for per-machine sequence number;
    public final static int machineIdShiftBits = 12; // 12 bits for per-machine sequence number;
    public final static long maxTimestamp = (1L << 41) - 1;
    public final static int maxMachineId = (1 << 10) - 1;
    public final static int maxSequenceValue = (1 << 12) - 1;
    /**
     * The instance ID should be designed to live a bit logger than it expected to solve the problems: un-accurate system clock.
     *
     */
    public final static double DEFAULT_INSTANCE_ID_TTL_MARGIN_RATE = 0.2;

    private Constants() {
    }
}
