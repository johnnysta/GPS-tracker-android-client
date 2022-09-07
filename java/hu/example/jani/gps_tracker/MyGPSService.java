package hu.example.jani.gps_tracker;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;



/**
 * Created by Janó on 2017.03.08..
 */

public class MyGPSService extends Service implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int PENDING_INTENT_ID = 201;
    private static final int NOTIF_ID = 101;
    private boolean enabled = true;
    private NotificationCompat.Builder notifBuilder;
    private PendingIntent pi;

    LocationManager lm;

    private String phone_number_setting;
    private String basic_message_setting;
    private String registered_id_setting;
    private String tracked_entity_name_setting;
    private String provider_setting;
    private long interval_setting;
    private int movement_setting;

    SharedPreferences defaultPrefs;

    RequestQueue queue_get, queue_post;
    StringRequest stringRequest;
    String strResponse;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void onCreate(){
        super.onCreate();
        Log.d("GPS debug", "Service onCreate");

        //Intent pending intent létrehozása a notificationhöz, ami Foreground mód esetén kötelező
        Intent i = new Intent(this, MainActivity.class);
        pi = PendingIntent.getActivity(this, PENDING_INTENT_ID, i, PendingIntent.FLAG_CANCEL_CURRENT);

        notifBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.kenu)
                        .setContentTitle("Status")
                        .setContentText("My GPS service is running")
                        .setTicker("Watching coordinates")
                        .setContentIntent(pi);


        defaultPrefs  =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        defaultPrefs.registerOnSharedPreferenceChangeListener(this);


        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }



    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            final double lat = location.getLatitude();
            final double lon = location.getLongitude();
            final double acc = location.getAccuracy();

            String strLat = Double.toString(lat);
            String strLon = Double.toString(lon);
            String strAcc = Double.toString(acc);

            final String message = basic_message_setting + " Latitude: " + lat + " Longitude: "
                    + lon + "Accurracy: " + acc;

            //Rows for sending broadcast message for location change:
            Intent intent = new Intent();
            intent.setAction("LOCATION_CHANGED");
            // Include extra data:
            intent.putExtra("Latitude", strLat);
            intent.putExtra("Longitude", strLon);
            intent.putExtra("Accuracy", strAcc);
            LocalBroadcastManager.getInstance(MyGPSService.this).sendBroadcast(intent);


            // Rows for sending SMS

            // Get the default instance of SmsManager
            //SmsManager smsManager = SmsManager.getDefault();
            // Send a text based SMS
            //smsManager.sendTextMessage(phone_number_setting, null, message, null, null);
            //Uri uri = Uri.parse("smsto:" + number);
            // intent = new Intent(Intent.ACTION_SENDTO, uri);
            // intent.putExtra("sms_body", message);
            //startActivity(intent);


            //Rows for sending HTTP request

            // Instantiate the RequestQueue.
            queue_get = Volley.newRequestQueue(MyGPSService.this);
            long time = System.currentTimeMillis();
            Log.d("GPS debug", Long.toString(time/1000));
            String url ="http://szazevezo.hu/add_to_db.php?lat="+strLat+"&lng="+strLon+"&acc="
                    +strAcc+"&id="+registered_id_setting+"&entity="+tracked_entity_name_setting+
                    "&time="+Long.toString(time/1000);

            // Request a string response from the provided URL.
            // Would be better to use POST method..
            stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {

                            Log.d("GPS debug", "url");

                            // Characters of the response string
                            strResponse = response.substring(0,response.length());

                            Log.d("GPS debug", strResponse);
                            //Rows for sending broadcast message for web server response:
                            Intent intent2 = new Intent();
                            intent2.setAction("WEB_SERVER_RESPONSE");
                            // Include extra data:
                            intent2.putExtra("Response", strResponse);
                            LocalBroadcastManager.getInstance(MyGPSService.this).sendBroadcast(intent2);

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    Log.d("GPS debug", "Service onErrorResponse");

                    if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                        strResponse="Network timeout!";
                    } else if (error instanceof AuthFailureError) {
                        strResponse="Auth Failure Error!";
                    } else if (error instanceof ServerError) {
                        strResponse="Server Error!";
                    } else if (error instanceof NetworkError) {
                        strResponse="Network Error!";
                    } else if (error instanceof ParseError) {
                        strResponse="Parse Error!";
                    }

                    //Rows for sending broadcast message for web server response:
                    Intent intent2 = new Intent();
                    intent2.setAction("WEB_SERVER_RESPONSE");
                    // Include extra data:
                    intent2.putExtra("Response", strResponse);
                    LocalBroadcastManager.getInstance(MyGPSService.this).sendBroadcast(intent2);

                }
            });
            // Add the request to the RequestQueue.
            queue_get.add(stringRequest);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };


    public void onDestroy(){

        if (locationListener != null){

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            lm.removeUpdates(locationListener);
        }

        //true: vegye le a notificationt:
        stopForeground(true);
        enabled=false;
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d("GPS debug", "Service onStartCommand");
        Notification notification = notifBuilder.build();
        startForeground(NOTIF_ID, notification);
        enabled = true;
        //new MyTimeThread().start();
        readPreferencesAndRequestLocUpdates();
        return super.onStartCommand(intent, flags, startId);
    }

    public void readPreferencesAndRequestLocUpdates()
    {
        interval_setting = Long.parseLong(defaultPrefs.getString("interval_setting", "30000"));
        movement_setting = Integer.parseInt(defaultPrefs.getString("movement_setting", "50"));
        phone_number_setting = defaultPrefs.getString("phone_number", "+36302221884");
        basic_message_setting = defaultPrefs.getString("basic_message", "Elmozdulás!");
        provider_setting = defaultPrefs.getString("provider", "gps");
        registered_id_setting = defaultPrefs.getString("registered_id", "");
        tracked_entity_name_setting = defaultPrefs.getString("tracked_entity_name", "");


        /*Toast.makeText(MainActivity.this,
                "elmozdulás beáll.:" + Integer.toString(movement_setting)+ " gyakoriság (ms): " +
                        Long.toString(interval_setting),
                Toast.LENGTH_LONG).show();
                */

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        /*
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
        criteria.setCostAllowed(false);

        String provider = lm.getBestProvider(criteria, true);
        */


        lm.requestLocationUpdates(provider_setting, interval_setting,
                movement_setting, locationListener);

        Log.d("provider running: ", provider_setting);
    }

    @Override
    //Implement change listener for preferences:
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        readPreferencesAndRequestLocUpdates();
        return;
    }


}