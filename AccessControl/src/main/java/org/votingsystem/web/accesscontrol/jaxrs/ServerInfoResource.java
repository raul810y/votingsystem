package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.dto.ActorDto;
import org.votingsystem.model.Actor;
import org.votingsystem.model.voting.ControlCenter;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/serverInfo")
public class ServerInfoResource {

    private static final Logger log = Logger.getLogger(ServerInfoResource.class.getName());

    @EJB ConfigVS configVS;
    @EJB CMSBean cmsBean;
    @EJB TimeStampBean timeStampBean;

    @GET @Produces(MediaType.APPLICATION_JSON)
    public Map doGet(@Context HttpServletRequest req, @Context HttpServletResponse resp) {
        HashMap serverInfo = new HashMap();
        serverInfo.put("serverType", Actor.Type.ACCESS_CONTROL);
        serverInfo.put("name", configVS.getServerName());
        serverInfo.put("serverURL", configVS.getContextURL());
        serverInfo.put("state",  Actor.State.OK);
        serverInfo.put("date", new Date());
        ControlCenter controlCenter = configVS.getControlCenter();
        if(controlCenter != null) serverInfo.put("controlCenter", new ActorDto(controlCenter));
        serverInfo.put("timeStampCertPEM", new String(timeStampBean.getSigningCertPEMBytes()));
        serverInfo.put("timeStampServerURL", configVS.getTimeStampServerURL());
        serverInfo.put("certChainPEM", new String(cmsBean.getKeyStoreCertificatesPEM()));
        //resp.setHeader("Access-Control-Allow-Origin", "*");
        //if (params.callback) render "${param.callback}(${serverInfo as JSON})"
        return serverInfo;
    }

}