package org.votingsystem.model;


import org.votingsystem.throwable.ValidationExceptionVS;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
//@Indexed
@Entity @Table(name="EventVSElection") @DiscriminatorValue("EventVSElection")
public class EventVSElection extends EventVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public EventVSElection() {}

    public EventVSElection(String subject, String content, Cardinality cardinality, UserVS userVS,
                   ControlCenterVS controlCenterVS, Date dateBegin, Date dateFinish) {
        setSubject(subject);
        setContent(content);
        setCardinality(cardinality);
        setUserVS(userVS);
        setControlCenterVS(controlCenterVS);
        setDateBegin(dateBegin);
        setDateFinish(dateFinish);
    }

    public EventVSElection(Long accessControlEventVSId, String subject, String content, String URL,
                   AccessControlVS accessControl, UserVS userVS, Date dateBegin, Date dateFinish) {
        setAccessControlEventVSId(accessControlEventVSId);
        setSubject(subject);
        setContent(content);
        setUrl(URL);
        setAccessControlVS(accessControl);
        setUserVS(userVS);
        setDateBegin(dateBegin);
        setDateFinish(dateFinish);
    }

    public FieldEventVS checkOptionId(Long optionId) throws ValidationExceptionVS {
        for(FieldEventVS option: getFieldsEventVS()) {
            if(optionId.longValue() == option.getId().longValue()) return option;
        }
        throw new ValidationExceptionVS("FieldEventVS not found - id: " + optionId);
    }

    public EventVSElection updateAccessControlIds() {
        setId(null);
        if(getFieldsEventVS() != null) {
            for(FieldEventVS fieldEventVS : getFieldsEventVS()) {
                fieldEventVS.setAccessControlFieldEventId(fieldEventVS.getId());
                fieldEventVS.setId(null);
            }
        }
        if(getTagVSSet() != null) {
            for(TagVS tagVS : getTagVSSet()) {
                tagVS.setId(null);
            }
        }
        return this;
    }

}
