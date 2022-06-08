package org.coderclan.edelweiss;

import org.springframework.beans.factory.annotation.Autowired;

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
    @Autowired
    private DataSource dataSource;

    @PostConstruct
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
                for (int i = 0; i <= Constants.maxMachineId; i++) {
                    ps.setInt(1, i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
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
                    rs.updateLong("expiring_time", expiringTime + ttlMargin);
                    rs.updateRow();
                    return rs.getInt("instance_id");
                }
            }
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
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
            statement.setLong(1, expiringTime + ttlMargin);
            statement.setLong(2, instanceId);
            statement.setString(3, key);
            statement.setLong(4, currentTimeStampSeconds);
            int rows = statement.executeUpdate();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            if (rows == 1) {
                return instanceId;
            } else {
                return this.assignAnInstanceId(key, expiringTime);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return -1;
    }

    private long getCurrentTimeStampSeconds() {
        return Instant.now().getEpochSecond();
    }
}
