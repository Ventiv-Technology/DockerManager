package org.ventiv.docker.manager.model

import groovy.util.logging.Slf4j
import org.jdeferred.DoneCallback
import org.jdeferred.FailCallback
import org.jdeferred.ProgressCallback
import org.jdeferred.Promise
import org.ventiv.docker.manager.build.BuildContext

/**
 * Created by jcrygier on 3/13/15.
 */
@Slf4j
class ServiceBuildInfo {

    String serviceName;
    Promise<BuildContext, Exception, String> promise;
    List<String> allStatus = [];
    BuildApplicationInfo buildApplicationInfo;

    public ServiceBuildInfo(BuildApplicationInfo buildApplicationInfo, String serviceName) {
        setBuildApplicationInfo(buildApplicationInfo);
        setServiceName(serviceName);
    }

    public void setPromise(Promise<BuildContext, Exception, String> promise) {
        this.promise = promise;

        // Listen to progress, and keep it in this bean
        promise.progress({ String progress ->
            log.debug("Build Progress for $serviceName: $progress");

            allStatus << progress;
            getBuildApplicationInfo().publishBuildEvent();
        } as ProgressCallback<String>)

        promise.done({ BuildContext buildContext ->
            log.debug("Finished Building $serviceName");

            allStatus << "Finished Build..."
            getBuildApplicationInfo().publishBuildEvent();
            getBuildApplicationInfo().serviceBuildSuccessful(this, buildContext);
        } as DoneCallback<BuildContext>)

        promise.fail({ Exception e ->
            log.error("Error building $serviceName", e)

            allStatus << "Error building $serviceName: ${e.getMessage()}";
            getBuildApplicationInfo().publishBuildEvent();
        } as FailCallback<Exception>)
    }

    public String getLastStatus() {
        return allStatus != null && allStatus.size() > 0 ? allStatus.last() : "";
    }

    public boolean isBuilding() {
        return promise != null && promise.isPending();
    }

    public boolean isSuccess() {
        return promise != null && promise.isResolved();
    }

    public boolean isFailed() {
        return promise != null && promise.isRejected();
    }

    public boolean isBuildNeeded() {
        return promise != null;
    }

}
