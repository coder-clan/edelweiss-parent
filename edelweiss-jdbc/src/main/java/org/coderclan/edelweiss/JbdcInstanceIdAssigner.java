package org.coderclan.edelweiss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;

/**
 * An InstanceIdAssigner who use Rational Database to store the assignation data.
 *
 * @author aray(dot)chou(dot)cn(at)gmail(dot)com
 */
public class JbdcInstanceIdAssigner implements InstanceIdAssigner {
    private static final Logger log = LoggerFactory.getLogger(JbdcInstanceIdAssigner.class);

    private final DataSource dataSource;

    public JbdcInstanceIdAssigner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    @jakarta.annotation.PostConstruct
    public void init() {
        final String createTableSQl = "CREATE TABLE sys_edelweiss_instance (\n" +
                "\tinstance_id INT NOT NULL,\n" +
                "\tinstance_key VARCHAR(127) NOT NULL,\n" +
                "\texpiring_time BIGINT NOT NULL,\n" +
                "\tPRIMARY KEY (instance_id)\n" +
                ")";
        final String insertDataSql = "insert into sys_edelweiss_instance (instance_id, instance_key, expiring_time) values (?,'NOT_USED_YET',0)";


        try (final Connection connection = this.dataSource.getConnection();
             final Statement statement = connection.createStatement();) {

            // to create table
            // if table exists, it will fail and don't execute following block to insert data.
            statement.execute(createTableSQl);

            // insert initial data
            try (final PreparedStatement ps = connection.prepareStatement(insertDataSql);) {
                for (int i = 0; i <= Constants.MAX_MACHINE_ID; i++) {
                    ps.setInt(1, i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            log.debug("", e);
        }
    }

    @Override
    public int assignAnInstanceId(String key, long expiringTime) {
        final String sql = "SELECT instance_id,instance_key,expiring_time FROM sys_edelweiss_instance where expiring_time<? limit 1 for update";
        try (final Connection connection = this.dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        ) {
            long currentTimeStampSeconds = getCurrentTimeStampSeconds();
            int ttlMargin = getTtlMargin(expiringTime, currentTimeStampSeconds);
            statement.setLong(1, currentTimeStampSeconds);
            try (ResultSet rs = statement.executeQuery();) {
                if (rs.next()) {
                    rs.updateString("instance_key", key);
                    final long expiringTimeWithMargin = expiringTime + ttlMargin;
                    rs.updateLong("expiring_time", expiringTimeWithMargin);
                    rs.updateRow();
                    if (!connection.getAutoCommit()) {
                        connection.commit();
                    }

                    final int instanceId = rs.getInt("instance_id");
                    log.info("Assigned a new snowflake instance ID. "
                            + "Snowflake Instance ID: {}, Instance Key: {}, Expiring Time (with Margin): {}", instanceId, key, expiringTimeWithMargin);
                    return instanceId;
                } else {
                    log.error("Snowflake Instance IDs exhausted!");
                    return -1;
                }
            }
        } catch (SQLException e) {
            log.error("Failed to assign snowflake instance ID!", e);
        }
        return -1;
    }

    /**
     * The system clock won't be accurate. We could add some margin to the TTL (the expiringTime) to make sure the instanceId is abandoned by the previous owner.
     * The previous owner should give up the instanceId before the expiringTime, and new owner can only obtain the instanceId after (expiringTime + ttlMargin)
     *
     * @param expiringTime
     * @param currentTimeStampSeconds
     * @return
     */
    private int getTtlMargin(long expiringTime, long currentTimeStampSeconds) {
        if (expiringTime < currentTimeStampSeconds) {
            throw new IllegalArgumentException("The expiringTime is a time point in the past.");
        }
        return ((int) ((expiringTime - currentTimeStampSeconds) * Constants.DEFAULT_INSTANCE_ID_TTL_MARGIN_RATE))
                // make the margin greater than 1 second.
                + 1;
    }

    @Override
    public int renewInstanceId(int instanceId, String key, long expiringTime) {
        final String sql = "update sys_edelweiss_instance set expiring_time=? where instance_id=? and instance_key=? and expiring_time>?";
        try (final Connection connection = this.dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql);) {
            long currentTimeStampSeconds = getCurrentTimeStampSeconds();
            int ttlMargin = getTtlMargin(expiringTime, currentTimeStampSeconds);
            final long expiringTimeWithMargin = expiringTime + ttlMargin;
            statement.setLong(1, expiringTimeWithMargin);
            statement.setLong(2, instanceId);
            statement.setString(3, key);
            statement.setLong(4, currentTimeStampSeconds);
            int rows = statement.executeUpdate();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            if (rows == 1) {
                log.debug("Renewed snowflake instance ID successfully. "
                        + "Snowflake Instance ID: {}, Instance Key: {}, Expiring Time (with Margin): {}", instanceId, key, expiringTimeWithMargin);
                return instanceId;
            } else {
                log.info("Failed to renewed snowflake instance ID, trying to assign a new one."
                        + "Snowflake Instance ID: {}, Instance Key: {}, Expiring Time (with Margin): {}", instanceId, key, expiringTimeWithMargin);
                return this.assignAnInstanceId(key, expiringTime);
            }
        } catch (SQLException e) {
            log.error("Failed to renewed snowflake instance ID!", e);
        }
        return -1;
    }

    private long getCurrentTimeStampSeconds() {
        return Instant.now().getEpochSecond();
    }
}
