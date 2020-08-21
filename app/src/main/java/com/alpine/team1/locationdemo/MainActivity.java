package com.alpine.team1.locationdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Button update;
    private TextView cityname;
    private static double latitude = 0.0;
    private static double longitude = 0.0;
    private static final String TAG = "GPS";
    LocationManager locationManager = null;
    private static final int PRIVATE_CODE = 1315; //开启GPS权限
    private static final int BAIDU_READ_PHONE_STATE = 100; //定位权限请求
    private Timer timer = new Timer(true);
    int count=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        update = findViewById(R.id.update);
        cityname = findViewById(R.id.cityname);
        showGpsContacts();
        timer.schedule(timerTask,0,1000*10);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGpsContacts();
                //Log.d(TAG, String.valueOf(count++));
            }
        });
    }
    /***************位置定时更新任务*********************************************/
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1){
                showGpsContacts();
            }
        }
    };
 //定时任务
     private TimerTask timerTask = new TimerTask() {
         @Override
         public void run() {
             Message msg = new Message();
             msg.what = 1;
             handler.sendMessage(msg);
         }
     } ;

    //权限回调函数
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case BAIDU_READ_PHONE_STATE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.length > 0) {
                    getLocation();
                } else {
                    showGpsContacts();
                }
                break;
            default:
                break;
        }
    }

    //2.获取手机GPS位置信息
    public void showGpsContacts() {
        //调用系统服务
        locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
        boolean ok = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (ok) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//权限检查
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                } else {
                    getLocation();
                }
            } else {
                getLocation();
            }
        } else {
            Toast.makeText(this, "GPS服务未开启，请开启", Toast.LENGTH_LONG).show();
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, PRIVATE_CODE);
        }
    }

    private void getLocation() {
        final LocationManager locationManager1;
        String serviceName = Context.LOCATION_SERVICE;
        locationManager1 = (LocationManager) this.getSystemService(serviceName);
        //查找到服务信息
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);//高精度
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);//低功耗
        String provider = locationManager1.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager1.getLastKnownLocation(provider);
        if (location != null){
            Log.d(TAG, "GPS");
            updateLocation(location);
        }else{
            locationManager1.requestLocationUpdates(provider, 1000, 10, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    updateLocation(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    updateLocation(locationManager1.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                   }

                   @Override
                   public void onProviderDisabled(String provider) {

                   }
               });
           }
    }

    //获取当前经纬度
    private void updateLocation(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        Log.d(TAG, "经度：" + location.getLongitude() + "，纬度：" + location.getLatitude());
        List<Address> addresslist = getAddress(this, location);
        if (addresslist.size() > 0) {
            cityname.setText(addresslist.get(0).getLocality());
            // Log.d(TAG,addresslist.get(0).getCountryName());//国
            Log.d(TAG, addresslist.get(0).getLocality());  //市
            // Log.d(TAG,addresslist.get(0).getAddressLine(0));//省市县
            // Log.d(TAG,addresslist.get(0).getAdminArea());//省
        }
    }

    //3.根据经纬度获得城市名
    private static List<Address> getAddress(Context context,Location location) {
        //用来接收位置的详细信息
        List<Address> result = null;
        try {
            if (location != null) {
                Geocoder gc = new Geocoder(context, Locale.getDefault());
                result = gc.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}