package org.votingsystem.throwable;

/**

 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletException extends ExceptionBase {

    public WalletException(String message) {
        super(message);
    }

    public WalletException(String message, String metaInf) {
        super(message, metaInf);
    }

    public WalletException(String message, String metaInf, Throwable cause) {
        super(message, metaInf, cause);
    }

    public WalletException(String message, Throwable cause) {
        super(message, cause);
    }

}
