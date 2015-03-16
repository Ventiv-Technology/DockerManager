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
package org.ventiv.docker.manager.utils

import spock.lang.Specification

/**
 * Created by jcrygier on 3/16/15.
 */
class DockerUtilsTest extends Specification {

    def "can convert string dates"() {
        when:
        Date dte = DockerUtils.convertDockerDate("2015-03-06T20:52:13.03291945Z");
        Calendar cal = new GregorianCalendar()
        cal.setTime(dte);
        cal.setTimeZone(TimeZone.getTimeZone("US/Eastern"))

        then:
        cal.get(Calendar.YEAR) == 2015
        cal.get(Calendar.MONTH) == 2
        cal.get(Calendar.DAY_OF_MONTH) == 6
        cal.get(Calendar.HOUR_OF_DAY) == 15
        cal.get(Calendar.MINUTE) == 52
        cal.get(Calendar.SECOND) == 13
        cal.get(Calendar.MILLISECOND) == 32
    }

    def "can convert to 'docker ps' status"(String date, String expected) {
        expect:
        Date referenceDate = DockerUtils.convertDockerDate("2015-03-16T23:32:00.03291945Z");
        DockerUtils.getStatusTime(DockerUtils.convertDockerDate(date), referenceDate) == expected

        where:
        date                                | expected
        "2015-03-06T20:52:13.03291945Z"     | "1 week ago"
        "2015-03-16T20:52:13.03291945Z"     | "3 hours ago"
        "2015-03-16T23:23:13.03291945Z"     | "9 minutes ago"
    }

}
