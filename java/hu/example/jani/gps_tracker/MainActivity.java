package hu.example.jani.gps_tracker;


import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {


    TextView textViewLatLng, textViewResponse;
    double lLatitude;
    double lLongitude;

    BroadcastReceiver mMessageReceiver;

    ArrayList<MyLatLng> listLatLon = new ArrayList<MyLatLng>();
    private final String POSITIONS_FILENAME="positions";
    private GoogleMap mMap;

    Button btnStart, btnStop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("GPS debug", "on create");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction ft = fm.beginTransaction();
        SupportMapFragment mapFragment = new  SupportMapFragment();
        ft.add(R.id.layoutContent, mapFragment);
        ft.commit();
        mapFragment.getMapAsync(this);

        textViewLatLng = (TextView) findViewById(R.id.twLatLng);
        textViewResponse = (TextView) findViewById(R.id.twResponse);


        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);

        if (isMyServiceRunning(MyGPSService.class))
        {
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        }
        else
        {
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        }


        //Broadcast listener

        mMessageReceiver = new BroadcastReceiver() {
            @TargetApi(Build.VERSION_CODES.ECLAIR)
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                Log.d("GPS debug", "on receive"+intent.getAction());
                switch (intent.getAction()) {
                    case "LOCATION_CHANGED":
                        String latitude = intent.getStringExtra("Latitude");
                        String longitude = intent.getStringExtra("Longitude");
                        String accuracy = intent.getStringExtra("Accuracy");
                        textViewLatLng.setText("Lat.: " + latitude + " Lng.: " + longitude +
                                "Acc.: " + accuracy + "m");
                        lLatitude = Double.parseDouble(latitude);
                        lLongitude = Double.parseDouble(longitude);

                        listLatLon.add(new MyLatLng(lLatitude, lLongitude));
                        //write the list with new point immediately to file:
                        writeListToFile();

                        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 30);
                        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);

                        drawAllPointsOnMap();
                        break;
                    case "WEB_SERVER_RESPONSE":
                        String response = intent.getStringExtra("Response");
                        textViewResponse.setText("Response: " + response);
                        Log.d("GPS debug", "inmain"+response.substring(0,response.length()));
                        break;
                }
            }
        };

        IntentFilter intentFilter1 = new IntentFilter();
        intentFilter1.addAction("LOCATION_CHANGED");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, intentFilter1);

        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("WEB_SERVER_RESPONSE");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, intentFilter2);
    }


    public void onStartButtonClick(View v){
        Intent i = new Intent(MainActivity.this, MyGPSService.class);
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        startService(i);
    }


    public void onStopButtonClick(View v){
        Intent i = new Intent(MainActivity.this, MyGPSService.class);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        stopService(i);
    }

    public void onClearButtonClick(View v){
        listLatLon.clear();
        writeListToFile();
        drawAllPointsOnMap();
    }


    protected void onResume(){
        //reload list of positions
        Log.d("GPS debug", "on resume");
        readListFromFile();
        super.onResume();
    }

    private void readListFromFile() {
        java.io.FileInputStream fin = null;
        try {
            fin = openFileInput(POSITIONS_FILENAME);
            ObjectInputStream ois = new ObjectInputStream(fin);
            ArrayList<MyLatLng> temp3  = (ArrayList<MyLatLng>) ois.readObject();
            ois.close();
            //Check whether readObject returned null:
            if (temp3 != null){
                Log.d("open_list:", "temp3 not null");
                this.listLatLon.clear();
                this.listLatLon.addAll(temp3);
                Log.d("listLatLon size or:", String.valueOf(listLatLon.size()));
            }
            else {
                this.listLatLon.clear();
                Log.d("open_list:", "temp3 is null");
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if (fin != null)
                try{
                    fin.close();
                }
                catch (java.io.IOException e){
                    e.printStackTrace();
                }
        }
    }

    protected void onPause(){
        //save list of positions in a file not needed here, because it was immeadiately
        // saved after updating it, so the bellow can be commented out
        Log.d("GPS debug", "on pause");
        //Log.d("listLatLon size op:", String.valueOf(listLatLon.size()));
        writeListToFile();
        super.onPause();
    }

    private void writeListToFile() {
        java.io.FileOutputStream fos = null;
        try{
            fos = openFileOutput(POSITIONS_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(listLatLon);
            oos.close();
            Log.d("GPS debug", "Ide eljutott: fájlbaírás");
        } catch (java.io.FileNotFoundException e) {
            Log.d("GPS debug", "File Not Found");
            e.printStackTrace();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        finally {
            if (fos != null)
                try {
                    fos.close();
                }
                catch (java.io.IOException e){
                    e.printStackTrace();
                }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId()== R.id.action_settings)
        {
            Intent i = new Intent();
            i.setClass(this, ActivitySettings.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("GPS debug", "on map ready");
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        drawAllPointsOnMap();
    }

    public void drawAllPointsOnMap(){
        if (mMap!=null)
        {
            mMap.clear();

            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.width(2);
            polylineOptions.color(Color.RED);


            if(listLatLon.size()>0)
            {
                //Draw polyline based on list of positions:
                for (MyLatLng item : listLatLon) {
                    polylineOptions.add(new LatLng(item.getLat(), item.getLng()));
                }
                mMap.addPolyline(polylineOptions);


                //find the last item of the list:
                MyLatLng currentMyLatLng = listLatLon.get(listLatLon.size() - 1);
                LatLng currentLatLng = new LatLng(currentMyLatLng.getLat(), currentMyLatLng.getLng());

                //Add a marker to the last item, and zoom to it:
                mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Last position"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15.0f));
            }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
