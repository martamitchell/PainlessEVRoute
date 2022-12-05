package com.example.android.painlessevroute;

import android.content.Context;
import android.content.AsyncTaskLoader;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.ProgressBar;

import androidx.preference.PreferenceManager;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.geojson.Point;
import com.mapbox.navigation.dropin.NavigationView;

import java.util.List;


public class RouteLoader extends AsyncTaskLoader<Route> {
    private final String mUrl;
    private final SharedPreferences mPreferences;
    private final NavigationActivity mNavigationActivity;

    public RouteLoader(Context context, String url, SharedPreferences preferences, NavigationActivity activity) {
        super(context);
        mUrl = url;
        mPreferences = preferences;
        mNavigationActivity = activity;
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
        Point start = Point.fromLngLat(mPreferences.getFloat("startLongitude", 0), mPreferences.getFloat("startLatitude", 0));
        Point destination = Point.fromLngLat(mPreferences.getFloat("destinationLongitude", 0), mPreferences.getFloat("destinationLatitude", 0));
        uriBuilder.appendQueryParameter("waypoint", String.format("%.10f,%.10f", start.latitude(), start.longitude()));
        uriBuilder.appendQueryParameter("waypoint", String.format("%.10f,%.10f", destination.latitude(), destination.longitude()));

        uriBuilder.appendQueryParameter("length", "1");
        if (mPreferences.getBoolean("pain-unpaved", false)){
            uriBuilder.appendQueryParameter("unpaved", "1");
        }
        if (mPreferences.getBoolean("pain-paved", false)){
            uriBuilder.appendQueryParameter("paved", "1");
        }
        if (mPreferences.getBoolean("pain-tolls", false)){
            uriBuilder.appendQueryParameter("toll_road", "1");
        }
        if (mPreferences.getBoolean("pain-oneway", false)){
            uriBuilder.appendQueryParameter("one_way", "1");
        }
        if (mPreferences.getBoolean("pain-twoway", false)){
            uriBuilder.appendQueryParameter("two_way", "1");
        }
        if (mPreferences.getBoolean("pain-local-speed", false)){
            uriBuilder.appendQueryParameter("local_speed", "1");
        }
        if (mPreferences.getBoolean("pain-city-speed", false)){
            uriBuilder.appendQueryParameter("city_speed", "1");
        }
        if (mPreferences.getBoolean("pain-highway-speed", false)){
            uriBuilder.appendQueryParameter("highway_speed", "1");
        }
        if (mPreferences.getBoolean("pain-freeway-speed", false)){
            uriBuilder.appendQueryParameter("freeway_speed", "1");
        }

        mNavigationActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNavigationActivity.onLoadStarted();
            }
        });

        return QueryUtils.fetchRoute(uriBuilder.toString());
    }
}

