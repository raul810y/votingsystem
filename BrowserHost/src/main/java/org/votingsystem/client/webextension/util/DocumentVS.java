package org.votingsystem.client.webextension.util;

import org.votingsystem.util.ContentTypeVS;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface DocumentVS {

    public byte[] getDocumentBytes() throws Exception;
    public ContentTypeVS getContentTypeVS();

}
