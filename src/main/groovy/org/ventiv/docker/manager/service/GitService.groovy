package org.ventiv.docker.manager.service

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.config.DockerServiceConfiguration

import javax.annotation.PostConstruct
import javax.annotation.Resource
import java.util.concurrent.ScheduledFuture

/**
 * Created by jcrygier on 3/10/15.
 */
@Slf4j
@Service
@CompileStatic
class GitService implements Runnable {

    public static final int MAX_RETRIES = 5;

    @Resource DockerManagerConfiguration props;
    @Resource TaskScheduler taskScheduler;
    @Resource DockerServiceConfiguration dockerServiceConfiguration;

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
                log.debug("Cloning config repository ${props.config.git.url} to ${props.config.git.location}")
                
                Git.cloneRepository()
                        .setURI(props.config.git.url)
                        .setDirectory(cloneLocation)
                        .setBare(false)
                        .setCredentialsProvider(credentialsProvider)
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

                if (pullResult.getMergeResult().getMergeStatus() == MergeResult.MergeStatus.FAST_FORWARD) {
                    dockerServiceConfiguration.readConfiguration();
                }

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
