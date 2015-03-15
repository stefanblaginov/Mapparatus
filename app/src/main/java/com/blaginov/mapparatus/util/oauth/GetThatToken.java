package com.blaginov.mapparatus.util.oauth;

import android.os.AsyncTask;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

/**
 * Created by stefanblag on 05.03.15.
 */
public class GetThatToken extends AsyncTask<Object, Void, String[]> {
    @Override
    protected String[] doInBackground(Object... objects) {
        String[] result = {null, null};
        OAuthHelper oAuth = (OAuthHelper) objects[0];
        String token = (String) objects[1];
        try {
            result = oAuth.getAccessToken(token);
        } catch (OAuthMessageSignerException e) {
            e.printStackTrace();
        } catch (OAuthNotAuthorizedException e) {
            e.printStackTrace();
        } catch (OAuthExpectationFailedException e) {
            e.printStackTrace();
        } catch (OAuthCommunicationException e) {
            e.printStackTrace();
        }

        return result;
    }
}