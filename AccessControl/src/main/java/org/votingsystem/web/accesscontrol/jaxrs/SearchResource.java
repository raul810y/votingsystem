package org.votingsystem.web.accesscontrol.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.accesscontrol.ejb.EventVSBean;
import org.votingsystem.web.accesscontrol.ejb.RepresentativeBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.logging.Logger;

@Path("/search")
public class SearchResource {

    private static final Logger log = Logger.getLogger(SearchResource.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject EventVSBean eventVSBean;
    @Inject RepresentativeBean representativeBean;

    @Transactional
    @Path("/representative") @GET
    public Response doGet(@QueryParam("searchText") String searchText,
                          @DefaultValue("0") @QueryParam("offset") int offset,
                          @DefaultValue("100") @QueryParam("max") int max,
                          @Context Request req, @Context HttpServletResponse resp) throws JsonProcessingException {
        Query query = dao.getEM().createQuery("select u from User u where u.type =:type and (u.firstName like :searchText " +
                "or u.lastName like :searchText or u.description like :searchText)").setParameter(
                "type", User.Type.REPRESENTATIVE).setParameter("searchText", "%" + searchText + "%");
        List<User> representativeList = query.getResultList();
        List<UserDto> resultList = new ArrayList<>();
        for(User representative : representativeList) {
            resultList.add(representativeBean.getRepresentativeDto(representative));
        }
        Map representativeMap = new HashMap<>();
        representativeMap.put("representatives", resultList);
        representativeMap.put("offset", offset);
        representativeMap.put("max", max);
        representativeMap.put("numRepresentatives", resultList.size());
        representativeMap.put("numTotalRepresentatives", resultList.size()); //TODO totalCount
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(representativeMap)).build();
    }

    @Transactional
    @Path("/eventVS") @GET
    public Response eventVS(@QueryParam("searchText") String searchText, @QueryParam("eventVSState") String eventVSStateReq,
                          @DefaultValue("0") @QueryParam("offset") int offset,
                          @DefaultValue("100") @QueryParam("max") int max,
                          @Context Request req, @Context HttpServletResponse resp) throws JsonProcessingException {
        List<EventVS.State> inList = Arrays.asList(EventVS.State.ACTIVE, EventVS.State.PENDING, EventVS.State.CANCELED,
                EventVS.State.TERMINATED);
        if(eventVSStateReq != null) {
            try {
                EventVS.State eventVSState = EventVS.State.valueOf(eventVSStateReq);
                if(eventVSState == EventVS.State.TERMINATED) {
                    inList = Arrays.asList(EventVS.State.TERMINATED,EventVS.State.CANCELED);
                } else if(eventVSState != EventVS.State.DELETED_FROM_SYSTEM) inList = Arrays.asList(eventVSState);
            } catch(Exception ex) {}
        }
        /*Date dateBeginFrom = null;
        Date dateBeginTo = null;
        if(params.dateBeginFrom) try {dateBeginFrom = DateUtils.getDateFromString(params.dateBeginFrom)} catch(Exception ex) {}
        if(params.dateBeginTo) try {dateBeginTo = DateUtils.getDateFromString(params.dateBeginTo)} catch(Exception ex) {}*/
        Query query = dao.getEM().createQuery("select e from EventVS e where e.state in :inList and " +
                "(e.subject like :searchText or e.content like :searchText)").setParameter("inList", inList)
                .setParameter("searchText", "%" + searchText + "%");
        List<EventVS> eventvsList = query.getResultList();
        List<EventVSDto> dtoList = new ArrayList<>();
        for(EventVS eventVS : eventvsList) {
            dtoList.add(new EventVSDto(eventVS, config.getServerName(), config.getContextURL()));
        }
        ResultListDto<EventVSDto> resultListDto = new ResultListDto<>(dtoList, offset, max, dtoList.size());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).type(MediaTypeVS.JSON).build();
    }

}
