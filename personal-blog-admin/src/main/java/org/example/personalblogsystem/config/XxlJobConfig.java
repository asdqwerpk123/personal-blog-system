package org.example.personalblogsystem.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@ConditionalOnProperty(prefix = "xxl.job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XxlJobConfig {

    private final XxlJobProperties xxlJobProperties;

    public XxlJobConfig(XxlJobProperties xxlJobProperties) {
        this.xxlJobProperties = xxlJobProperties;
    }

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(xxlJobProperties.getAdmin().getAddresses());
        executor.setAccessToken(xxlJobProperties.getAccessToken());
        executor.setAppname(xxlJobProperties.getExecutor().getAppname());
        executor.setAddress(xxlJobProperties.getExecutor().getAddress());
        executor.setIp(xxlJobProperties.getExecutor().getIp());
        executor.setPort(xxlJobProperties.getExecutor().getPort());
        executor.setLogPath(xxlJobProperties.getExecutor().getLogpath());
        executor.setLogRetentionDays(xxlJobProperties.getExecutor().getLogretentiondays());
        return executor;
    }

    @Data
    @Component
    @ConfigurationProperties(prefix = "xxl.job")
    public static class XxlJobProperties {

        private Boolean enabled = true;

        private Admin admin = new Admin();

        private String accessToken;

        private Executor executor = new Executor();

        @Data
        public static class Admin {
            private String addresses;
        }

        @Data
        public static class Executor {
            private String appname;
            private String address;
            private String ip;
            private Integer port;
            private String logpath;
            private Integer logretentiondays;
        }
    }
}
