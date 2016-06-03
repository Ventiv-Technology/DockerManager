/**
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
package org.ventiv.docker.manager.dockerjava;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dockerjava.api.command.DockerCmdSyncExec;
import com.github.dockerjava.api.command.SyncDockerCmd;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

/**
 * Created by jcrygier on 5/1/15.
 */
public interface ImageHistoryCmd extends SyncDockerCmd<List<ImageHistoryCmd.ImageHistory>> {

    @Override
    public List<ImageHistoryCmd.ImageHistory> exec();

    public String getImageName();

    public ImageHistoryCmd withImageName(String imageName);

    public static interface Exec extends DockerCmdSyncExec<ImageHistoryCmd, List<ImageHistory>> {
    }

    public static final class ImageHistory {
        @JsonProperty("Created")
        private long created;

        @JsonProperty("CreatedBy")
        private String createdBy;

        @JsonProperty("Id")
        private String id;

        @JsonProperty("Size")
        private long size;

        @JsonProperty("Tags")
        private String[] tags;

        @JsonProperty("Comment")
        private String comment;

        public long getCreated() {
            return created;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public String getId() {
            return id;
        }

        public long getSize() {
            return size;
        }

        public String[] getTags() {
            return tags;
        }

        public String getComment() {
            return comment;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
