package org.votingsystem.web.controlcenter.jaxrs;

import org.votingsystem.dto.CertificateDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.CertificateBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/certificate")
public class CertificateResource {

    private static final Logger log = Logger.getLogger(CertificateResource.class.getName());


    @Inject ConfigVS app;
    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;
    @Inject
    CertificateBean certificateBean;

    @Transactional
    @Path("/certs")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response certs(@DefaultValue("0") @QueryParam("offset") int offset,
                          @DefaultValue("100") @QueryParam("max") int max,
                          @DefaultValue("USER") @QueryParam("type") String typeStr,
                          @DefaultValue("OK") @QueryParam("state") String stateStr,
                          @DefaultValue("") @QueryParam("format") String format, @QueryParam("searchText") String searchText,
                          @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType(): "";
        Certificate.Type type = Certificate.Type.valueOf(typeStr);
        Certificate.State state = Certificate.State.valueOf(stateStr);
        Query query;
        Long totalCount = null;
        if(searchText == null) {
            query = dao.getEM().createQuery("select count(c) from Certificate c where c.type =:type and c.state =:state")
                    .setParameter("type", type).setParameter("state", state);
            totalCount = (long) query.getSingleResult();
            query = dao.getEM().createQuery("select c from Certificate c where c.type =:type and c.state =:state")
                    .setParameter("type", type).setParameter("state", state)
                    .setFirstResult(offset).setMaxResults(max);;
        } else {
            query = dao.getEM().createQuery("select count (c) from Certificate c where c.type =:type and c.state =:state " +
                    "and (c.user.name like :searchText or c.user.nif like :searchText " +
                    "or c.user.firstName like :searchText or c.user.lastName like :searchText " +
                    "or c.user.description like :searchText)").setParameter("type", type)
                    .setParameter("state", state).setParameter("searchText", "%" + searchText + "%");
            totalCount = (long) query.getSingleResult();
            query = dao.getEM().createQuery("select c from Certificate c where c.type =:type and c.state =:state " +
                    "and (c.user.name like :searchText or c.user.nif like :searchText " +
                    "or c.user.firstName like :searchText or c.user.lastName like :searchText " +
                    "or c.user.description like :searchText)").setParameter("type", type)
                    .setParameter("state", state).setParameter("searchText", "%" + searchText + "%")
                    .setFirstResult(offset).setMaxResults(max);;
        }
        List<Certificate> certificates = query.getResultList();
        if(contentType.contains("pem")) {
            List<X509Certificate> resultList = new ArrayList<>();
            for(Certificate certificate : certificates) {
                resultList.add(certificate.getX509Cert());
            }
            return Response.ok().entity(PEMUtils.getPEMEncoded(resultList)).build();
        } else {
            List<CertificateDto> listDto = new ArrayList<>();
            for(Certificate certificate : certificates) {
                listDto.add(new CertificateDto(certificate));
            }
            ResultListDto<CertificateDto> resultListDto = new ResultListDto<>(listDto, offset, max, totalCount);
            if(contentType.contains("json")) {
                return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
            } else {
                req.getSession().setAttribute("certListDto", JSON.getMapper().writeValueAsString(resultListDto));
                return Response.temporaryRedirect(new URI("../certificate/certs.xhtml")).build();
            }
        }
    }

    @GET @Path("/serialNumber/{serialNumber}")
    public Response cert(@PathParam("serialNumber") Long serialNumber, @QueryParam("format") String format,
             @Context ServletContext context,
             @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        Query query = dao.getEM().createQuery("select c from Certificate c where c.serialNumber =:serialNumber")
                .setParameter("serialNumber", serialNumber);
        Certificate certificate = dao.getSingleResult(Certificate.class, query);
        if(certificate == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - Certificate not found - serialNumber: " + serialNumber).build();
        X509Certificate x509Cert = certificate.getX509Cert();
        if(contentType.contains("pem") || (format != null && "pem".equals(format.toLowerCase()))){
            resp.setHeader("Content-Disposition", format("inline; filename='trustedCert_{0}'", serialNumber));
            return Response.ok().entity(PEMUtils.getPEMEncoded (x509Cert)).build();
        } else {
            CertificateDto certDto = new CertificateDto(certificate);
            if(contentType.contains("json")) {
                return Response.ok().entity(JSON.getMapper().writeValueAsBytes(certDto))
                        .type(MediaType.JSON).build();
            } else {
                req.getSession().setAttribute("certMap", certDto);
                req.getSession().setAttribute("certDto", JSON.getMapper().writeValueAsString(certDto));
                return Response.temporaryRedirect(new URI("../certificate/cert.xhtml?menu="+req.getParameter("menu"))).build();
            }
        }
    }

    @Path("/user/id/{userId}")
    @GET  @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    public Response user(@PathParam("userId") Long userId) throws Exception {
        User user = dao.find(User.class, userId);
        if(user == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - User not found - userId: " + userId).build();
        Query query = dao.getEM().createQuery("select c from Certificate c where c.user =:user and c.state =:state")
                .setParameter("user", user).setParameter("state", Certificate.State.OK);
        Certificate certificate = dao.getSingleResult(Certificate.class, query);
        if (certificate != null) {
            X509Certificate certX509 = CertUtils.loadCertificate(certificate.getContent());
            return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
        } else return Response.status(Response.Status.BAD_REQUEST).entity("ERROR - User without active Certificate").build();
    }

    @Path("/eventVS/id/{eventId}/CACert")
    @GET  @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    public Response eventCA(@PathParam("eventId") Long eventId) throws Exception {
        EventElection eventElection = dao.find(EventElection.class, eventId);
        if(eventElection == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - EventElection not found - eventId: " + eventId).build();
        Certificate certificateCA = eventElection.getCertificate();
        X509Certificate certX509 = CertUtils.loadCertificate(certificateCA.getContent());
        return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
    }

    @Path("/hashHex/{hashHex}")
    @GET  @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    public Response hashHex(@PathParam("hashHex") String hashHex, @Context Request req,
            @Context HttpServletResponse resp) throws Exception {
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        String hashCertVSBase64 = new String(hexConverter.unmarshal(hashHex));
        Query query = dao.getEM().createQuery("select c from Certificate c where c.hashCertVSBase64 =:hashCertVS")
                .setParameter("hashCertVS", hashCertVSBase64);
        Certificate certificate = dao.getSingleResult(Certificate.class, query);
        if (certificate != null) {
            X509Certificate certX509 = CertUtils.loadCertificate(certificate.getContent());
            return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
        } else return Response.status(Response.Status.NOT_FOUND).entity("hashHex: " + hashHex).build();
    }

    @Path("/trustedCerts")
    @GET  @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    public Response trustedCerts() throws Exception {
        Set<X509Certificate> trustedCerts = cmsBean.getTrustedCerts();
        return Response.ok().entity(PEMUtils.getPEMEncoded (trustedCerts)).build();
    }

    @Path("/user/{userId}")
    @GET  @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    public Response user(@PathParam("userId") long userId, @Context Request req, @Context HttpServletResponse resp)
            throws Exception {
        User user = dao.find(User.class, userId);
        if(user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("userId: " + userId).build();
        } else {
            Query query = dao.getEM().createQuery("select c from Certificate c where c.user =:user and c.state =:state")
                    .setParameter("user", user).setParameter("state", Certificate.State.OK);
            Certificate certificate = dao.getSingleResult(Certificate.class, query);
            if (certificate != null) {
                X509Certificate certX509 = CertUtils.loadCertificate (certificate.getContent());
                return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
            } else return Response.status(Response.Status.NOT_FOUND).entity("userWithoutCert - userId: " + userId).build();
        }
    }

    @Path("/editCert")
    @POST @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response editCert(CMSMessage cmsMessage, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        Certificate certificate = certificateBean.editCert(cmsMessage);
        return Response.ok().entity("editCert - certificate id: " + certificate.getId()).build();
    }

}