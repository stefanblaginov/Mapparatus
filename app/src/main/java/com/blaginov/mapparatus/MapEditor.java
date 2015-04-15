package com.blaginov.mapparatus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.blaginov.mapparatus.osm.Server;
import com.blaginov.mapparatus.util.oauth.OAuthHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;


public class MapEditor extends ActionBarActivity {
    private SharedPreferences sharedPrefs;
    private OAuthHelper oAuth;
    private Server server;

    // To be used once the app is ready for production well
    //private final String consKeyO = "LOzBvclzt3FQjEK9ZJexNzgINUoYe43giLzRV2Va";
    //private final String consSecO = "qdv36SIwNkpJuJjgvl7RvPZuJbDdnHPNe0mojiLI";
    //private final String urlBaseO = "https://www.openstreetmap.org/";
    private final String consKeyD = "CNFPvywkyDrqEaA6CiBgdhLxhaRJgRdHdV146pR1";
    private final String consSecD = "7XQE30u7qeKQpVp0sT4ZRyfsS5UWoQrOtyUyG7md";
    private final String urlBaseD = "http://api06.dev.openstreetmap.org/";
    private final String callback = "mapparatus://oauth/";
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_editor);


        textView = (TextView) findViewById(R.id.textView);
        // Initialise the shared preferences object
        sharedPrefs = getSharedPreferences("mapparatus", 0);

        setOAuthPreferences(consKeyD, consSecD, urlBaseD, callback);
        try {
            oAuth = new OAuthHelper(sharedPrefs);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sharedPrefs.getBoolean("isLoggingIn1", false)) {
            SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
            sharedPrefsEditor.putBoolean("isLoggingIn1", false);
            sharedPrefsEditor.commit();
            // Resume OAuth processing
            oAuth.completeOAuthProcess(this.getIntent().getData());
            Toast toast = Toast.makeText(this, sharedPrefs.getString("accessToken0", ""), Toast.LENGTH_SHORT);
            Toast toast2 = Toast.makeText(this, sharedPrefs.getString("accessToken1", ""), Toast.LENGTH_SHORT);
            toast.show();
            toast2.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_login) {
            return true;
        }
        if (id == R.id.action_download) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void osmLogin(MenuItem item) {
        startActivity(new Intent("android.intent.action.VIEW", Uri.parse(oAuth.getRequestToken())));
    }

    public void osmDownload(MenuItem item) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        class DownloadOsmXml extends AsyncTask<Void, Void, InputStream> {
            @Override
            protected InputStream doInBackground(Void... params) {
                try {
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
                    con.setReadTimeout(4000);
                    con.setConnectTimeout(4000);
                    con.setRequestProperty("Accept-Encoding", "gzip");
                    con.setRequestProperty("User-Agent", "Mapparatus/Unborn");

                    //--Start: got response header
                    isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
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
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        DownloadOsmXml dsf = new DownloadOsmXml();
        dsf.execute();

        InputStream nngn = dsf.get(10, TimeUnit.SECONDS);

        class ReaOsmXml extends AsyncTask<InputStream, String, Void> {
            @Override
            protected Void doInBackground(InputStream... params) {
                try {
                    BufferedReader bff = new BufferedReader(new InputStreamReader(params[0]));
                    TextView textView = (TextView) findViewById(R.id.textView);
                    String line;
                    while ((line = bff.readLine()) != null) {
                        //Log.i("BUFFER", line);

                        publishProgress(line);
                        Thread.sleep(200);
                    }

                    bff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                textView.setText(values[0]);
            }

        }

        new ReaOsmXml().execute(nngn);
    }

    public void openMap(MenuItem item) {
        Intent intent = new Intent(this, Map.class);
        startActivity(intent);
    }

    private void setOAuthPreferences(String consKey, String consSecret, String urlBase, String callbackUrl) {
        SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putString("consKey", consKey);
        sharedPrefsEditor.putString("consSecret", consSecret);
        sharedPrefsEditor.putString("urlBase", urlBase);
        sharedPrefsEditor.putString("callbackUrl", callbackUrl);
        sharedPrefsEditor.commit();
    }
}
