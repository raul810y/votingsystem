package org.votingsystem.client.backup;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface BackupValidator<T>{

    public void cancel();
    public T call() throws Exception;
}
