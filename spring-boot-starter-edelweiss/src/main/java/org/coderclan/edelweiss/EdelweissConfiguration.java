package org.coderclan.edelweiss;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EdelweissConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public IdGenerator idGenerator() {
        SnowFlakeIdGenerator instance = SnowFlakeIdGenerator.instance();
        instance.setMachineId(1);
        return instance;
    }

    @Bean
    @ConditionalOnMissingBean
    public InstanceIdAssigner instanceIdAssigner() {
        return new RdbmsInstanceIdAssigner();
    }

//    @Bean
//    @ConditionalOnMissingBean
//    public IdentifierGenerator identifierGenerator() {
//        return new EdelweissIdentifierGenerator();
//    }
}
