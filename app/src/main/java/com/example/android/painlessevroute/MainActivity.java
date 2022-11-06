package com.example.android.painlessevroute;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.gestures.Utils;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.api.matching.v5.MapboxMapMatching;
import com.mapbox.api.matching.v5.models.MapMatchingMatching;
import com.mapbox.api.matching.v5.models.MapMatchingResponse;
import com.mapbox.bindgen.Expected;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.extension.style.layers.generated.LineLayer;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.route.NavigationRoute;
import com.mapbox.navigation.base.route.NavigationRouterCallback;
import com.mapbox.navigation.base.route.RouterFailure;
import com.mapbox.navigation.base.route.RouterOrigin;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.MapboxNavigationProvider;
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer;
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView;
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView;
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions;
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError;
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Route>, Callback<MapMatchingResponse>, NavigationRouterCallback, MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> {

    public static final int ROUTE_LOADER_ID = 1;
    public static final String REQUEST_URL =

            "https://maps.brewingbeer.ca/api/route";



    MapboxMap mapboxMap;
    MapboxNavigation mapboxNavigation;
    MapboxRouteLineApi routeLineApi;
    MapboxRouteLineView routeLineView;
    MapboxRouteArrowView routeArrowView;

    @Override
    @SuppressLint("MissingPermission")

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        FloatingActionButton optionsButton = (FloatingActionButton) findViewById(R.id.toggleOptions);
        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });



        //initialize mapbox map
        mapboxMap = ((MapView)findViewById(R.id.mapView)).getMapboxMap();

        if( MapboxNavigationProvider.isCreated()) {
            mapboxNavigation = MapboxNavigationProvider.retrieve();
        } else {
            NavigationOptions navigationOptions = new NavigationOptions.Builder(this)
                    .accessToken(getString(R.string.mapbox_access_token))
                    .build();
            mapboxNavigation = MapboxNavigationProvider.create(navigationOptions);
        }

        MapboxRouteLineOptions mapboxRouteLineOptions = new MapboxRouteLineOptions.Builder(this)
                .withRouteLineBelowLayerId("road-label")
                .build();
        routeLineApi = new MapboxRouteLineApi(mapboxRouteLineOptions);
        routeLineView = new MapboxRouteLineView(mapboxRouteLineOptions);

        RouteArrowOptions routeArrowOptions = new RouteArrowOptions.Builder(this).build();
        routeArrowView = new MapboxRouteArrowView(routeArrowOptions);

        mapboxNavigation.startTripSession();
        //done initializing mapbox map

        //loading a route to display
        LoaderManager loaderManager = getLoaderManager();
        //calls onCreateLoader
        loaderManager.initLoader(ROUTE_LOADER_ID, null, this);

    }



    @Override
    public Loader<Route> onCreateLoader(int id, Bundle args) {
        //we have url and we call RouteLoader class
        return new RouteLoader(this, REQUEST_URL, PreferenceManager.getDefaultSharedPreferences(this));
    }

    //once RouteLoader is done, it will call onLoadFinished
    //we get the list of points and we pass that to mapbox's matching API
    //to convert it to a line (to connect the points)
    @Override
    public void onLoadFinished(Loader<Route> loader, Route data) {
        List<Point> coordinates = data.getJunctions();//.subList(0,24);
        MapboxMapMatching mapboxMapMatchingRequest = MapboxMapMatching.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .coordinates(coordinates)
                .waypointIndices(0,coordinates.size()-1)
                .tidy(true)
                .steps(true)
                .voiceInstructions(true)
                .bannerInstructions(true)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .build();
        mapboxMapMatchingRequest.enqueueCall(this);
    }

    @Override
    public void onLoaderReset(Loader<Route> loader) {
    }

    //when enqueueCall is done we end up here:
    //the reponse of this is a list of line segments and we tell mapbox to display the lines
    @Override
    public void onResponse(Call<MapMatchingResponse> call, Response<MapMatchingResponse> response) {
        if (response.isSuccessful()) {
            MapMatchingResponse body = response.body();
            if(body == null) {
                return;
            }

            List<RouteLine> routeLines = new ArrayList<>();
            for (MapMatchingMatching matching: body.matchings()) {
                routeLines.add(new RouteLine(matching.toDirectionRoute(), null));
            }

            MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> self = this;
            routeLineApi.clearRouteLine( new MapboxNavigationConsumer<Expected<RouteLineError, RouteLineClearValue>>() {
                @Override
                public void accept(Expected<RouteLineError, RouteLineClearValue> routeLineErrorRouteLineClearValueExpected) {
                    routeLineView.renderClearRouteLineValue(mapboxMap.getStyle(), routeLineErrorRouteLineClearValueExpected);
                    routeLineApi.setRoutes(routeLines, self);
                }
            });
            //mapboxNavigation.requestRoutes(body.matchings().get(0).toDirectionRoute().routeOptions(), this);

        }
    }

    @Override
    public void onFailure(Call<MapMatchingResponse> call, Throwable t) {

    }

    @Override
    public void onCanceled(@NonNull RouteOptions routeOptions, @NonNull RouterOrigin routerOrigin) {

    }

    @Override
    public void onFailure(@NonNull List<RouterFailure> list, @NonNull RouteOptions routeOptions) {

    }

    @Override
    public void onRoutesReady(@NonNull List<NavigationRoute> routes, @NonNull RouterOrigin routerOrigin) {
        mapboxNavigation.setNavigationRoutes(routes);
    }

    @Override
    public void accept(Expected<RouteLineError, RouteSetValue> routeLineErrorRouteSetValueExpected) {
        routeLineView.renderRouteDrawData(mapboxMap.getStyle(), routeLineErrorRouteSetValueExpected);
        routeLineView.hideTraffic(mapboxMap.getStyle());
    }
}
