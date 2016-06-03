/**
 * Copyright (c) 2014 - 2016 Ventiv Technology
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
package org.ventiv.docker.manager.utils;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by jcrygier on 5/27/16.
 */
public class LogContainerToStreamCallback extends LogContainerResultCallback {

    private OutputStream outputStream;

    public LogContainerToStreamCallback(OutputStream outputStream) {
        super();

        this.outputStream = new DockerLogsOutputStream(outputStream);
    }

    @Override
    public void onNext(Frame item) {
        try {
            outputStream.write(item.getPayload());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
