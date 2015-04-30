package org.ventiv.docker.manager.utils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapped output stream to strip docker logs / attach header information
 * See: http://docs.docker.com/reference/api/docker_remote_api_v1.13/#attach-to-a-container
 */
public class DockerLogsOutputStream extends OutputStream {

    public static final byte[] STD_IN  = "STDIN : ".getBytes();
    public static final byte[] STD_OUT = "STDOUT: ".getBytes();
    public static final byte[] STD_ERR = "STDERR: ".getBytes();

    private OutputStream wrapped;
    private boolean prependStreamName = false;

    public DockerLogsOutputStream(OutputStream wrapped) {
        this.wrapped = wrapped;
    }

    public DockerLogsOutputStream(OutputStream wrapped, boolean prependStreamName) {
        this.wrapped = wrapped;
        this.prependStreamName = prependStreamName;
    }

    @Override
    public void write(int b) throws IOException {
        wrapped.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (b.length >= off + 8 && len >= 8 &&
                (b[off] == 0x0 || b[off] == 0x1 || b[off] == 0x2) &&
                b[off+1] == 0 &&
                b[off+2] == 0 &&
                b[off+3] == 0) {
            // TODO: If we ever care, extract header information: b[off] == 0 -> Stdin, b[off] == 1 -> Stdout, b[off] == 2 -> Stderr
            if (prependStreamName) {
                byte[] toPrepend = STD_IN;
                if (b[off] == 0x1) toPrepend = STD_OUT;
                else if (b[off] == 0x2) toPrepend = STD_ERR;

                System.arraycopy(toPrepend, 0, b, 0, toPrepend.length);

                super.write(b, off, len);
            } else {
                super.write(b, off+8, len-8);
            }
        } else {
            super.write(b, off, len);
        }
    }

}
