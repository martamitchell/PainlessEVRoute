package com.example.android.painlessevroute;

import com.mapbox.geojson.Point;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;


public class QueryUtils {

    //we send request to backend
    //we get a response, parse JSON data into a Route instance
    //return Route
    public static Route fetchRoute(String stringUrl) {
        if (stringUrl == null) {
            return null;
        }

        URL url = createUrl(stringUrl);
        String jsonResponse = makeHttpRequest(url);
        return extractDataFromJson(jsonResponse);
    }

    private static URL createUrl(String stringUrl) {
        URL url = null;
        if (stringUrl == null) {
            return null;
        }

        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }


    private static String makeHttpRequest(URL url) {
        HttpURLConnection urlConnection = null;
        String jsonResponse = "";
        InputStream inputStream = null;

        if (url == null) {
            return null;
        }

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jsonResponse;
    }

    private static String readFromStream(InputStream inputStream) {
        InputStreamReader streamReader = null;
        StringBuilder result = new StringBuilder();
        BufferedReader bufferedReader = null;

        streamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        bufferedReader = new BufferedReader(streamReader);

        try {
            String line = bufferedReader.readLine();
            while (line != null) {
                result.append(line);
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    private static Route extractDataFromJson(String jsonResponse) {

        Route route = new Route();

        try {

            JSONObject json = new JSONObject(jsonResponse);

            JSONArray junctions = json.getJSONArray("junctions");

            for (int i = 0; i < junctions.length(); i++) {

                JSONObject junction = junctions.getJSONObject(i);
                double latitude = junction.getDouble("latitude");
                double longitude = junction.getDouble("longitude");


                Point j = Point.fromLngLat(longitude, latitude);
                route.addJunction(j);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return route;
    }
}