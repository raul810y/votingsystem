package org.votingsystem.util;

import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.vicket.model.Vicket;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletUtils {

    private static Logger logger = Logger.getLogger(StringUtils.class);

    public static void saveVicketsToWallet(Collection<Vicket> vicketCollection, String walletPath) throws Exception {
        for(Vicket vicket : vicketCollection) {
            byte[] vicketSerialized =  ObjectUtils.serializeObject(vicket);
            new File(walletPath).mkdirs();
            String vicketPath = walletPath + UUID.randomUUID().toString() + ".servs";
            File vicketFile = FileUtils.copyStreamToFile(new ByteArrayInputStream(vicketSerialized), new File(vicketPath));
            logger.debug("Stored vicket: " + vicketFile.getAbsolutePath());
        }

    }

    public static List<Map> getSerializedVicketList(Collection<Vicket> vicketCollection) throws UnsupportedEncodingException {
        List<Map> result = new ArrayList<>();
        for(Vicket vicket : vicketCollection) {
            Map<String, String> vicketDataMap = vicket.getCertSubject().getDataMap();
            byte[] vicketSerialized =  ObjectUtils.serializeObject(vicket);
            vicketDataMap.put("object", new String(vicketSerialized, "UTF-8"));
            result.add(vicketDataMap);
        }
        return result;
    }

}
