package org.votingsystem.web.currency.websocket;


import javax.websocket.server.ServerEndpointConfig;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SocketConfigurator extends ServerEndpointConfig.Configurator {

    private static Logger log = Logger.getLogger(SocketConfigurator.class.getName());

    @Override public boolean checkOrigin(String originHeaderValue) {
        return true;
    }

}
