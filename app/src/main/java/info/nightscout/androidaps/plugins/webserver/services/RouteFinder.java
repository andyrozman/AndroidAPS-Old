package info.nightscout.androidaps.plugins.webserver.services;



import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import info.nightscout.androidaps.plugins.webserver.data.WebResponse;


/**
 * Created by jamorham on 17/01/2018.
 *
 * Calls the WebService module associated with the route
 *
 * Refactored to remove some dependencies that are not needed.
 *
 */

public class RouteFinder {

    private final List<RouteInfo> routes = new ArrayList<>();


    public RouteFinder() {

        // support for pebble nightscout watchface emulates /pebble Nightscout endpoint
        routes.add(new RouteInfo("pebble", new WebServicePebble())); //"WebServicePebble"));

        // support for nightscout style sgv.json endpoint
        routes.add(new RouteInfo("sgv.json", new WebServiceSgv())); //"WebServiceSgv"));

        // support for nightscout style barebones status.json endpoint
        routes.add(new RouteInfo("status.json", new WebServiceStatus())); //"WebServiceStatus"));

        // support for nightscout style treatments.json endpoint
        routes.add(new RouteInfo("treatments.json", new WebServiceTreatments()));

    }

    // process a received route
    WebResponse handleRoute(final String route) {
        return handleRoute(route, null);
    }

    // process a received route with source details
    public WebResponse handleRoute(final String route, final InetAddress source) {

        for (final RouteInfo routeEntry : routes) {
            if (route.startsWith(routeEntry.path)) {
                return routeEntry.processRequest(route, source);
            }
        }
        // unknown service error reply
        return new WebResponse("Path not found: " + route + "\r\n", 404, "text/plain");
    }



    private static final class RouteInfo {
        public String path;
        BaseWebService baseWebService;
        boolean raw = false;

        public RouteInfo(String path, BaseWebService baseWebService) {
            this.path = path;
            this.baseWebService = baseWebService;
        }

        RouteInfo useRaw() {
            raw = true;
            return this;
        }

        BaseWebService getService() {
            return baseWebService;
        }

        WebResponse processRequest(final String route, final InetAddress source) {
            try {
                return getService().request(raw ? route : URLDecoder.decode(route, "UTF-8"), source);
            } catch (UnsupportedEncodingException e) {
                return new WebResponse("Decoding error", 500, "text/plain");
            }
        }
    }
}
