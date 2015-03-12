package org.ventiv.docker.manager.build

import org.jdeferred.Promise

/**
 * Created by jcrygier on 3/12/15.
 */
interface AsyncBuildStage {

    public Promise doBuild(Map<String, String> buildSettings, Map<String, Object> buildContext);

}