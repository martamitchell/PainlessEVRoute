package com.example.android.painlessevroute;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.Loader;
import android.text.Layout;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.api.matching.v5.MapboxMapMatching;
import com.mapbox.api.matching.v5.models.MapMatchingMatching;
import com.mapbox.api.matching.v5.models.MapMatchingResponse;
import com.mapbox.geojson.Point;
import com.mapbox.maps.MapView;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener;
import com.mapbox.navigation.base.route.NavigationRoute;
import com.mapbox.navigation.base.route.NavigationRouterCallback;
import com.mapbox.navigation.base.route.RouterFailure;
import com.mapbox.navigation.base.route.RouterOrigin;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.dropin.NavigationView;
import com.mapbox.navigation.dropin.ViewBinderCustomization;
import com.mapbox.navigation.dropin.ViewOptionsCustomization;
import com.mapbox.navigation.dropin.map.MapViewObserver;
import com.mapbox.navigation.dropin.navigationview.NavigationViewListener;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class NavigationActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Route> {

    public static final int ROUTE_LOADER_ID = 1;
    public static final String REQUEST_URL =

            "https://maps.brewingbeer.ca/api/route";

    private ProgressBar loadingProgress = null;
    private LocationEngine locationEngine = null;

    @Override
    @SuppressLint("MissingPermission")

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation);

        loadingProgress = (ProgressBar) findViewById(R.id.loadingProgress);
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("destinationLongitude");
            editor.remove("destinationLatitude");
            editor.apply();
        }
        //loading a route to display (call to the back-end API)
        LoaderManager loaderManager = getLoaderManager();


        locationEngine = LocationEngineProvider.getBestLocationEngine(getApplicationContext());
        view.registerMapObserver(new MapViewObserver() {
            @Override
            public void onAttached(@NonNull MapView mapView) {
                super.onAttached(mapView);
                mapView.getMapboxMap().gesturesPlugin(new Function1<GesturesPlugin, Object>() {
                    @Override
                    public Object invoke(GesturesPlugin gesturesPlugin) {
                        gesturesPlugin.addOnMapLongClickListener(new OnMapLongClickListener() {
                            @Override
                            public boolean onMapLongClick(@NonNull Point point) {
                                locationEngine.getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
                                    @Override
                                    public void onSuccess(LocationEngineResult currentLocation) {
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putFloat("startLongitude", (float) (currentLocation.getLastLocation().getLongitude()));
                                        editor.putFloat("startLatitude", (float) (currentLocation.getLastLocation().getLatitude()));
                                        editor.putFloat("destinationLongitude", (float) (point.longitude()));
                                        editor.putFloat("destinationLatitude", (float) (point.latitude()));
                                        editor.apply();
                                        loaderManager.getLoader(ROUTE_LOADER_ID).forceLoad();
                                    }

                                    @Override
                                    public void onFailure(@NonNull Exception exception) {

                                    }
                                });

                                return false;
                            }
                        });
                        return null;
                    }
                });
            }
        });

        view.customizeViewOptions(new Function1<ViewOptionsCustomization, Unit>() {
            @Override
            public Unit invoke(ViewOptionsCustomization viewOptionsCustomization) {
                viewOptionsCustomization.setEnableMapLongClickIntercept(false);
                return null;
            }
        });

        //calls onCreateLoader
        loaderManager.initLoader(ROUTE_LOADER_ID, null, this);
    }


    @Override
    public Loader<Route> onCreateLoader(int id, Bundle args) {
        return new RouteLoader(this, REQUEST_URL, PreferenceManager.getDefaultSharedPreferences(this), this);
    }

    public void onLoadStarted() {
        loadingProgress.setVisibility(View.VISIBLE);
    }

    //once RouteLoader is done, it will call onLoadFinished
    //we get the list of points and we pass that to mapbox's matching API
    //to convert it to a line (to connect the points)
    @Override
    public void onLoadFinished(Loader<Route> loader, Route data) {
        loadingProgress.setVisibility(View.GONE);
        NavigationView view = (NavigationView) findViewById(R.id.navigationView);
        view.getApi().routeReplayEnabled(false);

        MapboxNavigation mapboxNavigation = MapboxNavigationApp.current();
        if (mapboxNavigation == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NavigationActivity self = this;

        List<Point> coordinates = data.getJunctions();
        if( coordinates.size() < 2 ) {
            return;
        }
        if( coordinates.size() > 100 ) {
            coordinates = coordinates.subList(0, 99);
        }

        mapboxNavigation.setRoutes(new ArrayList<>());
        MapboxMapMatching client = MapboxMapMatching.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .coordinates(coordinates)
                .profile("driving")
                .steps(true)
                //.voiceInstructions(true)
                //.voiceUnits("metric")
                .bannerInstructions(true)
qafd231q232q                .overview("full")
                .annotations("maxspeed,duration")
                .waypointIndices(0, (coordinates.size() - 1))
                .build();

        client.enqueueCall(new Callback<MapMatchingResponse>() {
            @Override
            public void onResponse(Call<MapMatchingResponse> call, Response<MapMatchingResponse> response) {
                loadingProgress.setVisibility(View.GONE);
                if(response.isSuccessful()) {
                    List<DirectionsRoute> directionsRoutes = new ArrayList<>();
                    MapMatchingMatching matching = response.body().matchings().get(0);
                    directionsRoutes.add(matching.toDirectionRoute());
                    mapboxNavigation.setRoutes(directionsRoutes);
                    //view.getApi().startRoutePreview();
                }
            }

            @Override
            public void onFailure(Call<MapMatchingResponse> call, Throwable t) {
                loadingProgress.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Route> loader) {
        loadingProgress.setVisibility(View.GONE);
    }
}
\]]]///////