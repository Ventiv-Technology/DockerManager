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
