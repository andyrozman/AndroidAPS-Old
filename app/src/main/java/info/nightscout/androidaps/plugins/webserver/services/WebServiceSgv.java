package info.nightscout.androidaps.plugins.webserver.services;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.db.BgReading;
import info.nightscout.androidaps.plugins.webserver.data.WebResponse;
import info.nightscout.androidaps.plugins.webserver.util.WebServerUtil;


/**
 * Created by jamorham on 06/01/2018.
 * <p>
 * emulates the Nightscout /api/v1/entries/sgv.json endpoint at sgv.json
 * <p>
 * Always outputs 24 items and ignores any parameters
 * Always uses display glucose values
 * <p>
 *  Refactored to work with AndroidAPS database, and changed some data handling.
 */

public class WebServiceSgv extends BaseWebService {

    //private static String TAG = "WebServiceSgv";
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceTreatments.class);


    // process the request and produce a response object
    public WebResponse request(String query) {

        int units_indicator = 1; // show the units we are using
        boolean brief = false; // whether to cut out portions of the data

        final Map<String, String> cgi = getQueryParameters(query);

        LOGGER.debug("Sgv: [queryParameters={}]", cgi);

        int count = 24;

        if (cgi.containsKey("count")) {
            try {
                count = Integer.valueOf(cgi.get("count"));

                if (count>1000 || count<1) {
                    count = 25;
                }

                LOGGER.debug("SGV count request for: " + count + " entries");
            } catch (Exception e) {
                // meh
            }
        }

        if (cgi.containsKey("brief_mode")) {
            brief = true;
        }


        final JSONArray reply = new JSONArray();

        // whether to include data which doesn't match the current sensor

        List<BgReading> readings = MainApp.getDbHelper().getBgreadingsDataByCount(count, false);

        if (readings != null) {
            // populate json structures
            try {

                // for each reading produce a json record

                for(int i=0; i<count-1; i++) {

                    BgReading reading = readings.get(i);
                    BgReading nextReading = readings.get(i+1);

                    final JSONObject item = new JSONObject();
                    if (!brief) {
                        if (reading._id!=null) {
                            item.put("_id", reading._id);
                        } else {
                            item.put("_id", UUID.randomUUID());
                        }
                        item.put("device", "AndroidAPS-CGMS");
                        item.put("dateString", WebServerUtil.toNightscoutFormat(reading.date));
                        item.put("sysTime", WebServerUtil.toNightscoutFormat(reading.date));
                    }

                    item.put("date", reading.date);
                    item.put("sgv", (int) reading.value);
                    item.put("delta", WebServerUtil.getDelta(reading, nextReading));
                    item.put("direction", reading.direction);
                    item.put("noise", 1);

                    if (!brief) {
                        item.put("filtered", (long) (reading.raw * 1000));
                        item.put("unfiltered", (long) (reading.raw * 1000));
                        item.put("rssi", 100);
                        item.put("type", "sgv");
                    }

                    reply.put(item);
                }

            } catch (JSONException e) {
                LOGGER.error("Got json exception: " + e);
            }
        }

        // whether to send empty string instead of empty json array
        if (cgi.containsKey("no_empty") && reply.length() == 0) {
            LOGGER.debug("Sgv Data: Empty response");
            return new WebResponse("");
        } else {
            LOGGER.debug("Sgv Data: " + reply.toString());
            return new WebResponse(reply.toString());
        }
    }


//    {
//            "type": "string",
//            "dateString": "string",
//            "date": 0,
//            "sgv": 0,
//            "direction": "string",
//            "noise": 0,
//            "filtered": 0,
//            "unfiltered": 0,
//            "rssi": 0
//    }

//    {
//        "_id": "5e0090b95b8c92c792631ac4",
//            "device": "xDrip-DexcomG5",
//            "date": 1577095350649,
//            "dateString": "2019-12-23T10:02:30.649Z",
//            "sgv": 134,
//            "delta": 4.687,
//            "direction": "Flat",
//            "type": "sgv",
//            "filtered": 117368,
//            "unfiltered": 128418,
//            "rssi": 100,
//            "noise": 1,
//            "sysTime": "2019-12-23T10:02:30.649Z",
//            "utcOffset": 60
//    },



//    {
//            "_id": "5dffe6cda294e572bcbe0f33",
//            "device": "xDrip-DexcomG5",
//            "date": 1577051851333,
//            "dateString": "2019-12-22T21:57:31.333Z",
//            "sgv": 296,
//            "delta": 3.398,
//            "direction": "Flat",
//            "type": "sgv",
//            "filtered": 321504,
//            "unfiltered": 325040,
//            "rssi": 100,
//            "noise": 1,
//            "sysTime": "2019-12-22T21:57:31.333Z",
//            "utcOffset": 60
//    },

}
