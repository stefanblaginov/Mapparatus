package com.blaginov.mapparatus.osm;

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

    public InputStream getStreamFromBox() throws IOException {
        URL url = null;
        try {
            url = new URL("http://api.openstreetmap.org/api/0.6/map?bbox=11.54,48.14,11.543,48.145");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean isServerGzipEnabled = false;

        //--Start: header not yet send
        con.setReadTimeout(TIMEOUT);
        con.setConnectTimeout(TIMEOUT);
        con.setRequestProperty("Accept-Encoding", "gzip");
        con.setRequestProperty("User-Agent", "Mapparatus/Unborn");

        //--Start: got response header
        isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));

		/*
		// retry if we have no resopnse-code
		try {SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		saxParser.parse(in, this);
			if (con.getResponseCode() == -1) {
				System.out.println( ":getStreamForBox" + "no valid http response-code, trying again");
				con = (HttpURLConnection) url.openConnection();
				//--Start: header not yet send
				con.setReadTimeout(TIMEOUT);
				con.setConnectTimeout(TIMEOUT);
				con.setRequestProperty("Accept-Encoding", "gzip");
				con.setRequestProperty("User-Agent", "Mapparatus/Unborn");

				//--Start: got response header
				isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}*/

        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                System.out.println(con.getResponseCode() + "The API server does not except the request: " + con
                        + ", response code: " + con.getResponseCode() + " \"" + con.getResponseMessage() + "\"");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isServerGzipEnabled) {
            return new GZIPInputStream(con.getInputStream());

        } else {
            return con.getInputStream();
        }
    }
}
