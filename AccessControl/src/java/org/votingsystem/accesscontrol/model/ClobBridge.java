package org.votingsystem.accesscontrol.model;

import java.sql.Clob;

import org.hibernate.search.bridge.StringBridge;
import org.votingsystem.util.StringUtils;

public class ClobBridge implements StringBridge {

    @Override
    public String objectToString(Object object){
        return StringUtils.getStringFromClob((Clob) object);
    }

}