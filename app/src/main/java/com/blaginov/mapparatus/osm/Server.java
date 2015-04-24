package com.blaginov.mapparatus.osm;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.blaginov.mapparatus.exception.OsmServerException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Created by stefanblag on 16.03.15.
 */
public class Server {
    private static final int TIMEOUT = 45 * 1000;
    private final String USER_AGENT = "Mapparatus";
    private final String SERVER_URL = "http://api.openstreetmap.org/api/0.6/";

    /**
     * @param \area
     * @return
     * @throws IOException
     * @throws \OsmServerException
     */
    public InputStream getStreamForBox(final BoundingBox box) throws OsmServerException, IOException {
        Log.d("Server", "getStreamForBox");
        URL url = new URL(SERVER_URL  + "map?bbox=" + box.toApiString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        boolean isServerGzipEnabled = false;

        Log.d("Server", "getStreamForBox " + url.toString());

        //--Start: header not yet send
        con.setReadTimeout(TIMEOUT);
        con.setConnectTimeout(TIMEOUT);
        con.setRequestProperty("Accept-Encoding", "gzip");
        con.setRequestProperty("User-Agent", USER_AGENT);

        //--Start: got response header
        isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));

        // retry if we have no response-code
        if (con.getResponseCode() == -1) {
            Log.w(getClass().getName()+ ":getStreamForBox", "no valid http response-code, trying again");
            con = (HttpURLConnection) url.openConnection();
            //--Start: header not yet send
            con.setReadTimeout(TIMEOUT);
            con.setConnectTimeout(TIMEOUT);
            con.setRequestProperty("Accept-Encoding", "gzip");
            con.setRequestProperty("User-Agent", USER_AGENT);

            //--Start: got response header
            isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
        }

        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            if (con.getResponseCode() == 400) {
                Log.e("OSM Server", "400");
            }
            else {
                Log.e("OSM Server", con.getResponseMessage());
            }
            throw new OsmServerException(con.getResponseCode(), "The API server does not except the request: " + con
                    + ", response code: " + con.getResponseCode() + " \"" + con.getResponseMessage() + "\"");
        }

        if (isServerGzipEnabled) {
            return new GZIPInputStream(con.getInputStream());
        } else {
            return con.getInputStream();
        }
    }
}
