package com.blaginov.mapparatus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.blaginov.mapparatus.util.OAuthHelper;

import java.io.UnsupportedEncodingException;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;


public class MapEditor extends ActionBarActivity {
    private OAuthHelper oAuth;
    private final String consKey = "LOzBvclzt3FQjEK9ZJexNzgINUoYe43giLzRV2Va";
    private final String consSec = "qdv36SIwNkpJuJjgvl7RvPZuJbDdnHPNe0mojiLI";
    private final String urlBase = "https://www.openstreetmap.org/";
    private final String consKeyD = "CNFPvywkyDrqEaA6CiBgdhLxhaRJgRdHdV146pR1";
    private final String consSecD = "7XQE30u7qeKQpVp0sT4ZRyfsS5UWoQrOtyUyG7md";
    private final String urlBaseD = "http://api06.dev.openstreetmap.org/";
    private final String callback = "mapparatus://oauth/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_editor);

        try {
            oAuth = new OAuthHelper(urlBase, consKey, consSec, callback);
        } catch (UnsupportedEncodingException e) {
            Log.e("balls", "UnsupportedEncodingException, yo!");
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        String[] token = getVerifier();
        SharedPreferences sharedPref = getSharedPreferences("mapparatus",0);
        Log.i("consToken", sharedPref.getString("consToken",""));
        Log.i("consTokenSecret", sharedPref.getString("consToken",""));
        oAuth.resume(urlBase, consKey, consSec, sharedPref.getString("consToken",""), sharedPref.getString("consTokenSecret",""), callback);

        if (token != null) {
            try {
                String[] accessToken = oAuth.getAccessToken(token[1]);
            } catch (OAuthMessageSignerException e) {
                e.printStackTrace();
            } catch (OAuthNotAuthorizedException e) {
                e.printStackTrace();
            } catch (OAuthExpectationFailedException e) {
                e.printStackTrace();
            } catch (OAuthCommunicationException e) {
                e.printStackTrace();
            }
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

    public void startProcess(View view) {
        String uri = oAuth.getRequestToken();
        //consTok = oAuth.getConsToken();
        //consTokSec = oAuth.getConsTokenSecret();

        SharedPreferences sharedPref = getSharedPreferences("mapparatus",0);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("consToken", oAuth.getConsToken());
        editor.putString("consTokenSecret", oAuth.getConsTokenSecret());
        editor.commit();
        
        startActivity(new Intent("android.intent.action.VIEW", Uri.parse(uri)));
    }
    
    
    
    
    

    private String[] getVerifier() {
        // extract the token if it exists
        Uri uri = this.getIntent().getData();
        if (uri == null) {
            return null;
        }

        String token = uri.getQueryParameter("oauth_token");
        Log.i("token",token);
        String verifier = uri.getQueryParameter("oauth_verifier");
        Log.i("verifier",verifier);
        return new String[] { token, verifier };
    }

}
