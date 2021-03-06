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
package org.ventiv.docker.manager.model.configuration

import com.jayway.jsonpath.JsonPath
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Created by jcrygier on 3/4/15.
 */
@CompileStatic
@Slf4j
class VersionSelectionConfiguration extends RestCallConfiguration {

    String jsonPath;
    Collection<VersionSelectionFilterConfiguration> filters;

    public List<String> getPossibleVersions(Map<String, Object> variables) {
        List<String> versions = [];

        if (getUri() && getJsonPath()) {
            Object response = makeCall(variables);
            versions = (List<String>) JsonPath.read(response, getJsonPath());
        }

        if (getFilters()) {
            getFilters().each { VersionSelectionFilterConfiguration filterConfiguration ->
                versions = versions.collect { String versionNumber ->
                    filterConfiguration.filter(versionNumber);
                }
            }
        }

        // Query parameter filtering
        if (variables.query) {
            versions = ((List<String>)versions).findAll { it.contains(variables.query.toString()) }.toList()
        }

        return versions;
    }

}
