package org.example.personalblogsystem.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@Profile("test")
public class TestProfileDatabaseEnvValidationConfig implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final String BLOG_DB_TEST_URL = "BLOG_DB_TEST_URL";
    private static final String BLOG_DB_TEST_USERNAME = "BLOG_DB_TEST_USERNAME";
    private static final String BLOG_DB_TEST_PASSWORD = "BLOG_DB_TEST_PASSWORD";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<String> missingVariables = new ArrayList<>();
        addIfMissing(missingVariables, BLOG_DB_TEST_URL);
        addIfMissing(missingVariables, BLOG_DB_TEST_USERNAME);
        addIfMissing(missingVariables, BLOG_DB_TEST_PASSWORD);
        if (!missingVariables.isEmpty()) {
            throw new BeanCreationException(
                    "Missing required test database environment variables: " + String.join(", ", missingVariables));
        }
    }

    private void addIfMissing(List<String> missingVariables, String propertyName) {
        if (!StringUtils.hasText(environment.getProperty(propertyName))) {
            missingVariables.add(propertyName);
        }
    }
}
