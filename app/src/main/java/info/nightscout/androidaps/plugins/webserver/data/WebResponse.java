package info.nightscout.androidaps.plugins.webserver.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * Created by jamorham on 06/01/2018.
 *
 * Data class for webservice responses
 */

public class WebResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebResponse.class);

    public byte[] bytes;
    public String mimeType;
    public int resultCode;

    public WebResponse(String str) {
        this(str, 200, "application/json");
    }

    public WebResponse(String str, int resultCode, String mimeType) {
        try {
            bytes = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("UTF8 is unsupported!");
        }
        this.mimeType = mimeType;
        this.resultCode = resultCode;
    }
}
