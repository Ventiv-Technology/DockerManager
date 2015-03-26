package org.ventiv.docker.manager.utils

import groovy.util.logging.Slf4j

/**
 * Created by jcrygier on 3/26/15.
 */
@Slf4j
class TimingUtils {

    public static <T> T time(String timerMessage, Closure<T> callback) {
        long startTime = System.currentTimeMillis();
        T returnValue = callback();
        log.trace("TIME [$timerMessage]: ${System.currentTimeMillis() - startTime} ms")

        return returnValue;
    }

}
