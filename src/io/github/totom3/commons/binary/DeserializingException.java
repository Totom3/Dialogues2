package io.github.totom3.commons.binary;

import java.io.IOException;

/**
 *
 * @author Totom3
 */
public class DeserializingException extends IOException {
    private static final long serialVersionUID = 1L;

    public DeserializingException() {
    }

    public DeserializingException(String message) {
	super(message);
    }

    public DeserializingException(String message, Throwable cause) {
	super(message, cause);
    }

    public DeserializingException(Throwable cause) {
	super(cause);
    }
}
