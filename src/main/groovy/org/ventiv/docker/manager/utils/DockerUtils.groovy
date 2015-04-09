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

import org.joda.time.DateTime
import org.joda.time.Period
import org.ocpsoft.prettytime.PrettyTime

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by jcrygier on 3/16/15.
 */
class DockerUtils {

    public static final Pattern UP_PATTERN = ~/Up (\d*) (\w*)/
    public static final Pattern EXITED_PATTERN = ~/Exited \((-?\d*)\) (\d*) (\w*) ago/
    public static final Pattern APPROX_PATTERN = ~/Up About an? (\w*)/

    public static Date convertDockerDate(String dte) {
        return new DateTime(dte).toDate();
    }

    public static String getStatusTime(Date statusDate, Date now = new Date()) {
        return new PrettyTime(now).format(statusDate);
    }

    public static String getStatusTime(String dte) {
        return getStatusTime(convertDockerDate(dte));
    }

    public static Date convertPsStatusToDate(String psStatus, Date now = new Date()) {
        Matcher upMatcher = UP_PATTERN.matcher(psStatus)
        Matcher exitedMatcher = EXITED_PATTERN.matcher(psStatus)
        Matcher approxMatcher = APPROX_PATTERN.matcher(psStatus)

        DateTime dateTime = new DateTime(now)
        Integer scalar;
        String periodType;

        if (upMatcher.find()) {
            scalar = Integer.parseInt(upMatcher[0][1]);
            periodType = upMatcher[0][2].toLowerCase();
        } else if (exitedMatcher.find()) {
            scalar = Integer.parseInt(exitedMatcher[0][2]);
            periodType = exitedMatcher[0][3].toLowerCase();
        } else if (approxMatcher.find()) {
            scalar = 1;
            periodType = approxMatcher[0][1].toLowerCase() + 's';
        }

        return dateTime.minus(Period."$periodType"(scalar)).toDate();
    }

}
