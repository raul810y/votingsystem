package org.votingsystem.serviceprovider.util;

import org.votingsystem.ejb.QRSessionsEJB;

import javax.ejb.EJB;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@WebListener
public class HttpSessionManager implements HttpSessionListener {

    private static Logger log = Logger.getLogger(HttpSessionManager.class.getName());

    @EJB private QRSessionsEJB qrSessions;

    private static HttpSessionManager INSTANCE;

    public HttpSessionManager() {
        INSTANCE = this;
    }

    @Override
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        log.info("sessionCreated: " + sessionEvent.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        Set<String> qrOperations = (Set<String>) sessionEvent.getSession().getAttribute(Constants.QR_OPERATIONS_KEY);
        log.info("sessionDestroyed: " + sessionEvent.getSession().getId() + " - closing qrOperations: " + qrOperations);
        if(qrOperations != null) {
            for(String qrOperation:qrOperations) {
                qrSessions.removeOperation(qrOperation);
            }
        }
    }

    public static HttpSessionManager getInstance() {
        return INSTANCE;
    }

}
