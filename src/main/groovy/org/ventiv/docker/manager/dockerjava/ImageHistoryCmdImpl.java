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

import com.github.dockerjava.core.command.AbstrDockerCmd;

import java.util.List;

/**
 * Created by jcrygier on 5/1/15.
 */
public class ImageHistoryCmdImpl extends AbstrDockerCmd<ImageHistoryCmd, List<ImageHistoryCmd.ImageHistory>> implements ImageHistoryCmd {

    private String imageName;

    public ImageHistoryCmdImpl(ImageHistoryCmd.Exec execution, String imageName) {
        super(execution);
        withImageName(imageName);
    }

    @Override
    public String getImageName() {
        return imageName;
    }

    @Override
    public ImageHistoryCmd withImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }
}
