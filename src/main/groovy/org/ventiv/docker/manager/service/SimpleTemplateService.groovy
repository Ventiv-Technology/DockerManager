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
        def filledTemplate = fillTemplate(template, bindings);

        if (backupFileExtension)
            FileUtils.copyFile(templateFile, new File(templateFile.getAbsolutePath() + backupFileExtension));

        templateFile.delete();
        templateFile.write(filledTemplate);
    }

    public def fillTemplate(String template, Map<String, Object> bindings) {
        def currentObject = bindings;

        String filledTemplate = template.replaceAll(compiledPattern) { List<String> match ->
            currentObject = bindings;

            try {
                match[1].split('\\.').each { String nextAccessor ->
                    if (nextAccessor.indexOf('()') > -1) {          // We're trying to call a no-arg method
                        String methodName = nextAccessor.substring(0, nextAccessor.indexOf('('))
                        currentObject = currentObject.getMetaClass().getMetaMethod(methodName).invoke(currentObject);
                    } else
                        currentObject = currentObject[nextAccessor]
                }
            } catch (Exception e) {
                if (props.template.ignoreMissingProperties)
                    currentObject = match[0]
                else
                    throw e;
            }

            return currentObject;
        }

        // If we've just come down to one variable, return the actual instance, so we keep the type
        if (filledTemplate == currentObject.toString())
            return currentObject;
        else
            return filledTemplate;
    }

}
