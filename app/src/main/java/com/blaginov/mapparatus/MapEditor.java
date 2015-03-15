package com.blaginov.mapparatus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.blaginov.mapparatus.util.oauth.OAuthHelper;

import java.io.UnsupportedEncodingException;


public class MapEditor extends ActionBarActivity {
    private SharedPreferences sharedPrefs;
    private OAuthHelper oAuth;

    // To be used once the app is ready for production well
    //private final String consKeyO = "LOzBvclzt3FQjEK9ZJexNzgINUoYe43giLzRV2Va";
    //private final String consSecO = "qdv36SIwNkpJuJjgvl7RvPZuJbDdnHPNe0mojiLI";
    //private final String urlBaseO = "https://www.openstreetmap.org/";
    private final String consKeyD = "CNFPvywkyDrqEaA6CiBgdhLxhaRJgRdHdV146pR1";
    private final String consSecD = "7XQE30u7qeKQpVp0sT4ZRyfsS5UWoQrOtyUyG7md";
    private final String urlBaseD = "http://api06.dev.openstreetmap.org/";
    private final String callback = "mapparatus://oauth/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_editor);

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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void osmLogin(MenuItem item) {
        startActivity(new Intent("android.intent.action.VIEW", Uri.parse(oAuth.getRequestToken())));
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
