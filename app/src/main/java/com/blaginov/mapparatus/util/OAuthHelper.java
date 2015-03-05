package com.blaginov.mapparatus.util;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import android.app.Application;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import com.blaginov.mapparatus.R;
import com.blaginov.mapparatus.exception.OsmException;
//import com.blaginov.mapparatus.exception.OsmException;

/**
 * Helper class for signpost OAuth more or less based on text below
 *
 * @author http://nilvec.com/implementing-client-side-oauth-on-android.html
 * @author Stefan Blaginov
 */
public class OAuthHelper {
    private static OAuthConsumer sConsumer;
    private static OAuthProvider sProvider;
    private static String sCallbackUrl;

    /* Not using this for now
    public OAuthHelper(String osmBaseUrl) throws OsmException {
        Resources r = Application.mainActivity.getResources();
        String urls[] = r.getStringArray(R.array.api_urls);
        String keys[] = r.getStringArray(R.array.api_consumer_keys);
        String secrets[] = r.getStringArray(R.array.api_consumer_secrets);
        String oauth_urls[] = r.getStringArray(R.array.api_oauth_urls);

        for (int i = 0; i < urls.length; i++) {
            if (urls[i].equalsIgnoreCase(osmBaseUrl)) {
                sConsumer = new CommonsHttpOAuthConsumer(keys[i], secrets[i]);
                Log.d("OAuthHelper", "Using " + osmBaseUrl + "oauth/request_token " + osmBaseUrl + "oauth/access_token " + osmBaseUrl + "oauth/authorize");
                Log.d("OAuthHelper", "With key " + keys[i] + " secret " + secrets[i]);
                sProvider = new CommonsHttpOAuthProvider(
                        oauth_urls[i] + "oauth/request_token",
                        oauth_urls[i] + "oauth/access_token",
                        oauth_urls[i] + "oauth/authorize");
                sProvider.setOAuth10a(true);
                sCallbackUrl = "vespucci://oauth/"; //OAuth.OUT_OF_BAND; //
                return;
            }
        }

        Log.d("OAuthHelper", "No matching API for " + osmBaseUrl + "found");
        throw new OsmException("No matching OAuth configuration found for this API");
    }*/

    public OAuthHelper(String osmBaseUrl, String consumerKey, String consumerSecret, String callbackUrl) throws UnsupportedEncodingException {
        Log.i("consumerKey",consumerKey);
        Log.i("consumerSecret",consumerSecret);
        sConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
        sProvider = new CommonsHttpOAuthProvider(
                osmBaseUrl + "oauth/request_token",
                osmBaseUrl + "oauth/access_token",
                osmBaseUrl + "oauth/authorize");
        sProvider.setOAuth10a(true);
        sCallbackUrl = (callbackUrl == null ? OAuth.OUT_OF_BAND : callbackUrl);
    }

    /**
     * this constructor is for access to the singletons
     */
    public OAuthHelper() {
    }

    /**
     * Returns an OAuthConsumer initialized with the consumer keys for the API in question
     *
     * @param osmBaseUrl
     * @return
     */
    /* Not for now
    public OAuthConsumer getConsumer(String osmBaseUrl) {
        Resources r = Application.mainActivity.getResources();

        String urls[] = r.getStringArray(R.array.api_urls);
        String keys[] = r.getStringArray(R.array.api_consumer_keys);
        String secrets[] = r.getStringArray(R.array.api_consumer_secrets);
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].equalsIgnoreCase(osmBaseUrl)) {
                return new DefaultOAuthConsumer(keys[i], secrets[i]);
            }
        }
        Log.d("OAuthHelper", "No matching API for " + osmBaseUrl + "found");
        // TODO: protect against failure
        return null;
    }*/

    /**
     * @return null if fails
     */
    public String getRequestToken() {
        class MyTask extends AsyncTask<Void, Void, String> {
            String result;

            @Override
            protected void onPreExecute() {
                Log.d("Main", "oAuthHandshake onPreExecute");
            }

            @Override
            protected String doInBackground(Void... params) {

                try {
                    String authUrl = sProvider.retrieveRequestToken(sConsumer, sCallbackUrl);
                    Log.i("consumer token",sConsumer.getToken());
                    Log.i("consumer token secret",sConsumer.getTokenSecret());
                    Log.i("consumer key",sConsumer.getConsumerKey());
                    Log.i("consumer secret",sConsumer.getConsumerSecret());
                    Log.i("provider request token",authUrl);
                    return authUrl;
                } catch (Exception e) {
                    Log.d("Main", "OAuth handshake failed");
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String authUrl) {
                Log.d("Main", "oAuthHandshake onPostExecute");
                result = authUrl;
            }
        };

        MyTask loader = new MyTask();
        loader.execute();
        try {
            return loader.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // TODO: Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO: Auto-generated catch block
            e.printStackTrace();
        } catch (TimeoutException e) {
            // TODO: Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Transforms the verifier string into an access token consisting of a pair of consumer token and secret
     * @param verifier
     * @return A string array containing the consumer's token and secret
     * @throws OAuthMessageSignerException
     * @throws OAuthNotAuthorizedException
     * @throws OAuthExpectationFailedException
     * @throws OAuthCommunicationException
     */
    public String[] getAccessToken(String verifier) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
        Log.d("OAuthHelper", "verifier: " + verifier);

        // If this helper class is not initialised throw an exception
        if (sProvider == null || sConsumer == null) {
            throw new OAuthExpectationFailedException("OAuthHelper not initialized!");
        }

        // Get token from provider
        sProvider.retrieveAccessToken(sConsumer, verifier);

        Log.i("consumer token",sConsumer.getToken());
        Log.i("consumer access key (secret)",sConsumer.getTokenSecret());

        // Return a string array containing the consumer token and secret
        return new String[] {sConsumer.getToken(), sConsumer.getTokenSecret()};
    }

    public void resume(String osmBaseUrl, String consumerKey, String consumerSecret, String requestToken, String tokenSecret, String callbackUrl) {
        //sConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
        sConsumer.setTokenWithSecret(requestToken, tokenSecret);
        //sProvider = new CommonsHttpOAuthProvider(
               // osmBaseUrl + "oauth/request_token",
              //  osmBaseUrl + "oauth/access_token",
              //  osmBaseUrl + "oauth/authorize");
       // sProvider.setOAuth10a(true);
        //sCallbackUrl = (callbackUrl == null ? OAuth.OUT_OF_BAND : callbackUrl);

        Log.i("consumer token n",sConsumer.getToken());
        Log.i("consumer token secret n",sConsumer.getTokenSecret());
        Log.i("consumer key n",sConsumer.getConsumerKey());
        Log.i("consumer secret n",sConsumer.getConsumerSecret());
    }

    public String getConsToken() {
        return sConsumer.getToken();
    }

    public String getConsTokenSecret() {
        return sConsumer.getTokenSecret();
    }
}