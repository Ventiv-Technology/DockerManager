package org.ventiv.docker.manager.config

import org.springframework.core.env.Environment
import org.ventiv.docker.manager.DockermanagerApplication

/**
 * Created by jcrygier on 3/2/15.
 */
enum PropertyTypes {

    Active_Tiers;

    private static Environment env = DockermanagerApplication.getApplicationContext().getBean(Environment);
    String propertyKey;

    private PropertyTypes() {
        this.propertyKey = this.name().toLowerCase().replaceAll("_", ".");
    }

    private PropertyTypes(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    String getValue() {
        return env.getProperty(getPropertyKey())
    }

    Integer getInteger() {
        return env.getProperty(getPropertyKey(), Integer);
    }

    List<String> getStringListValue() {
        return getListValue(String);
    }

    public <T> List<T> getListValue(Class<T> targetType) {
        List<T> answer = []
        int idx = 0;
        T lastValue = env.getProperty(propertyKey + "[${idx++}]", targetType);

        while (lastValue != null) {
            answer << lastValue;
            lastValue = env.getProperty(propertyKey + "[${idx++}]", targetType);
        }

        return answer;
    }

}