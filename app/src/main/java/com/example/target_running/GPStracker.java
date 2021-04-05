package com.example.target_running;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class GPStracker implements LocationListener {
    Context context;

    public GPStracker(Context context) {
        this.context = context;
    }
    public Location getLocation(){

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            Toast.makeText(context, "permissons not granted", Toast.LENGTH_SHORT).show();
            return  null;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        boolean isGPSenbled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (isGPSenbled){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000, 1,this);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            return location;
        }
//        else{
//            Toast.makeText(context, "enble gps", Toast.LENGTH_SHORT).show();
//        }

        return null;
    }
    public boolean onOff(){
        LocationManager locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        boolean isGPSenbled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isGPSenbled;
    }

    @Override
    public void onLocationChanged(Location location) {
//        Toast.makeText(context, "new Location\n"+location.getLatitude()+" : "+location.getLongitude(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
