package org.votingsystem.client.util;

import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Notification<T> {

    private static Logger log = Logger.getLogger(Notification.class);

    public enum State {PENDING, PROCESSED}

    private TypeVS typeVS;
    private String message;
    private Date date;
    private State state = State.PENDING;
    private String UUID;
    private T data;

    public Notification() {}

    public Notification(JSONObject jsonObject) throws ParseException {
        typeVS = TypeVS.valueOf(jsonObject.getString("typeVS"));
        message = jsonObject.getString("message");
        date = DateUtils.getDayWeekDate(jsonObject.getString("date"));
        UUID = jsonObject.getString("UUID");
    }

    public Date getDate() {
        return date;
    }

    public Notification setDate(Date date) {
        this.date = date;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Notification setMessage(String message) {
        this.message = message;
        return this;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public Notification setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
        return this;
    }

    public State getState() {
        return state;
    }

    public Notification setState(State state) {
        this.state = state;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public Notification setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public static Notification getPlainWalletNotEmptyNotification(List<Cooin> cooinList) {
        Notification notification = new Notification();
        return notification.setMessage(MsgUtils.getPlainWalletNotEmptyMsg(Cooin.getCurrencyMap(
                cooinList))).setTypeVS(TypeVS.COOIN_IMPORT);
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        result.put("date", DateUtils.getDayWeekDateStr(date));
        result.put("message", message);
        result.put("typeVS", typeVS.toString());
        result.put("UUID", getUUID());
        return result;
    }

}
