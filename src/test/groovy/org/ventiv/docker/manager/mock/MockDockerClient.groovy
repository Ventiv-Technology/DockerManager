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
package org.ventiv.docker.manager.mock

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.DockerException
import com.github.dockerjava.api.command.AttachContainerCmd
import com.github.dockerjava.api.command.AuthCmd
import com.github.dockerjava.api.command.BuildImageCmd
import com.github.dockerjava.api.command.CommitCmd
import com.github.dockerjava.api.command.ContainerDiffCmd
import com.github.dockerjava.api.command.CopyFileFromContainerCmd
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.CreateImageCmd
import com.github.dockerjava.api.command.CreateImageResponse
import com.github.dockerjava.api.command.EventCallback
import com.github.dockerjava.api.command.EventsCmd
import com.github.dockerjava.api.command.ExecCreateCmd
import com.github.dockerjava.api.command.ExecCreateCmdResponse
import com.github.dockerjava.api.command.ExecStartCmd
import com.github.dockerjava.api.command.InfoCmd
import com.github.dockerjava.api.command.InspectContainerCmd
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.command.InspectExecCmd
import com.github.dockerjava.api.command.InspectExecResponse
import com.github.dockerjava.api.command.InspectImageCmd
import com.github.dockerjava.api.command.InspectImageResponse
import com.github.dockerjava.api.command.KillContainerCmd
import com.github.dockerjava.api.command.ListContainersCmd
import com.github.dockerjava.api.command.ListImagesCmd
import com.github.dockerjava.api.command.LogContainerCmd
import com.github.dockerjava.api.command.PauseContainerCmd
import com.github.dockerjava.api.command.PingCmd
import com.github.dockerjava.api.command.PullImageCmd
import com.github.dockerjava.api.command.PushImageCmd
import com.github.dockerjava.api.command.RemoveContainerCmd
import com.github.dockerjava.api.command.RemoveImageCmd
import com.github.dockerjava.api.command.RestartContainerCmd
import com.github.dockerjava.api.command.SaveImageCmd
import com.github.dockerjava.api.command.SearchImagesCmd
import com.github.dockerjava.api.command.StartContainerCmd
import com.github.dockerjava.api.command.StatsCallback;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.command.StopContainerCmd
import com.github.dockerjava.api.command.TagImageCmd
import com.github.dockerjava.api.command.TopContainerCmd
import com.github.dockerjava.api.command.TopContainerResponse
import com.github.dockerjava.api.command.UnpauseContainerCmd
import com.github.dockerjava.api.command.VersionCmd
import com.github.dockerjava.api.command.WaitContainerCmd
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.api.model.AuthResponse
import com.github.dockerjava.api.model.ChangeLog
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.Identifier
import com.github.dockerjava.api.model.Image
import com.github.dockerjava.api.model.Info
import com.github.dockerjava.api.model.SearchItem
import com.github.dockerjava.api.model.Version
import com.github.dockerjava.core.command.AttachContainerCmdImpl
import com.github.dockerjava.core.command.AuthCmdImpl
import com.github.dockerjava.core.command.BuildImageCmdImpl
import com.github.dockerjava.core.command.CommitCmdImpl
import com.github.dockerjava.core.command.ContainerDiffCmdImpl
import com.github.dockerjava.core.command.CopyFileFromContainerCmdImpl
import com.github.dockerjava.core.command.CreateContainerCmdImpl
import com.github.dockerjava.core.command.CreateImageCmdImpl
import com.github.dockerjava.core.command.EventsCmdImpl
import com.github.dockerjava.core.command.ExecCreateCmdImpl
import com.github.dockerjava.core.command.ExecStartCmdImpl
import com.github.dockerjava.core.command.InfoCmdImpl
import com.github.dockerjava.core.command.InspectContainerCmdImpl
import com.github.dockerjava.core.command.InspectExecCmdImpl
import com.github.dockerjava.core.command.InspectImageCmdImpl
import com.github.dockerjava.core.command.KillContainerCmdImpl
import com.github.dockerjava.core.command.ListContainersCmdImpl
import com.github.dockerjava.core.command.ListImagesCmdImpl
import com.github.dockerjava.core.command.LogContainerCmdImpl
import com.github.dockerjava.core.command.PauseContainerCmdImpl
import com.github.dockerjava.core.command.PingCmdImpl
import com.github.dockerjava.core.command.PullImageCmdImpl
import com.github.dockerjava.core.command.PushImageCmdImpl
import com.github.dockerjava.core.command.RemoveContainerCmdImpl
import com.github.dockerjava.core.command.RemoveImageCmdImpl
import com.github.dockerjava.core.command.RestartContainerCmdImpl
import com.github.dockerjava.core.command.SaveImageCmdImpl
import com.github.dockerjava.core.command.SearchImagesCmdImpl
import com.github.dockerjava.core.command.StartContainerCmdImpl
import com.github.dockerjava.core.command.StatsCmdImpl
import com.github.dockerjava.core.command.StopContainerCmdImpl
import com.github.dockerjava.core.command.TagImageCmdImpl
import com.github.dockerjava.core.command.TopContainerCmdImpl
import com.github.dockerjava.core.command.UnpauseContainerCmdImpl
import com.github.dockerjava.core.command.VersionCmdImpl
import com.github.dockerjava.core.command.WaitContainerCmdImpl
import spock.lang.Specification

import java.util.concurrent.ExecutorService

/**
 * Created by jcrygier on 4/13/15.
 */
class MockDockerClient extends Specification implements DockerClient {

    Map<Class<?>, Object> execResponses = [:]

    Map<Class<?>, Object> mockedInterfaces = [:]
    public <T> T getMockedInterface(Class<T> toMock) {
        if (mockedInterfaces == null)
            mockedInterfaces = [:]

        if (!mockedInterfaces.containsKey(toMock))
            mockedInterfaces.put(toMock, Mock(toMock));

        return mockedInterfaces[toMock];
    }

    @Override
    AuthConfig authConfig() throws DockerException {
        return new AuthConfig();
    }

    @Override
    AuthCmd authCmd() {
        AuthCmd.Exec mockExec = new AuthCmd.Exec() {
            @Override
            AuthResponse exec(AuthCmd command) {
                return execResponses[AuthCmd];
            }
        }
        return new AuthCmdImpl(mockExec, authConfig())
    }

    @Override
    InfoCmd infoCmd() {
        InfoCmd.Exec mockExec = new InfoCmd.Exec() {
            @Override
            Info exec(InfoCmd command) {
                return execResponses[InfoCmd];
            }
        }
        return new InfoCmdImpl(mockExec)
    }

    @Override
    PingCmd pingCmd() {
        PingCmd.Exec mockExec = new PingCmd.Exec() {
            @Override
            Void exec(PingCmd command) {
                return execResponses[PingCmd];
            }
        }
        return new PingCmdImpl(mockExec)
    }

    @Override
    VersionCmd versionCmd() {
        VersionCmd.Exec mockExec = new VersionCmd.Exec() {
            @Override
            Version exec(VersionCmd command) {
                return execResponses[VersionCmd];
            }
        }
        return new VersionCmdImpl(mockExec)
    }

    @Override
    PullImageCmd pullImageCmd(String repository) {
        PullImageCmd.Exec mockExec = new PullImageCmd.Exec() {
            @Override
            InputStream exec(PullImageCmd command) {
                return execResponses[PullImageCmd];
            }
        }
        return new PullImageCmdImpl(mockExec, authConfig(), repository)
    }

    @Override
    PushImageCmd pushImageCmd(String name) {
        PushImageCmd.Exec mockExec = new PushImageCmd.Exec() {
            @Override
            PushImageCmd.Response exec(PushImageCmd command) {
                return execResponses[PushImageCmd];
            }
        }
        return new PushImageCmdImpl(mockExec, name)
    }

    @Override
    PushImageCmd pushImageCmd(Identifier identifier) {
        PushImageCmd.Exec mockExec = new PushImageCmd.Exec() {
            @Override
            PushImageCmd.Response exec(PushImageCmd command) {
                return execResponses[PushImageCmd];
            }
        }
        return new PushImageCmdImpl(mockExec, identifier)
    }

    @Override
    CreateImageCmd createImageCmd(String repository, InputStream imageStream) {
        CreateImageCmd.Exec mockExec = new CreateImageCmd.Exec() {
            @Override
            CreateImageResponse exec(CreateImageCmd command) {
                return execResponses[CreateImageCmd];
            }
        }
        return new CreateImageCmdImpl(mockExec, repository, imageStream)
    }

    @Override
    SearchImagesCmd searchImagesCmd(String term) {
        SearchImagesCmd.Exec mockExec = new SearchImagesCmd.Exec() {
            @Override
            List<SearchItem> exec(SearchImagesCmd command) {
                return execResponses[SearchImagesCmd];
            }
        }
        return new SearchImagesCmdImpl(mockExec, term)
    }

    @Override
    RemoveImageCmd removeImageCmd(String imageId) {
        RemoveImageCmd.Exec mockExec = new RemoveImageCmd.Exec() {
            @Override
            Void exec(RemoveImageCmd command) {
                return execResponses[RemoveImageCmd];
            }
        }
        return new RemoveImageCmdImpl(mockExec, imageId)
    }

    @Override
    ListImagesCmd listImagesCmd() {
        ListImagesCmd.Exec mockExec = new ListImagesCmd.Exec() {
            @Override
            List<Image> exec(ListImagesCmd command) {
                return execResponses[ListImagesCmd];
            }
        }
        return new ListImagesCmdImpl(mockExec)
    }

    @Override
    InspectImageCmd inspectImageCmd(String imageId) {
        InspectImageCmd.Exec mockExec = new InspectImageCmd.Exec() {
            @Override
            InspectImageResponse exec(InspectImageCmd command) {
                return execResponses[InspectImageCmd];
            }
        }
        return new InspectImageCmdImpl(mockExec, imageId)
    }

    @Override
    SaveImageCmd saveImageCmd(String name) {
        SaveImageCmd.Exec mockExec = new SaveImageCmd.Exec() {
            @Override
            InputStream exec(SaveImageCmd command) {
                return execResponses[SaveImageCmd];
            }
        }
        return new SaveImageCmdImpl(mockExec, name)
    }

    @Override
    ListContainersCmd listContainersCmd() {
        ListContainersCmd.Exec mockExec = new ListContainersCmd.Exec() {
            @Override
            List<Container> exec(ListContainersCmd command) {
                return execResponses[ListContainersCmd];
            }
        }
        return new ListContainersCmdImpl(mockExec)
    }

    @Override
    CreateContainerCmd createContainerCmd(String image) {
        CreateContainerCmd.Exec mockExec = new CreateContainerCmd.Exec() {
            @Override
            CreateContainerResponse exec(CreateContainerCmd command) {
                return execResponses[CreateContainerCmd];
            }
        }
        return new CreateContainerCmdImpl(mockExec, image)
    }

    @Override
    StartContainerCmd startContainerCmd(String containerId) {
        StartContainerCmd.Exec mockExec = new StartContainerCmd.Exec() {
            @Override
            Void exec(StartContainerCmd command) {
                return execResponses[StartContainerCmd];
            }
        }
        return new StartContainerCmdImpl(mockExec, containerId)
    }

    @Override
    ExecCreateCmd execCreateCmd(String containerId) {
        ExecCreateCmd.Exec mockExec = new ExecCreateCmd.Exec() {
            @Override
            ExecCreateCmdResponse exec(ExecCreateCmd command) {
                return execResponses[ExecCreateCmd];
            }
        }
        return new ExecCreateCmdImpl(mockExec, containerId)
    }

    @Override
    InspectContainerCmd inspectContainerCmd(String containerId) {
        InspectContainerCmd.Exec mockExec = new InspectContainerCmd.Exec() {
            @Override
            InspectContainerResponse exec(InspectContainerCmd command) {
                return execResponses[InspectContainerCmd];
            }
        }
        return new InspectContainerCmdImpl(mockExec, containerId)
    }

    @Override
    RemoveContainerCmd removeContainerCmd(String containerId) {
        RemoveContainerCmd.Exec mockExec = new RemoveContainerCmd.Exec() {
            @Override
            Void exec(RemoveContainerCmd command) {
                return execResponses[RemoveContainerCmd];
            }
        }
        return new RemoveContainerCmdImpl(mockExec, containerId)
    }

    @Override
    WaitContainerCmd waitContainerCmd(String containerId) {
        WaitContainerCmd.Exec mockExec = new WaitContainerCmd.Exec() {
            @Override
            Integer exec(WaitContainerCmd command) {
                return execResponses[WaitContainerCmd];
            }
        }
        return new WaitContainerCmdImpl(mockExec, containerId)
    }

    @Override
    AttachContainerCmd attachContainerCmd(String containerId) {
        AttachContainerCmd.Exec mockExec = new AttachContainerCmd.Exec() {
            @Override
            InputStream exec(AttachContainerCmd command) {
                return execResponses[AttachContainerCmd];
            }
        }
        return new AttachContainerCmdImpl(mockExec, containerId)
    }

    @Override
    ExecStartCmd execStartCmd(String containerId) {
        ExecStartCmd.Exec mockExec = new ExecStartCmd.Exec() {
            @Override
            InputStream exec(ExecStartCmd command) {
                return execResponses[ExecStartCmd];
            }
        }
        return new ExecStartCmdImpl(mockExec, containerId)
    }

    @Override
    InspectExecCmd inspectExecCmd(String execId) {
        InspectExecCmd.Exec mockExec = new InspectExecCmd.Exec() {
            @Override
            InspectExecResponse exec(InspectExecCmd command) {
                return execResponses[InspectExecCmd];
            }
        }
        return new InspectExecCmdImpl(mockExec, execId)
    }

    @Override
    LogContainerCmd logContainerCmd(String containerId) {
        LogContainerCmd.Exec mockExec = new LogContainerCmd.Exec() {
            @Override
            InputStream exec(LogContainerCmd command) {
                return execResponses[LogContainerCmd];
            }
        }
        return new LogContainerCmdImpl(mockExec, containerId)
    }

    @Override
    CopyFileFromContainerCmd copyFileFromContainerCmd(String containerId, String resource) {
        CopyFileFromContainerCmd.Exec mockExec = new CopyFileFromContainerCmd.Exec() {
            @Override
            InputStream exec(CopyFileFromContainerCmd command) {
                return execResponses[CopyFileFromContainerCmd];
            }
        }
        return new CopyFileFromContainerCmdImpl(mockExec, containerId, resource)
    }

    @Override
    ContainerDiffCmd containerDiffCmd(String containerId) {
        ContainerDiffCmd.Exec mockExec = new ContainerDiffCmd.Exec() {
            @Override
            List<ChangeLog> exec(ContainerDiffCmd command) {
                return execResponses[ContainerDiffCmd];
            }
        }
        return new ContainerDiffCmdImpl(mockExec, containerId)
    }

    @Override
    StopContainerCmd stopContainerCmd(String containerId) {
        StopContainerCmd.Exec mockExec = new StopContainerCmd.Exec() {
            @Override
            Void exec(StopContainerCmd command) {
                return execResponses[StopContainerCmd];
            }
        }
        return new StopContainerCmdImpl(mockExec, containerId)
    }

    @Override
    KillContainerCmd killContainerCmd(String containerId) {
        KillContainerCmd.Exec mockExec = new KillContainerCmd.Exec() {
            @Override
            Void exec(KillContainerCmd command) {
                return execResponses[KillContainerCmd];
            }
        }
        return new KillContainerCmdImpl(mockExec, containerId)
    }

    @Override
    RestartContainerCmd restartContainerCmd(String containerId) {
        RestartContainerCmd.Exec mockExec = new RestartContainerCmd.Exec() {
            @Override
            Void exec(RestartContainerCmd command) {
                return execResponses[RestartContainerCmd];
            }
        }
        return new RestartContainerCmdImpl(mockExec, containerId)
    }

    @Override
    CommitCmd commitCmd(String containerId) {
        CommitCmd.Exec mockExec = new CommitCmd.Exec() {
            @Override
            String exec(CommitCmd command) {
                return execResponses[CommitCmd];
            }
        }
        return new CommitCmdImpl(mockExec, containerId)
    }

    @Override
    BuildImageCmd buildImageCmd() {
        BuildImageCmd.Exec mockExec = new BuildImageCmd.Exec() {
            @Override
            BuildImageCmd.Response exec(BuildImageCmd command) {
                return execResponses[BuildImageCmd];
            }
        }
        return new BuildImageCmdImpl(mockExec)
    }

    @Override
    BuildImageCmd buildImageCmd(File dockerFileOrFolder) {
        BuildImageCmd.Exec mockExec = new BuildImageCmd.Exec() {
            @Override
            BuildImageCmd.Response exec(BuildImageCmd command) {
                return execResponses[BuildImageCmd];
            }
        }
        return new BuildImageCmdImpl(mockExec, dockerFileOrFolder)
    }

    @Override
    BuildImageCmd buildImageCmd(InputStream tarInputStream) {
        BuildImageCmd.Exec mockExec = new BuildImageCmd.Exec() {
            @Override
            BuildImageCmd.Response exec(BuildImageCmd command) {
                return execResponses[BuildImageCmd];
            }
        }
        return new BuildImageCmdImpl(mockExec, tarInputStream)
    }

    @Override
    TopContainerCmd topContainerCmd(String containerId) {
        TopContainerCmd.Exec mockExec = new TopContainerCmd.Exec() {
            @Override
            TopContainerResponse exec(TopContainerCmd command) {
                return execResponses[TopContainerCmd];
            }
        }
        return new TopContainerCmdImpl(mockExec, containerId)
    }

    @Override
    TagImageCmd tagImageCmd(String imageId, String repository, String tag) {
        TagImageCmd.Exec mockExec = new TagImageCmd.Exec() {
            @Override
            Void exec(TagImageCmd command) {
                return execResponses[TagImageCmd];
            }
        }
        return new TagImageCmdImpl(mockExec, imageId, repository, tag)
    }

    @Override
    PauseContainerCmd pauseContainerCmd(String containerId) {
        PauseContainerCmd.Exec mockExec = new PauseContainerCmd.Exec() {
            @Override
            Void exec(PauseContainerCmd command) {
                return execResponses[PauseContainerCmd];
            }
        }
        return new PauseContainerCmdImpl(mockExec, containerId)
    }

    @Override
    UnpauseContainerCmd unpauseContainerCmd(String containerId) {
        UnpauseContainerCmd.Exec mockExec = new UnpauseContainerCmd.Exec() {
            @Override
            Void exec(UnpauseContainerCmd command) {
                return execResponses[UnpauseContainerCmd];
            }
        }
        return new UnpauseContainerCmdImpl(mockExec, containerId)
    }

    @Override
    EventsCmd eventsCmd(EventCallback eventCallback) {
        EventsCmd.Exec mockExec = new EventsCmd.Exec() {
            @Override
            ExecutorService exec(EventsCmd command) {
                return execResponses[EventsCmd];
            }
        }
        return new EventsCmdImpl(mockExec, eventCallback)
    }

    @Override
    void close() throws IOException {

    }

	@Override
	public StatsCmd statsCmd(StatsCallback statsCallback) {
		StatsCmd.Exec mockExec = new StatsCmd.Exec() {
            @Override
            ExecutorService exec(StatsCmd command) {
                return execResponses[StatsCmd];
            }
        }
        return new StatsCmdImpl(mockExec, statsCallback)
	}
}
