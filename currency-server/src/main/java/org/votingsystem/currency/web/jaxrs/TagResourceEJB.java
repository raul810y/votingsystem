package org.votingsystem.currency.web.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.currency.web.ejb.ConfigCurrencyServer;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyAccountDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Tag;
import org.votingsystem.util.JSON;
import org.votingsystem.util.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
@Path("/tag")
public class TagResourceEJB {

    private static final Logger log = Logger.getLogger(TagResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private SignatureService signatureService;

    @GET @Path("/")
    public Response index(@DefaultValue("") @QueryParam("tag") String tag, @Context ServletContext context,
                  @Context HttpServletRequest req, @Context HttpServletResponse resp) throws JsonProcessingException {
        List<Tag> tagList = em.createQuery("select t from Tag t where upper(t.name) like :tag").setParameter("tag",
                "%" + StringUtils.removeAccents(tag).toUpperCase() + "%").getResultList();
        ResultListDto<Tag> resultListDto = new ResultListDto<>(tagList);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).type(MediaType.JSON).build();
    }

    @GET @Path("/list")
    public Response list() throws JsonProcessingException {
        List<Tag> tagList = em.createQuery("select t from Tag t").getResultList();
        ResultListDto resultList = new ResultListDto(tagList);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultList)).type(MediaType.JSON).build();
    }

    @GET @Path("/currencyAccountByBalanceTag")
    public Response currencyAccountByBalanceTag(@QueryParam("tag") String tag) throws JsonProcessingException {
        if(tag == null)
            return Response.status(Response.Status.BAD_REQUEST).entity("missing param - tag").build();
        List<CurrencyAccount> accountList = em.createQuery("select c from CurrencyAccount c where c.tag.name =:tag")
                .setParameter("tag", tag).getResultList();
        List<CurrencyAccountDto> resultList = new ArrayList<>();
        for(CurrencyAccount account : accountList) {
            resultList.add(new CurrencyAccountDto(account));
        }
        ResultListDto resultListDto = new ResultListDto(resultList);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).type(MediaType.JSON).build();
    }

}