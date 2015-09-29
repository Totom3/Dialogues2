package io.github.totom3.commons.binary;

import java.io.IOException;

/**
 *
 * @author Totom3
 */
public class SerializingException extends IOException {
    private static final long serialVersionUID = 1L;

    public SerializingException() {
    }

    public SerializingException(String message) {
	super(message);
    }

    public SerializingException(String message, Throwable cause) {
	super(message, cause);
    }

    public SerializingException(Throwable cause) {
	super(cause);
    }

}
