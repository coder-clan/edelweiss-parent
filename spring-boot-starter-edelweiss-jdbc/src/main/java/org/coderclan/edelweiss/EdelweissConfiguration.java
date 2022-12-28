package org.coderclan.edelweiss;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author aray(dot)chou(dot)cn(at)gmail(dot)com
 */
@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class EdelweissConfiguration {
    @Value("${coderclan.edelweiss.machineIdTtl:600}")
    private int machineIdTtl;

    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    public SnowFlakeIdGenerator idGenerator() {
        return SnowFlakeIdGenerator.instance();
    }

    @Bean
    @ConditionalOnMissingBean(InstanceIdAssigner.class)
    @ConditionalOnBean(DataSource.class)
    public JbdcInstanceIdAssigner instanceIdAssigner(DataSource dataSource) {
        return new JbdcInstanceIdAssigner(dataSource);
    }

    @Bean
    @ConditionalOnBean({SnowFlakeIdGenerator.class, InstanceIdAssigner.class})
    public SnowFlakeInstanceIdRenewer snowFlakeInstanceIdRenewer(SnowFlakeIdGenerator idGenerator, InstanceIdAssigner instanceIdAssigner) {
        return new SnowFlakeInstanceIdRenewer(idGenerator, instanceIdAssigner, machineIdTtl);
    }
}
