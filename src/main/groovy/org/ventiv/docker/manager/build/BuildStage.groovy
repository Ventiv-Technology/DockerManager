package org.ventiv.docker.manager.build

/**
 * Created by jcrygier on 3/11/15.
 */
interface BuildStage {

    public void doBuild(Map<String, String> buildSettings, Map<String, Object> buildContext);

}
