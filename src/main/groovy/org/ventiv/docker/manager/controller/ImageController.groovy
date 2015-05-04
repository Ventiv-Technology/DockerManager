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
package org.ventiv.docker.manager.controller

import com.github.dockerjava.api.model.Image
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.dockerjava.ImageHistoryCmd
import org.ventiv.docker.manager.model.ImageDetails
import org.ventiv.docker.manager.service.DockerService
import org.ventiv.docker.manager.service.ServiceInstanceService

import javax.annotation.Resource

/**
 * Methods to deal with Images
 */
@Slf4j
@RequestMapping("/api/image")
@RestController
@CompileStatic
class ImageController {

    @Resource DockerService dockerService;
    @Resource ServiceInstanceService serviceInstanceService;

    @RequestMapping("/{hostName:.*}")
    public List<ImageDetails> getImages(@PathVariable String hostName) {
        // TODO: Make tree out of response, and push serviceInstanceList up to parents
        dockerService.getDockerClient(hostName).listImagesCmd().exec().collect { Image image ->
            return new ImageDetails([
                    id: image.getId(),
                    tag: image.getRepoTags()?.first(),
                    virtualSize: image.getVirtualSize(),
                    serviceInstanceList: serviceInstanceService.getServiceInstances().findAll {
                        it.getServerName() == hostName && (it.getContainerImageId() == image.getId() || it.getContainerImage().toString() == image.getRepoTags()?.first());
                    }
            ])
        }
    }

    @RequestMapping("/{hostName}/{imageTag:.*}")
    public List<ImageDetails> getImageDetails(@PathVariable String hostName, @PathVariable String imageTag) {
        List<ImageHistoryCmd.ImageHistory> imageHistoryList = dockerService.getImageHistoryCmd(hostName, imageTag).exec();

        List<ImageDetails> answer = [];
        ImageDetails currentDetail = null;
        imageHistoryList.each { ImageHistoryCmd.ImageHistory imageHistory ->
            if (imageHistory.getTags()) {
                currentDetail = new ImageDetails(id: imageHistory.getId(), tag: imageHistory.getTags().first(), history: [], serviceInstanceList: []);
                answer << currentDetail;

                currentDetail.serviceInstanceList.addAll(serviceInstanceService.getServiceInstances().findAll {
                    it.getServerName() == hostName && (it.getContainerImageId() == imageHistory.getId() || it.getContainerImage().toString() == imageHistory.getTags().first());
                });
            }

            currentDetail.getHistory().add(imageHistory);
        }

        for (int i = 0; i < answer.size(); i++) {
            ImageDetails imageDetails = answer[i];

            for (int j = i; j < answer.size(); j++) {
                imageDetails.virtualSize += answer[j].getIncrementalSize();
            }
        }

        return answer;
    }

    @RequestMapping(value = "/{hostName}/{imageId:.*}", method = RequestMethod.DELETE)
    public void removeImage(@PathVariable String hostName, @PathVariable String imageId) {
        dockerService.getDockerClient(hostName).removeImageCmd(imageId).exec()
    }

    @RequestMapping(value = "/{hostName:.*}", method = RequestMethod.DELETE)
    public Map<String, String> removeImages(@PathVariable String hostName, @RequestBody List<String> imagesToDelete) {
        imagesToDelete.collectEntries { String imageToDelete ->
            try {
                removeImage(hostName, imageToDelete);
                return [imageToDelete, "Successful"]
            } catch (Exception e) {
                return [imageToDelete, e.getMessage()]
            }
        }
    }

}
