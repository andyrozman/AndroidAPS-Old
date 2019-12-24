package info.nightscout.androidaps.plugins.webserver.services;








import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;



import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.db.BgReading;
import info.nightscout.androidaps.plugins.webserver.data.WebResponse;
import info.nightscout.androidaps.plugins.webserver.util.WebServerUtil;


/**
 * Created by jamorham on 06/01/2018.
 * <p>
 * emulates the Nightscout /pebble endpoint
 * <p>
 * we use the mgdl/mmol setting from preferences and ignore any query string
 * <p>
 * Set the data endpoint on Pebble Nightscout watchface to: http://127.0.0.1:17580/pebble
 *
 *  Refactored to work with AndroidAPS database, and changed some data handling.
 */

public class WebServicePebble extends BaseWebService {

    //private static String TAG = "WebServicePebble";
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceTreatments.class);


    WebServicePebble() {

    }

    // process the request and produce a response object
    public WebResponse request(String query) {

//        final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
//        if (dg == null) return null; // TODO better error handling?
//
//        final BgReading bgReading = BgReading.last();
//        if (bgReading == null) return null; // TODO better error handling?

        //final Calibration calibration = Calibration.lastValid(); // this can be null

        // prepare json objects
        final JSONObject reply = new JSONObject();

        final JSONObject status = new JSONObject();
        final JSONObject bgs = new JSONObject();
        final JSONObject cals = new JSONObject();

        final JSONArray status_array = new JSONArray();
        final JSONArray bgs_array = new JSONArray();
        final JSONArray cals_array = new JSONArray();

        // populate json structures
        try {

            status.put("now", System.currentTimeMillis());

            List<BgReading> readings = MainApp.getDbHelper().getBgreadingsDataByCount(2, false);

            BgReading reading = readings.get(0);
            BgReading nextReading = readings.get(1);

            if (WebServerUtil.isMgDl()) {
                bgs.put("sgv", reading.value);
            } else {
                bgs.put("sgv", WebServerUtil.getMmolValue(reading.value));
            }
            //bgs.put("trend", bgReading.getSlopeOrdinal()); // TODO beware not coming from Display Glucose
            bgs.put("direction", reading.direction);
            bgs.put("datetime", reading.date);
            bgs.put("filtered", (long) (reading.raw * 1000));
            bgs.put("unfiltered", (long) (reading.raw * 1000));

            bgs.put("noise", 1);

            BigDecimal delta = WebServerUtil.getDelta(reading, nextReading);

            if (WebServerUtil.isMgDl()) {
                bgs.put("bgdelta", delta.longValue());
            } else {
                bgs.put("bgdelta", WebServerUtil.getMmolValue(delta.doubleValue()));
            }

            bgs.put("battery", 100); // TODO
            bgs.put("iob", 0); // TODO get iob
            bgs.put("bwp", 0);  // TODO bwp
            bgs.put("bwpo", 0); // TODO output bwp and bwpo

            status_array.put(status);
            bgs_array.put(bgs);

            reply.put("status", status_array);
            reply.put("bgs", bgs_array);

//            // optional calibration
//            if (calibration != null) {
//                cals.put("scale", 1);
//                cals.put("slope", calibration.slope * 1000);
//                cals.put("intercept", calibration.intercept * 1000); // negated??
//                cals_array.put(cals);
//                reply.put("cals", cals_array);
//
//            }

            reply.put("cals", cals_array);

            LOGGER.debug("Pebble Data: " + reply.toString());

        } catch (JSONException e) {
            LOGGER.error("Got json exception: " + e);
        }

        return new WebResponse(reply.toString());
    }



//    {
//        "status": [
//        {
//            "now": 1577095397847
//        }
//    ],
//        "bgs": [
//        {
//                "sgv": "7.4",
//                "trend": 4,
//                "direction": "Flat",
//                "datetime": 1577095350649,
//                "bgdelta": "0.2",
//                "battery": "87",
//                "iob": "2.67",
//                "bwp": "-0.95",
//                "bwpo": 4.5,
//                "cob": 4.7
//        }
//    ],
//        "cals": []
//    }



}
