package com.example.android.painlessevroute;


import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;

//list of points to visit
public class Route {
    private final List<Point> mJunctions = new ArrayList<>();

    public Route() {

    }

    public void addJunction(Point junction){
        //pushing junction on to end of the list
        mJunctions.add(junction);
    }

    public List<Point> getJunctions(){
        return mJunctions;
    }
}