/*
 * Copyright (c) 2014 - 2016 Ventiv Technology
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
package org.ventiv.docker.manager.utils

import org.springframework.core.io.Resource
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer

/**
 * Created by jcrygier on 6/30/16.
 */
class YamlUtils {

    public static <T> T loadAs(Resource resource, Class<T> clazz) {
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(new DockerManagerConstructor(), representer)

        return yaml.loadAs(resource.getInputStream(), clazz);
    }

    public static <T> T loadAs(String yamlStr, Class<T> clazz) {
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(new DockerManagerConstructor(), representer)

        return yaml.loadAs(yamlStr, clazz);
    }
}
