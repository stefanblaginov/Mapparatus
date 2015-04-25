package com.blaginov.mapparatus.util.oauth;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

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
    private static SharedPreferences sharedPrefs;

    public OAuthHelper(SharedPreferences sharedPrefs) throws UnsupportedEncodingException {
        OAuthHelper.sharedPrefs = sharedPrefs;

        OAuthHelper.sConsumer = new CommonsHttpOAuthConsumer(sharedPrefs.getString("consKey", ""), sharedPrefs.getString("consSecret", ""));
        OAuthHelper.sProvider = new CommonsHttpOAuthProvider(
                sharedPrefs.getString("urlBase", "") + "oauth/request_token",
                sharedPrefs.getString("urlBase", "") + "oauth/access_token",
                sharedPrefs.getString("urlBase", "") + "oauth/authorize");
        OAuthHelper.sProvider.setOAuth10a(true);
        OAuthHelper.sCallbackUrl = (sharedPrefs.getString("callbackUrl", "") == null ? OAuth.OUT_OF_BAND : sharedPrefs.getString("callbackUrl", ""));
    }

    /**
     * @return null if fails
     */
    public String getRequestToken() {
        class GetRequestTokenTask extends AsyncTask<Void, Void, String> {
            String result;

            @Override
            protected void onPreExecute() {
                Log.d("Main", "oAuthHandshake onPreExecute");
            }

            @Override
            protected String doInBackground(Void... params) {

                try {
                    String authUrl = sProvider.retrieveRequestToken(sConsumer, sCallbackUrl);

                    SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
                    sharedPrefsEditor.putString("consToken", sConsumer.getToken());
                    sharedPrefsEditor.putString("consTokenSecret", sConsumer.getTokenSecret());
                    sharedPrefsEditor.putBoolean("isLoggingIn1", true);
                    sharedPrefsEditor.commit();

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
        }

        GetRequestTokenTask loader = new GetRequestTokenTask();
        loader.execute();
        try {
            return loader.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Transforms the verifier string into an access token consisting of a pair of consumer token and secret
     * @return A string array containing the consumer's token and secret
     * @throws OAuthMessageSignerException
     * @throws OAuthNotAuthorizedException
     * @throws OAuthExpectationFailedException
     * @throws OAuthCommunicationException
     */
    public String[] getAccessToken(String verifier) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {

        // If this helper class is not initialised throw an exception
        if (sProvider == null || sConsumer == null) {
            throw new OAuthExpectationFailedException("OAuthHelper not initialized!");
        }

        // Get token from provider
        sProvider.retrieveAccessToken(sConsumer, verifier);

        // Return a string array containing the consumer token and secret
        return new String[] {sConsumer.getToken(), sConsumer.getTokenSecret()};
    }

    public void completeOAuthProcess(Uri uri) {
        sConsumer.setTokenWithSecret(sharedPrefs.getString("consToken", ""), sharedPrefs.getString("consTokenSecret", ""));

        if (getVerifier(uri) != null) {
            try {
                String[] accessToken = new GetThatToken().execute(this, getVerifier(uri)[1]).get();

                SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
                sharedPrefsEditor.putString("accessToken0", accessToken[0]);
                sharedPrefsEditor.putString("accessToken1", accessToken[1]);
                sharedPrefsEditor.commit();

                Log.i("accessToken0", sharedPrefs.getString("accessToken0", ""));
                Log.i("accessToken1", sharedPrefs.getString("accessToken1", ""));
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public OAuthConsumer getOAuthConsumer() {
        return this.sConsumer;
    }

    private String[] getVerifier(Uri uri) {
        // extract the token if it exists
        if (uri == null) {
            return null;
        }

        String token = uri.getQueryParameter("oauth_token");
        String verifier = uri.getQueryParameter("oauth_verifier");
        return new String[] { token, verifier };
    }
}