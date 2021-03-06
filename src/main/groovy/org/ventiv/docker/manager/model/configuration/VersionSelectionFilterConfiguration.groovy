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

import org.ventiv.docker.manager.utils.CachingGroovyShell


/**
 * Created by jcrygier on 3/4/15.
 */
class VersionSelectionFilterConfiguration {

    String groovy;
    private CachingGroovyShell sh;

    String filter(String versionNumber) {
        // Groovy Filter
        if (getGroovy()) {
            return getCachingGroovyShell().eval([versionNumber: versionNumber]);
        }

        return versionNumber;
    }

    private CachingGroovyShell getCachingGroovyShell() {
        if (sh == null)
            sh = new CachingGroovyShell(getGroovy())

        return sh;
    }

}
