package org.votingsystem.client.util;

import org.votingsystem.model.currency.Tag;
import org.votingsystem.util.Messages;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgUtils {

    public static String truncateLog(String message) {
        if(message == null)
            return null;
        else
            return message.length() > 300 ? message.substring(0, 300) + "..." : message;
    }

    public static String getTagDescription(String tagName) {
        if(Tag.WILDTAG.equals(tagName)) return Messages.currentInstance().get("wildTagLbl").toLowerCase();
        else return tagName.toLowerCase();

    }

}