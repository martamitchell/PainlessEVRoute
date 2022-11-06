package com.example.android.painlessevroute;

import android.content.Context;
import android.content.AsyncTaskLoader;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.preference.PreferenceManager;

import java.util.List;


public class RouteLoader extends AsyncTaskLoader<Route> {
    private String mUrl;
    private SharedPreferences mPreferences;

    public RouteLoader(Context context, String url, SharedPreferences preferences) {
        super(context);
        mUrl = url;
        mPreferences = preferences;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public Route loadInBackground() {
        if(mUrl == null) {
            return null;
        }

        Uri baseUri = Uri.parse(mUrl); //takes constant from above and starts uri builder
        Uri.Builder uriBuilder = baseUri.buildUpon();
        //adds startpoint and endpoint, temporary hardcoded will be replaced with user selected values
        uriBuilder.appendQueryParameter("waypoint", "43.46724125189634,-80.52492213948975");
        uriBuilder.appendQueryParameter("waypoint", "43.62674668093105,-80.58130312098332");

        uriBuilder.appendQueryParameter("length", "1");
        if (mPreferences.getBoolean("pain-unpaved", false)){
            uriBuilder.appendQueryParameter("unpaved", "1");
        }
        if (mPreferences.getBoolean("pain-local-speed", false)){
            uriBuilder.appendQueryParameter("local_speed", "1");
        }
        if (mPreferences.getBoolean("pain-city-speed", false)){
            uriBuilder.appendQueryParameter("city_speed", "1");
        }

        return QueryUtils.fetchRoute(uriBuilder.toString());
    }
}

