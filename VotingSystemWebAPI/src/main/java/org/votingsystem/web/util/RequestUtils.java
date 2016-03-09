package org.votingsystem.web.util;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.MessageCMS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.TypeVS;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class RequestUtils {

    public static Object processRequest(MessageCMS messageCMS, @Context ServletContext context,
                                        @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        String cmsMessageStr = Base64.getEncoder().encodeToString(messageCMS.getContentPEM());
        CMSSignedMessage cmsMessage = messageCMS.getCMS();
        Date timeStampDate = null;
        Map signedContentMap;
        String viewer = "message-cms";
        if(cmsMessage.getTimeStampToken() != null) {
            timeStampDate = cmsMessage.getTimeStampToken().getTimeStampInfo().getGenTime();
        }
        signedContentMap = messageCMS.getSignedContentMap();
        TypeVS operation = TypeVS.valueOf((String) signedContentMap.get("operation"));
        switch(operation) {
            case SEND_VOTE:
                viewer = "message-cms-votevs";
                break;
            case CANCEL_VOTE:
                viewer = "message-cms-votevs-canceler";
                break;
            case ANONYMOUS_SELECTION_CERT_REQUEST:
                viewer = "message-cms-representative-anonymousdelegation-request";
                break;
            case ACCESS_REQUEST:
                viewer = "message-cms-access-request";
                break;
        }
        if(contentType.contains("json")) {
            Map resultMap = new HashMap<>();
            resultMap.put("operation", operation);
            resultMap.put("cmsMessage", cmsMessageStr);
            resultMap.put("signedContentMap", signedContentMap);
            resultMap.put("timeStampDate", timeStampDate.getTime());
            resultMap.put("viewer", viewer);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultMap)).type(MediaTypeVS.JSON).build();
        } else {
            req.getSession().setAttribute("operation", operation);
            req.getSession().setAttribute("cmsMessage", cmsMessageStr);
            req.getSession().setAttribute("signedContentMap", JSON.getMapper().writeValueAsString(signedContentMap));
            req.getSession().setAttribute("timeStampDate", timeStampDate.getTime());
            req.getSession().setAttribute("viewer", viewer);
            return Response.temporaryRedirect(new URI("../messageCMS/contentViewer.xhtml")).build();
        }
    }


}
