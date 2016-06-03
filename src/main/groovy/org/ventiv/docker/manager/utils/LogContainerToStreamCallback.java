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
