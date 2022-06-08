package org.coderclan.edelweiss;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author aray(dot)chou(dot)cn(at)gmail(dot)com
 */
public class SnowFlakeInstanceIdRenewer {

    private final SnowFlakeIdGenerator idGenerator;

    private final InstanceIdAssigner instanceIdAssigner;
    private final String key = UUID.randomUUID().toString();
    private volatile int machineId = -1;
    private final int machineIdTtl;
    private final ScheduledExecutorService executorService = Executors
            .newSingleThreadScheduledExecutor();


    public SnowFlakeInstanceIdRenewer(SnowFlakeIdGenerator idGenerator, InstanceIdAssigner instanceIdAssigner, int machineIdTtl) {
        this.idGenerator = idGenerator;
        this.instanceIdAssigner = instanceIdAssigner;
        this.machineIdTtl = machineIdTtl;

        this.renew();

        executorService.scheduleAtFixedRate(this::renew, machineIdTtl / 2, machineIdTtl / 2, TimeUnit.SECONDS);
    }


    private long getExpiringTime() {
        return Instant.now().getEpochSecond() + machineIdTtl;
    }


    private void renew() {
        long expiringTime = getExpiringTime();
        int machineId = instanceIdAssigner.renewInstanceId(this.machineId, key, expiringTime);
        if (machineId >= 0) {
            this.idGenerator.setMachineId(machineId, expiringTime);
            this.machineId = machineId;
        }
    }

}
