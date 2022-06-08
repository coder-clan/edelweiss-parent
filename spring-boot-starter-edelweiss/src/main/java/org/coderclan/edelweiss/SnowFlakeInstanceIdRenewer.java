package org.coderclan.edelweiss;

import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author aray(dot)chou(dot)cn(at)gmail(dot)com
 */
public class SnowFlakeInstanceIdRenewer {

    private final SnowFlakeIdGenerator idGenerator;

    private final InstanceIdAssigner instanceIdAssigner;
    private final String key = UUID.randomUUID().toString();
    private volatile int machineId = -1;
    private final static int ttl = 10;
    private final ScheduledExecutorService executorService = Executors
            .newSingleThreadScheduledExecutor();


    public SnowFlakeInstanceIdRenewer(SnowFlakeIdGenerator idGenerator, InstanceIdAssigner instanceIdAssigner) {
        this.idGenerator = idGenerator;
        this.instanceIdAssigner = instanceIdAssigner;

        long expiringTime = getExpiringTime();
        int machineId = instanceIdAssigner.assignAnInstanceId(key, expiringTime);
        if (machineId >= 0) {
            this.idGenerator.setMachineId(machineId, expiringTime);
            this.machineId = machineId;
        }
    }

    private long getExpiringTime() {
        return Instant.now().getEpochSecond() + ttl;
    }

    @Scheduled(fixedRate = ttl / 2 * 1000)
    void renew() {
        long expiringTime = getExpiringTime();
        int machineId = instanceIdAssigner.renewInstanceId(this.machineId, key, expiringTime);
        if (machineId >= 0) {
            this.idGenerator.setMachineId(machineId, expiringTime);
            this.machineId = machineId;
        }
    }

}
