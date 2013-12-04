package org.votingsystem.util;

import org.apache.log4j.Logger;

import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class StringUtils {
	
    private static Logger logger = Logger.getLogger(StringUtils.class);

    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String getStringFromClob (Clob clob) {
    		if(clob == null) return null;
            String stringClob = "";
            try {
            	if (clob.length() == 0) return stringClob;
                long i = 1;
                int clobLength = (int) clob.length();
                stringClob = clob.getSubString(i, clobLength);
            }
            catch (Exception e) {
                    logger.error(e.getMessage(), e);
            }
            return stringClob;
    }	

    public static String randomLowerString(long seed, int size) {
        StringBuffer tmp = new StringBuffer();
        Random random = new Random(seed);
        for (int i = 0; i < size; i++) {
            long newSeed = random.nextLong();
            int currInt = (int) (26 * random.nextFloat());
            currInt += 97;
            random = new Random(newSeed);
            tmp.append((char) currInt);
        }
        return tmp.toString();
    }
    
    public static Clob getClobFromString (String string) {
    	if(string == null) return null;
        Clob clob = null;
        try {
            clob = new SerialClob(string.toCharArray());
        } catch (SerialException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return clob;
    }

    public static String getFormattedErrorList(List<String> errorList) {
        if(errorList.isEmpty()) return null;
        else {
            StringBuilder result = new StringBuilder("");
            for(String error:errorList) {
                result.append(error + "\n");
            }
            return result.toString();
        }
    }

    public static String getUserDirPath (String userNIF) {
        int subPathLength = 3;
        String basePath = "/";
        while (userNIF.length() > 0) {
            if(userNIF.length() <= subPathLength) subPathLength = userNIF.length();
            String subPath = userNIF.substring(0, subPathLength);
            userNIF = userNIF.substring(subPathLength);
            basePath = basePath + subPath + File.separator;
        }
        if(!basePath.endsWith("/")) basePath = basePath + "/";
        return basePath;
    }

    public static String checkURL(String url) {
    	if(url == null) return null;
    	url = url.trim();
        String result = null;
        if(url.startsWith("http://") || url.startsWith("https://")) result = url;
        else result = "http://" + url;
        while(result.endsWith("/")) {
        	result = result.substring(0, result.length() -1);
        }
        return result;
    }
    
    public static String getStringFromInputStream(InputStream entrada) throws IOException {
    	ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buf =new byte[1024];
        int len;
        while((len = entrada.read(buf)) > 0){
            salida.write(buf,0,len);
        }
        salida.close();
        entrada.close();
        return new String(salida.toByteArray(), "UTF-8");
    }
    
    public static String getNormalized (String cadena) {
        if(cadena == null) return null;
        else return cadena.replaceAll("[\\/:.]", ""); 
    }

    public static String getRandomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()* ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

}
