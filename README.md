# Edelweiss

The Edelweiss is a <a href="https://en.wikipedia.org/wiki/Snowflake_ID">Snowflake ID</a> generator. It has following 2
features:

- Automatic assign instance id of Snowflake
- Pause to generate ID if the system clock has been changed backward.

## ChangeLog

- 1.0.1 support spring boot 3.x

## Design

The Edelweiss introduce an interface <code>org.coderclan.edelweiss.InstanceIdAssigner</code>. It calls <code>
InstanceIdAssigner.assignAnInstanceId(String key, long expiringTime)</code> to get a Machine ID when system startup. The
Instance ID will expire at <code>expiringTime</code> to avoid exhaust all IDs. The Edelweiss will renew the Instance ID
before expiring periodically by calling <code>InstanceIdAssigner.renewInstanceId(int instanceId, String key, long
expiringTime)</code>.

The <code>org.coderclan.edelweiss.JbdcInstanceIdAssigner</code> is an implementation of <code>InstanceIdAssigner</code>.
It uses Relational Database to store the information of Machine ID assignation. More implementation for MongoDB,
Zookeeper, etc. could be added in the future.

## How to Use

The module edelweiss-demo demonstrate how to use the Edelweiss.

- Add maven dependency.

<pre>
        &lt;dependency>
            &lt;groupId>org.coderclan&lt;/groupId>
            &lt;artifactId>spring-boot-starter-edelweiss-jdbc&lt;/artifactId>
        &lt;/dependency>
</pre>

- Inject IdGenerator and use it to generate ID, just as <code>org.coderclan.edelweiss.demo.Demo</code> does.
- configure the expiringTime of machine ID, <code>coderclan.edelweiss.machineIdTtl=600</code>, unit: second.

## Limitation

The Snowflake ID algorithm is highly depending on System Clock. The System Clock should be accurate enough. Using a
time-server to synchronize the System Clock is strong recommended.

Different System Clock between nodes may cause two or more nodes use the same Machine ID. we could prevent this by
increasing
the System Clock accuracy or Increasing the life of machine ID(increasing the value of <code>
coderclan.edelweiss.machineIdTtl</code>). The max difference of System Clocks allowed by The Edelweiss is (<code>
coderclan.edelweiss.machineIdTtl</code> * <code>Constants.DEFAULT_INSTANCE_ID_TTL_MARGIN_RATE</code> + 1), unit: second.
the default configuration of the Edelweiss will allow 2 minutes of System Clock difference. 