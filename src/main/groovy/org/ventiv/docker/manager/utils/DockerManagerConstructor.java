/**
 * Copyright (c) 2014 - 2017 Ventiv Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.ventiv.docker.manager.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ventiv.docker.manager.model.EnvironmentProperty;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created by jcrygier on 7/28/17.
 */
public class DockerManagerConstructor extends Constructor {
    private static final Logger log = LoggerFactory.getLogger(DockerManagerConstructor.class);

    public DockerManagerConstructor() {
        this.yamlClassConstructors.put(NodeId.mapping, new EnvironmentPropertyConstruct());
    }

    private class EnvironmentPropertyConstruct extends Constructor.ConstructMapping {
        @Override
        public Object construct(Node node) {
            if (node.getType() == EnvironmentProperty.class) {
                // Serialize it to a map, then use shared custom logic to transform
                node.setType(Map.class);
                return extractEnvironmentProperty((Map<String, Object>) super.construct(node));
            }

            return super.construct(node);
        }
    }

    public static EnvironmentProperty extractEnvironmentProperty(Map<String, Object> prop) {
        EnvironmentProperty answer = new EnvironmentProperty();

        prop.entrySet()
                .forEach(entry -> {
                    try {
                        Field f = EnvironmentProperty.class.getDeclaredField(entry.getKey());
                        f.setAccessible(true);
                        f.set(answer, entry.getValue());
                    } catch (NoSuchFieldException e) {
                        answer.setName(entry.getKey());
                        answer.setValue(entry.getValue() != null ? entry.getValue().toString() : null);
                    } catch (IllegalAccessException e) {
                        log.error("Unable to set value on EnvironmentProperty: " + entry);
                    }
                });

        // Detect if we have a template, convert to CachingGroovyShell
        if (answer != null && answer.getValue() != null && answer.getValue().contains("${")) {
            CachingGroovyShell cachingGroovyShell = new CachingGroovyShell('"' + answer.getValue() + '"');
            answer.setCachingGroovyShell(cachingGroovyShell);
        }

        return answer;
    }
}