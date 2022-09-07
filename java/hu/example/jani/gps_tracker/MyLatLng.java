package hu.example.jani.gps_tracker;

import java.io.Serializable;

/**
 * Created by Janó on 2017.03.10..
 */

public class MyLatLng implements Serializable {

    double fieldLatitude, fieldLongitude;

    public MyLatLng(double latitude, double longitude) {
        this.fieldLatitude = latitude;
        this.fieldLongitude = longitude;
    }

   /* protected MyLatLng(Parcel in) {
        this.fieldLatitude = Double.parseDouble(in.readString());
        this.fieldLongitude = Double.parseDouble(in.readString());
    }*/

    public double getLat(){
        return this.fieldLatitude;
    }

    public double getLng(){
        return this.fieldLongitude;
    }

    public void setLat(double aLat){
        this.fieldLatitude = aLat;
    }

    public void setLng(double aLng){
        this.fieldLatitude = aLng;
    }

 /*   public static final Creator<MyLatLng> CREATOR = new Creator<MyLatLng>() {
        @Override
        public MyLatLng createFromParcel(Parcel in) {
            return new MyLatLng(in);
        }

        @Override
        public MyLatLng[] newArray(int size) {
            return new MyLatLng[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(String.valueOf(this.fieldLatitude));
        dest.writeString(String.valueOf(this.fieldLongitude));
    }
    */
}