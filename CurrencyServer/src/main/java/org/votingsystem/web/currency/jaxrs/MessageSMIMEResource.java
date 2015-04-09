package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.currency.ejb.UserVSBean;
import org.votingsystem.web.currency.util.AsciiDocUtil;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/messageSMIME")
public class MessageSMIMEResource {

    private static final Logger log = Logger.getLogger(MessageSMIMEResource.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject UserVSBean userVSBean;

    @Path("/id/{id}") @GET
    public Object index(@PathParam("id") long id, @Context ServletContext context,
                                @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        MessageSMIME messageSMIME = dao.find(MessageSMIME.class, id);
        if(messageSMIME == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "MessageSMIME not found - id: " + id).build();
        if(contentType.contains(ContentTypeVS.TEXT.getName())) {
            return Response.ok().entity(messageSMIME.getContent()).type(ContentTypeVS.TEXT_STREAM.getName()).build();
        } else return processRequest(messageSMIME, context, req, resp);
    }

    private Object processRequest(MessageSMIME messageSMIME, @Context ServletContext context,
                                  @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        String smimeMessageStr = Base64.getEncoder().encodeToString(messageSMIME.getContent());
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        Date timeStampDate = null;
        Map signedContentMap;
        String viewer = "message-smime";
        if(smimeMessage.getTimeStampToken() != null) {
            timeStampDate = smimeMessage.getTimeStampToken().getTimeStampInfo().getGenTime();
        }
        if(smimeMessage.getContentTypeVS() == ContentTypeVS.ASCIIDOC) {
            signedContentMap = messageSMIME.getSignedContentMap();
            signedContentMap.put("asciiDoc", messageSMIME.getSMIME().getSignedContent());
            signedContentMap.put("asciiDocHTML", AsciiDocUtil.getHTML(messageSMIME.getSMIME().getSignedContent()));
        } else {
            signedContentMap = messageSMIME.getSignedContentMap();
        }
        if((boolean)signedContentMap.get("isTimeLimited")) {
            signedContentMap.put("validTo", DateUtils.getDayWeekDateStr(
                    DateUtils.getNexMonday(DateUtils.getCalendar(timeStampDate)).getTime()));
        }
        TypeVS operation = TypeVS.valueOf((String) signedContentMap.get("operation"));
        if(TypeVS.CURRENCY_SEND != operation) {
            if(signedContentMap.containsKey("fromUserVS")) {
                signedContentMap.put("fromUserVS", userVSBean.getUserVSBasicDataMap(messageSMIME.getUserVS()));
            }

        }
        switch(operation) {
            case CURRENCY_GROUP_NEW:
                viewer = "message-smime-groupvs-new";
                break;
            case FROM_BANKVS:
                viewer = "message-smime-transactionvs-from-bankvs";
                break;
            case CURRENCY_REQUEST:
                viewer = "message-smime-transactionvs-cooin-request";
                break;
        }
        /*if(params.operation) {
            try {
                TypeVS operationType = TypeVS.valueOf(params.operation.toUpperCase())
                operationType = TypeVS.valueOf(params.operation.toUpperCase())
                switch(operationType) {
                    case TypeVS.FROM_BANKVS:
                        viewer = "message-smime"
                        break;
                    case TypeVS.FROM_GROUP_TO_ALL_MEMBERS:
                        viewer = "message-smime"
                        break;
                }
            } catch(Exception ex) { log.error(ex.getMessage(), ex)}
        }*/
        if(contentType.contains("json")) {
            Map resultMap = new HashMap<>();
            resultMap.put("operation", signedContentMap.get("operation"));
            resultMap.put("smimeMessage", smimeMessageStr);
            req.setAttribute("signedContentMap", new ObjectMapper().writeValueAsString(signedContentMap));
            resultMap.put("timeStampDate", DateUtils.getISODateStr(timeStampDate));
            resultMap.put("viewer", viewer + ".vsp");
            return resultMap;
        } else {
            req.setAttribute("operation", signedContentMap.get("operation"));
            req.setAttribute("smimeMessage", smimeMessageStr);
            req.setAttribute("signedContentMap", signedContentMap);
            req.setAttribute("timeStampDate", DateUtils.getISODateStr(timeStampDate));
            req.setAttribute("viewer",  viewer + ".vsp");
            context.getRequestDispatcher("/jsf/messageSMIME/contentViewer.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/transactionVS/id/{id}") @GET // old_url -> /messageSMIME/transactionVS/$id
    public Object transactionVS(@PathParam("id") long id, @Context ServletContext context,
                        @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        TransactionVS transactionVS = dao.find(TransactionVS.class, id);
        if(transactionVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "TransactionVS not found - transactionVSId: " + id);
        return processRequest(transactionVS.getMessageSMIME(), context, req, resp);
    }


}