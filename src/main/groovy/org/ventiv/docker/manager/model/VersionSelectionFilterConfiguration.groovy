package org.ventiv.docker.manager.model

/**
 * Created by jcrygier on 3/4/15.
 */
class VersionSelectionFilterConfiguration {

    String groovy;

    String filter(String versionNumber) {
        // Groovy Filter
        if (getGroovy()) {
            return Eval.me('versionNumber', versionNumber, getGroovy())
        }

        return versionNumber;
    }
}
