package org.ventiv.docker.manager.model

import com.jayway.jsonpath.JsonPath

/**
 * Created by jcrygier on 3/4/15.
 */
class VersionSelectionConfiguration {

    String uri;
    String jsonPath;
    Collection<VersionSelectionFilterConfiguration> filters;

    public List<String> getPossibleVersions() {
        List<String> versions = [];

        if (getUri() && getJsonPath())
            versions = JsonPath.read(new URL(getUri()), getJsonPath())

        if (getFilters()) {
            getFilters().each { VersionSelectionFilterConfiguration filterConfiguration ->
                versions = versions.collect { String versionNumber ->
                    filterConfiguration.filter(versionNumber);
                }
            }
        }

        return versions.sort().reverse();
    }

}
