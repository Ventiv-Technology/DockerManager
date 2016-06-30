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
        Yaml yaml = new Yaml(representer)

        return yaml.loadAs(resource.getInputStream(), clazz);
    }

    public static <T> T loadAs(String yamlStr, Class<T> clazz) {
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(representer)

        return yaml.loadAs(yamlStr, clazz);
    }

}
