/*
 * Copyright (c) 2014 - 2015 Ventiv Technology
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
package org.ventiv.docker.manager.service

import org.apache.commons.io.FileUtils
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerManagerConfiguration

import javax.annotation.PostConstruct
import javax.annotation.Resource
import java.util.regex.Pattern

/**
 * A simple template service that allows for variables to be prefix/postfixed by any given character combination.  Will
 * also allow for ignoring properties that are missing, thus not doing variable replacement.
 *
 * Uses Regular Expressions for efficiency (i.e. no class generation / permgen issues)
 */
@Service
class SimpleTemplateService {

    @Resource DockerManagerConfiguration props;

    private Pattern compiledPattern;

    @PostConstruct
    public void createPatternMatcher() {
        compiledPattern = Pattern.compile("${props.template.startToken.replaceAll('\\{', '\\\\{')}([^${props.template.endToken}]*)${props.template.endToken}");
    }

    public void fillTemplate(File templateFile, Map<String, Object> bindings, String backupFileExtension = null) {
        String template = templateFile.getText();
        String filledTemplate = fillTemplate(template, bindings);

        if (backupFileExtension)
            FileUtils.copyFile(templateFile, new File(templateFile.getAbsolutePath() + backupFileExtension));

        templateFile.delete();
        templateFile.write(filledTemplate);
    }

    public String fillTemplate(String template, Map<String, Object> bindings) {
        template.replaceAll(compiledPattern) { List<String> match ->
            def currentObject = bindings;

            try {
                match[1].split('\\.').each { String nextAccessor ->
                    currentObject = currentObject[nextAccessor]
                }
            } catch (MissingPropertyException e) {
                if (props.template.ignoreMissingProperties)
                    currentObject = match[0]
                else
                    throw e;
            }

            return currentObject;
        }
    }

}
