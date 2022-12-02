package com.example.android.painlessevroute;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.Loader;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.navigation.base.route.NavigationRoute;
import com.mapbox.navigation.base.route.NavigationRouterCallback;
import com.mapbox.navigation.base.route.RouterFailure;
import com.mapbox.navigation.base.route.RouterOrigin;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.dropin.NavigationView;
import com.mapbox.navigation.dropin.ViewBinderCustomization;

import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class NavigationActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Route>, NavigationRouterCallback {

    public static final int ROUTE_LOADER_ID = 1;
    public static final String REQUEST_URL =

            "https://maps.brewingbeer.ca/api/route";

    @Override
    @SuppressLint("MissingPermission")

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation);

        NavigationView view = (NavigationView) findViewById(R.id.navigationView);

        // initialize the setting button (lower right side)
        FloatingActionButton optionsButton = (FloatingActionButton) findViewById(R.id.toggleOptions);
        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open the settings activity
                Intent settingsIntent = new Intent(NavigationActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        // initialize the search button
        FloatingActionButton searchButton = (FloatingActionButton) findViewById(R.id.addressSearch);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open the settings activity
                Intent addressSearch = new Intent(NavigationActivity.this, AddressSearch.class);
                startActivity(addressSearch);
            }
        });

        //loading a route to display (call to the back-end API)
        LoaderManager loaderManager = getLoaderManager();
        //calls onCreateLoader
        loaderManager.initLoader(ROUTE_LOADER_ID, null, this);
    }



    @Override
    public Loader<Route> onCreateLoader(int id, Bundle args) {
        return new RouteLoader(this, REQUEST_URL, PreferenceManager.getDefaultSharedPreferences(this));
    }

    //once RouteLoader is done, it will call onLoadFinished
    //we get the list of points and we pass that to mapbox's matching API
    //to convert it to a line (to connect the points)
    @Override
    public void onLoadFinished(Loader<Route> loader, Route data) {
        NavigationView view = (NavigationView) findViewById(R.id.navigationView);
        view.getApi().routeReplayEnabled(false);

        List<Point> coordinates = data.getJunctions();//.subList(0,24);
        MapboxNavigation mapboxNavigation = MapboxNavigationApp.current();
        if(mapboxNavigation == null) {
            return;
        }

        RouteOptions.Builder builder = RouteOptions.builder().coordinatesList(coordinates).profile("driving").steps(true).waypointIndices("0;" + (coordinates.size()-1));
        mapboxNavigation.requestRoutes(builder.build(), this);
    }

    @Override
    public void onLoaderReset(Loader<Route> loader) {

    }

    @Override
    public void onCanceled(@NonNull RouteOptions routeOptions, @NonNull RouterOrigin routerOrigin) {

    }

    @Override
    public void onFailure(@NonNull List<RouterFailure> list, @NonNull RouteOptions routeOptions) {

    }

    @Override
    public void onRoutesReady(@NonNull List<NavigationRoute> list, @NonNull RouterOrigin routerOrigin) {
        NavigationView view = (NavigationView) findViewById(R.id.navigationView);
        view.getApi().routeReplayEnabled(true);
        view.getApi().startActiveGuidance(list);
    }
}
