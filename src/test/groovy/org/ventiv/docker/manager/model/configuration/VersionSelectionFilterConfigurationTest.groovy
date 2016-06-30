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
package org.ventiv.docker.manager.model.configuration

import spock.lang.Specification

/**
 * Created by jcrygier on 6/29/16.
 */
class VersionSelectionFilterConfigurationTest extends Specification {

    def 'can filter w/ groovy'(String toFilter, String expected) {
        setup:
        def toTest = new VersionSelectionFilterConfiguration([groovy: 'versionNumber.substring(1)'])

        expect:
        toTest.filter(toFilter) == expected

        where:
        toFilter            | expected
        '/1234'             | '1234'
        '/987654321'        | '987654321'
    }

    def 'can filter w/o groovy'(String toFilter, String expected) {
        setup:
        def toTest = new VersionSelectionFilterConfiguration()

        expect:
        toTest.filter(toFilter) == expected

        where:
        toFilter            | expected
        '/1234'             | '/1234'
        '/987654321'        | '/987654321'
    }

    def 'can filter complex w/ groovy'(String toFilter, String expected) {
        setup:
        def toTest = new VersionSelectionFilterConfiguration([groovy: 'versionNumber.split("/").reverse().join()'])

        expect:
        toTest.filter(toFilter) == expected

        where:
        toFilter                    | expected
        '/1234/5678/9'              | '956781234'
        '/9/8765/4321'              | '432187659'
    }

}
