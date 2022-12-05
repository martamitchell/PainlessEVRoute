package com.example.android.painlessevroute;

public class Junction {
    private final double mLatitude;
    private final double mLongitude;
    private final long mJunctionId;

    public Junction(double latitude, double longitude, long junctionId) {
        mLatitude = latitude;
        mLongitude = longitude;
        mJunctionId = junctionId;
    }
    public double getLongitude() {
        return mLongitude;
    }
    public double getLatitude() {
        return mLatitude;
    }
    public double getJunctionId() {
        return mJunctionId;
    }
}


