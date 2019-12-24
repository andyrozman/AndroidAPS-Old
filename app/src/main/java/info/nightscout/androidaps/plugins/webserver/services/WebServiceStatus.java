package info.nightscout.androidaps.plugins.webserver.services;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.plugins.webserver.data.WebResponse;
import info.nightscout.androidaps.plugins.webserver.util.WebServerUtil;


/**
 * Created by jamorham on 04/02/2018.
 *
 *  Refactored to work with AndroidAPS database, and changed some data handling.
 *
 */

public class WebServiceStatus extends BaseWebService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceStatus.class);

    // process the request and produce a response object
    public WebResponse request(String query) {
        final JSONObject reply = new JSONObject();

        // TODO missing a lot of data, need to see what need to be added and what can be ignored

        // populate json structures
        try {
            // "settings":{"units":"mmol"}
            final JSONObject settings = new JSONObject();
            final boolean using_mgdl = WebServerUtil.getString("units", "mgdl").equals("mgdl");
            settings.put("units", using_mgdl ? "mg/dl" : "mmol");

            // thresholds":{"bgHigh":260,"bgTargetTop":180,"bgTargetBottom":80,"bgLow":55}
            double highMark = WebServerUtil.tolerantParseDouble(WebServerUtil.getString("highValue", "170"), 170d);
            double lowMark = WebServerUtil.tolerantParseDouble(WebServerUtil.getString("lowValue", "70"), 70d);

            if (!using_mgdl) {
                // if we're using mmol then the marks will be in mmol but should be expressed in mgdl
                // to be in line with how Nightscout presents data
                highMark = WebServerUtil.roundDouble(highMark * WebServerUtil.MMOLL_TO_MGDL, 0);
                lowMark = WebServerUtil.roundDouble(lowMark * WebServerUtil.MMOLL_TO_MGDL, 0);
            }

            final JSONObject thresholds = new JSONObject();
            thresholds.put("bgHigh", highMark);
            thresholds.put("bgLow", lowMark);

            settings.put("thresholds", thresholds);

            reply.put("settings", settings);

            LOGGER.debug("Output: " + reply.toString());
        } catch (JSONException e) {
            LOGGER.error("Got json exception: " + e);
        }

        LOGGER.debug("Status Data: " + reply.toString());

        return new WebResponse(reply.toString());
    }


//    {
//            "status": "ok",
//            "name": "nightscout",
//            "version": "0.12.5",
//            "serverTime": "2019-12-23T15:51:40.950Z",
//            "serverTimeEpoch": 1577116300950,
//            "apiEnabled": true,
//            "careportalEnabled": true,
//            "boluscalcEnabled": true,
//            "settings": {
//                "units": "mmol",
//                "timeFormat": 24,
//                "nightMode": false,
//                "editMode": "on",
//                "showRawbg": "always",
//                "customTitle": "AndysComboLoop-Nightscout",
//                "theme": "colors",
//                "alarmUrgentHigh": false,
//                "alarmUrgentHighMins": [
//        30,
//                60,
//                90,
//                120
//        ],
//        "alarmHigh": false,
//                "alarmHighMins": [
//        30,
//                60,
//                90,
//                120
//        ],
//        "alarmLow": false,
//                "alarmLowMins": [
//        15,
//                30,
//                45,
//                60
//        ],
//        "alarmUrgentLow": false,
//                "alarmUrgentLowMins": [
//        15,
//                30,
//                45
//        ],
//        "alarmUrgentMins": [
//        30,
//                60,
//                90,
//                120
//        ],
//        "alarmWarnMins": [
//        30,
//                60,
//                90,
//                120
//        ],
//        "alarmTimeagoWarn": false,
//                "alarmTimeagoWarnMins": 15,
//                "alarmTimeagoUrgent": false,
//                "alarmTimeagoUrgentMins": 30,
//                "alarmPumpBatteryLow": false,
//                "language": "en",
//                "scaleY": "linear",
//                "showPlugins": "careportal cage basal iob sage openaps pump rawbg pushover bgi iage cob food boluscalc delta direction upbat rawbg",
//                "showForecast": "ar2 openaps",
//                "focusHours": 3,
//                "heartbeat": "60",
//                "baseURL": "",
//                "authDefaultRoles": "readable",
//                "thresholds": {
//            "bgHigh": 180,
//                    "bgTargetTop": 144,
//                    "bgTargetBottom": 90,
//                    "bgLow": 72
//        },
//        "insecureUseHttp": true,
//                "secureHstsHeader": false,
//                "secureHstsHeaderIncludeSubdomains": false,
//                "secureHstsHeaderPreload": false,
//                "secureCsp": false,
//                "showClockClosebutton": true,
//                "deNormalizeDates": false,
//                "DEFAULT_FEATURES": [
//        "bgnow",
//                "delta",
//                "direction",
//                "timeago",
//                "devicestatus",
//                "upbat",
//                "errorcodes",
//                "profile"
//        ],
//        "alarmTypes": [
//        "simple"
//        ],
//        "enable": [
//        "careportal",
//                "cage",
//                "basal",
//                "iob",
//                "sage",
//                "openaps",
//                "pump",
//                "rawbg",
//                "pushover",
//                "bgi",
//                "iage",
//                "cob",
//                "food",
//                "boluscalc",
//                "treatmentnotify",
//                "bgnow",
//                "delta",
//                "direction",
//                "timeago",
//                "devicestatus",
//                "errorcodes",
//                "profile",
//                "simplealarms"
//        ]
//    },
//        "extendedSettings": {
//        "pump": {
//            "fields": "reservoir battery clock"
//        },
//        "openaps": {
//            "fields": "status-symbol status-label iob meal-assist freq rssi",
//                    "colorPredictionLines": true
//        },
//        "cage": {
//            "display": "days",
//                    "info": 60,
//                    "urgent": 72,
//                    "warn": 84
//        },
//        "sage": {
//            "info": 420,
//                    "urgent": 720,
//                    "warn": 504
//        },
//        "basal": {
//            "render": "icicle"
//        },
//        "devicestatus": {
//            "advanced": true
//        }
//    },
//        "authorized": null
//    }

// FULL
//    {
//        "status": "ok",
//            "name": "nightscout",
//            "version": "0.12.5",
//            "serverTime": "2019-12-23T15:51:40.950Z",
//            "serverTimeEpoch": 1577116300950,
//            "apiEnabled": true,
//            "careportalEnabled": true,
//            "boluscalcEnabled": true,
//            "settings": {
//                "units": "mmol",
//                "timeFormat": 24,
//                "nightMode": false,
//                "editMode": "on",
//                "showRawbg": "always",
//                "customTitle": "AndysComboLoop-Nightscout",
//                "theme": "colors",
//                "alarmUrgentHigh": false,
//                "alarmUrgentHighMins": [
//                        30,
//                        60,
//                        90,
//                        120
//                ],
//        "alarmHigh": false,
//                "alarmHighMins": [
//        30,
//                60,
//                90,
//                120
//        ],
//        "alarmLow": false,
//                "alarmLowMins": [
//        15,
//                30,
//                45,
//                60
//        ],
//        "alarmUrgentLow": false,
//                "alarmUrgentLowMins": [
//        15,
//                30,
//                45
//        ],
//        "alarmUrgentMins": [
//        30,
//                60,
//                90,
//                120
//        ],
//        "alarmWarnMins": [
//        30,
//                60,
//                90,
//                120
//        ],
//        "alarmTimeagoWarn": false,
//                "alarmTimeagoWarnMins": 15,
//                "alarmTimeagoUrgent": false,
//                "alarmTimeagoUrgentMins": 30,
//                "alarmPumpBatteryLow": false,
//                "language": "en",
//                "scaleY": "linear",
//                "showPlugins": "careportal cage basal iob sage openaps pump rawbg pushover bgi iage cob food boluscalc delta direction upbat rawbg",
//                "showForecast": "ar2 openaps",
//                "focusHours": 3,
//                "heartbeat": "60",
//                "baseURL": "",
//                "authDefaultRoles": "readable",
//                "thresholds": {
//            "bgHigh": 180,
//                    "bgTargetTop": 144,
//                    "bgTargetBottom": 90,
//                    "bgLow": 72
//        },
//        "insecureUseHttp": true,
//                "secureHstsHeader": false,
//                "secureHstsHeaderIncludeSubdomains": false,
//                "secureHstsHeaderPreload": false,
//                "secureCsp": false,
//                "showClockClosebutton": true,
//                "deNormalizeDates": false,
//                "DEFAULT_FEATURES": [
//        "bgnow",
//                "delta",
//                "direction",
//                "timeago",
//                "devicestatus",
//                "upbat",
//                "errorcodes",
//                "profile"
//        ],
//        "alarmTypes": [
//        "simple"
//        ],
//        "enable": [
//        "careportal",
//                "cage",
//                "basal",
//                "iob",
//                "sage",
//                "openaps",
//                "pump",
//                "rawbg",
//                "pushover",
//                "bgi",
//                "iage",
//                "cob",
//                "food",
//                "boluscalc",
//                "treatmentnotify",
//                "bgnow",
//                "delta",
//                "direction",
//                "timeago",
//                "devicestatus",
//                "errorcodes",
//                "profile",
//                "simplealarms"
//        ]
//    },
//        "extendedSettings": {
//        "pump": {
//            "fields": "reservoir battery clock"
//        },
//        "openaps": {
//            "fields": "status-symbol status-label iob meal-assist freq rssi",
//                    "colorPredictionLines": true
//        },
//        "cage": {
//            "display": "days",
//                    "info": 60,
//                    "urgent": 72,
//                    "warn": 84
//        },
//        "sage": {
//            "info": 420,
//                    "urgent": 720,
//                    "warn": 504
//        },
//        "basal": {
//            "render": "icicle"
//        },
//        "devicestatus": {
//            "advanced": true
//        }
//    },
//        "authorized": null
//    }




}
