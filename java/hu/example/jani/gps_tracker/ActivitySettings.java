package hu.example.jani.gps_tracker;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

/**
 * Created by Janó on 2016.06.01..
 */
public class ActivitySettings extends PreferenceActivity {
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        Log.d("GPS debug", "Setting activity elindult!");

        //if (hasHeaders()){
        Button button = new Button(this);
        button.setText("Vissza a főképernyőre");
        //setListFooter(button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ArrayList<View> buttonList = new ArrayList<>(1);
        buttonList.add(button);
        this.findViewById(android.R.id.content).addFocusables(buttonList, View.FOCUS_FORWARD);


    }
}