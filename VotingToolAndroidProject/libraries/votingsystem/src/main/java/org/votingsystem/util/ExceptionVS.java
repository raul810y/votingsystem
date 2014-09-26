package org.votingsystem.util;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ExceptionVS extends Exception {

    public ExceptionVS(String message) {
        super(message);
    }

    public ExceptionVS(String message, Throwable cause) {
        super(message, cause);
    }

}