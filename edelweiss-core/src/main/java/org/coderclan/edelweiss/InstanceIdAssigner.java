package org.coderclan.edelweiss;

/**
 * Assign an ID to instance.
 * A new instance can generate a random key to call {@link #assignAnInstanceId(String, long)} to get an ID.
 * The ID will be expired after a period of time. The instance can only continue to use the ID by renew it (call {@link #renewInstanceId(int, String, long)}).
 *
 * @author aray(dot)chou(dot)cn(at)gmail(dot)com
 */
public interface InstanceIdAssigner {
    /**
     * @param key          a Random key associated with the ID. It is needed when renewing the ID. {@link #renewInstanceId(int, String, long)}
     * @param expiringTime timestamp, unit: second. the key will become invalid after this time.
     * @return A new instance ID associating with the key. return -1 if failed to assign an instanceId
     */
    int assignAnInstanceId(String key, long expiringTime);

    /**
     * Renew the instanceId. make it valid before expiringTime.
     * If the instanceId is already expired, it will create a new instanceId,
     * and associate the new instanceId with the key, and return the newly created instanceId.
     *
     * @param instanceId   the instance ID
     * @param key          the Key associated with Instance ID. (The parameter of {@link #assignAnInstanceId(String, long)})
     * @param expiringTime timestamp, unit: second. the key will become invalid after this time.
     * @return the instanceId if successfully renewed, or a new instanceId, or -1 if failed to assign an instanceId
     */
    int renewInstanceId(int instanceId, String key, long expiringTime);


}
