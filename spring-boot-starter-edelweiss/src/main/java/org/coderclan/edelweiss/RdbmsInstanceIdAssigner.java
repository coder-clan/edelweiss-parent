package org.coderclan.edelweiss;

import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;

public class RdbmsInstanceIdAssigner implements InstanceIdAssigner {
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

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public int assignAnInstanceId(String key, long expiringTime) {
        final String sql = "SELECT instance_id,instance_key,expiring_time FROM SYS_EDELWEISS_INSTANCE where expiring_time<? limit 1 for update";
        try (final Connection connection = this.dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        ) {
            statement.setLong(1, getCurrentTimeStampSeconds() - 10);
            try (ResultSet rs = statement.executeQuery();) {
                if (rs.next()) {
                    rs.updateString("instance_key", key);
                    rs.updateLong("expiring_time", expiringTime);
                    return rs.getInt("instance_id");
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return -1;
    }

    @Override
    public int renewInstanceId(int instanceId, String key, long expiringTime) {
        final String sql = "update SYS_EDELWEISS_INSTANCE set expiring_time=? where instance_id=? and instance_key=? and expiring_time>?";
        try (final Connection connection = this.dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql);) {
            statement.setLong(1, expiringTime);
            statement.setLong(2, instanceId);
            statement.setString(3, key);
            statement.setLong(4, getCurrentTimeStampSeconds() + 10);
            int rows = statement.executeUpdate();
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
