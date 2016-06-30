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
package org.ventiv.docker.manager.service

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerManagerConfiguration

import javax.annotation.PostConstruct
import javax.annotation.Resource
import java.util.concurrent.ScheduledFuture

/**
 * Created by jcrygier on 3/10/15.
 */
@Slf4j
@Service
@Order(Ordered.HIGHEST_PRECEDENCE)
@CompileStatic
class GitService implements Runnable {

    public static final int MAX_RETRIES = 5;

    @Resource DockerManagerConfiguration props;
    @Resource TaskScheduler taskScheduler;

    private Repository repository;
    private Git git;
    private CredentialsProvider credentialsProvider;
    private ScheduledFuture scheduledTask;
    private int pullErrorCount = 0;

    @PostConstruct
    void cloneGitIfNecessary() {
        if (props?.config?.git?.url) {
            File cloneLocation = new File(props.config.git.location);

            if (props.config.git.user)
                credentialsProvider = new UsernamePasswordCredentialsProvider(props.config.git.user, props.config.git.password);

            if (!cloneLocation.exists()) {
                log.info("Cloning config repository ${props.config.git.url} to ${props.config.git.location}")
                
                Git.cloneRepository()
                        .setURI(props.config.git.url)
                        .setDirectory(cloneLocation)
                        .setBare(false)
                        .setCredentialsProvider(credentialsProvider)
                        .setBranch(props.config.git.branch ?: 'master')
                        .call();
            }

            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            repository = builder.setGitDir(new File(cloneLocation, ".git"))
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();

            git = new Git(repository);

            if (props.config.git.refreshPeriod > 0)
                scheduledTask = taskScheduler.scheduleAtFixedRate(this, props.config.git.refreshPeriod);
        }
    }

    @Override
    void run() {
        if (git) {
            log.debug("Pulling latest configuration from Git: " + git.getRepository().getDirectory().getAbsolutePath());

            try {
                PullResult pullResult = git.pull().setCredentialsProvider(credentialsProvider).call();

                pullErrorCount = 0;
            } catch (Exception e) {
                log.error("Error pulling.  Current count: ${pullErrorCount + 1}", e)

                if (++pullErrorCount > MAX_RETRIES) {
                    log.error("Error count is too high, stopping pull")
                    scheduledTask.cancel(true);
                }
            }
        }
    }
}
