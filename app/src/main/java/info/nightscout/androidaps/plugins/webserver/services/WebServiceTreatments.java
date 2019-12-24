package info.nightscout.androidaps.plugins.webserver.services;



import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.plugins.treatments.Treatment;
import info.nightscout.androidaps.plugins.treatments.TreatmentsPlugin;
import info.nightscout.androidaps.plugins.webserver.data.TreatmentObject;
import info.nightscout.androidaps.plugins.webserver.data.WebResponse;
import info.nightscout.androidaps.plugins.webserver.util.WebServerUtil;


/**
 * Created by andyrozman on 24/12/2019.
 *
 * Added Treatments data
 */

public class WebServiceTreatments extends BaseWebService {

    //private static String TAG = "WebServiceSgv";
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceTreatments.class);


    // process the request and produce a response object
    public WebResponse request(String query) {


        boolean brief = false; // whether to cut out portions of the data

        final Map<String, String> cgi = getQueryParameters(query);

        LOGGER.debug("Treatments: [queryParameters={}]", cgi);

        int count = 24;

        // for last 24 hours

        GregorianCalendar gc = new GregorianCalendar();
        gc.add(Calendar.HOUR_OF_DAY, -24);

        long millis = gc.getTimeInMillis();

        List<TreatmentObject> allTreatments = new ArrayList<>();

        List<TemporaryBasal> temporaryBasalsSource = MainApp.getDbHelper().getTemporaryBasalsDataFromTime(millis, false);

        for (TemporaryBasal temporaryBasal : temporaryBasalsSource) {
            allTreatments.add(WebServerUtil.convert(temporaryBasal));
        }

        List<Treatment> treatmentsSource = TreatmentsPlugin.getPlugin().getTreatmentsFromHistoryAfterTimestamp(millis);

        for (Treatment treatment : treatmentsSource) {
            allTreatments.add(WebServerUtil.convert(treatment));
        }



        // TODO bolus

        // TODO tbr

        final JSONArray reply = new JSONArray();

        // whether to include data which doesn't match the current sensor
        //final boolean ignore_sensor = Home.get_follower() || cgi.containsKey("all_data");

        for (TreatmentObject allTreatment : allTreatments) {

            final JSONObject item = new JSONObject();



        }






//        List<BgReading> readings = MainApp.getDbHelper().getBgreadingsDataByCount(count, false);
//
//        //final List<BgReading> readings = BgReading.latest(count, ignore_sensor);
//        if (readings != null) {
//            // populate json structures
//            try {
//
//                //final String collector_device = DexCollectionType.getBestCollectorHardwareName();
//                //String external_status_line = getLastStatusLine();
//
//                // for each reading produce a json record
//                for (BgReading reading : readings) {
//                    final JSONObject item = new JSONObject();
//                    if (!brief) {
//                        item.put("_id", reading.uuid);
//                        item.put("device", collector_device);
//                        item.put("dateString", DateUtil.toNightscoutFormat(reading.timestamp));
//                        item.put("sysTime", DateUtil.toNightscoutFormat(reading.timestamp));
//                    }
//
//                    item.put("date", reading.date);
//                    item.put("sgv", (int) reading.getDg_mgdl());
//                    try {
//                        item.put("delta", new BigDecimal(reading.getDg_slope() * 5 * 60 * 1000).setScale(3, BigDecimal.ROUND_HALF_UP));
//                    } catch (NumberFormatException e) {
//                        UserError.Log.e(TAG, "Could not pass delta to webservice as was invalid number");
//                    }
//                    item.put("direction", reading.getDg_deltaName());
//                    item.put("noise", reading.noiseValue());
//
//                    if (!brief) {
//                        item.put("filtered", (long) (reading.filtered_data * 1000));
//                        item.put("unfiltered", (long) (reading.raw_data * 1000));
//                        item.put("rssi", 100);
//                        item.put("type", "sgv");
//                    }
//                    if (units_indicator > 0) {
//                        item.put("units_hint", Pref.getString("units", "mgdl").equals("mgdl") ? "mgdl" : "mmol");
//                        units_indicator = 0;
//                    }
//
//
//
//                    reply.put(item);
//                }
//
//                Log.d(TAG, "Output: " + reply.toString());
//            } catch (JSONException e) {
//                UserError.Log.wtf(TAG, "Got json exception: " + e);
//            }
//        }

        // whether to send empty string instead of empty json array
        if (cgi.containsKey("no_empty") && reply.length() == 0) {
            return new WebResponse("");
        } else {
            return new WebResponse(reply.toString());
        }
    }


//    {
//        "_id": "5e00d3ab5b04c112453a4202",
//        "eventType": "Temp Basal",
//        "duration": 30,
//        "percent": 360,
//        "rate": 4.37,
//        "created_at": "2019-12-23T14:48:10Z",
//        "enteredBy": "openaps://AndroidAPS",
//        "NSCLIENT_ID": 1577112490278,
//        "carbs": null,
//        "insulin": null
//    },

//    {
//        "_id": "5e00c4b15b04c112453a41e2",
//        "eventType": "Bolus Wizard",
//        "insulin": 1.1,
//        "created_at": "2019-12-23T13:43:10Z",
//        "date": 1577108590000,
//        "isSMB": false,
//        "pumpId": 1577108590000,
//        "glucose": 11.1,
//        "glucoseType": "Manual",
//        "boluscalc": {
//              "profile": "Andy(100%,1h)",
//              "notes": "",
//              "eventTime": "2019-12-23T13:43:48Z",
//              "targetBGLow": 5.5,
//              "targetBGHigh": 5.5,
//              "isf": 1.1,
//              "ic": 6,
//        "iob": 4.014,
//        "bolusiob": -0.056,
//        "basaliob": -3.958,
//        "bolusiobused": true,
//        "basaliobused": true,
//        "bg": 11.1,
//        "insulinbg": 5.09090909090909,
//        "insulinbgused": true,
//        "bgdiff": 5.6,
//        "insulincarbs": 0,
//        "carbs": 0,
//        "cob": 0,
//        "cobused": false,
//        "insulincob": 0,
//        "othercorrection": 0,
//        "insulinsuperbolus": 0,
//              "insulintrend": 0,
//              "insulin": 1.1,
//              "superbolusused": false,
//              "trendused": false,
//              "trend": "",
//              "ttused": false
//          },
//        "NSCLIENT_ID": 1577108656682,
//        "carbs": null
//    },


}
