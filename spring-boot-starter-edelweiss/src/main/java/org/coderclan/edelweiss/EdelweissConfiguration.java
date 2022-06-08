package org.coderclan.edelweiss;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class EdelweissConfiguration {
    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    public SnowFlakeIdGenerator idGenerator() {
        SnowFlakeIdGenerator instance = SnowFlakeIdGenerator.instance();
        return instance;
    }

    @Bean
    @ConditionalOnMissingBean(InstanceIdAssigner.class)
    @ConditionalOnBean(DataSource.class)
    public JbdcInstanceIdAssigner instanceIdAssigner() {
        return new JbdcInstanceIdAssigner();
    }

    @Bean
    @ConditionalOnBean({SnowFlakeIdGenerator.class, InstanceIdAssigner.class})
    public SnowFlakeInstanceIdRenewer snowFlakeInstanceIdRenewer(SnowFlakeIdGenerator idGenerator, InstanceIdAssigner instanceIdAssigner) {
        return new SnowFlakeInstanceIdRenewer(idGenerator, instanceIdAssigner);
    }
//    @Bean
//    @ConditionalOnMissingBean
//    public IdentifierGenerator identifierGenerator() {
//        return new EdelweissIdentifierGenerator();
//    }
}
