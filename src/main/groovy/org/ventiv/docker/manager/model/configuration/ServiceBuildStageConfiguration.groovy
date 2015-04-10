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

import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.build.AsyncBuildStage
import org.ventiv.docker.manager.build.BuildContext
import org.ventiv.docker.manager.build.BuildStage

/**
 * Created by jcrygier on 3/11/15.
 */
class ServiceBuildStageConfiguration {

    String type;
    Map<String, String> settings;

    public Promise<Object, Exception, String> execute(BuildContext buildContext) {
        def buildStage = DockerManagerApplication.getApplicationContext().getBean(type);

        if (buildStage instanceof BuildStage) {
            DeferredObject<Object, Exception, String> deferred = new DeferredObject();

            buildStage.doBuild(settings, buildContext);
            deferred.resolve("finished");

            return deferred.promise();
        } else if (buildStage instanceof AsyncBuildStage) {
            return buildStage.doBuild(settings, buildContext);
        }
    }

}
