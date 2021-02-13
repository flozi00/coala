package io.aware.coala;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import androidx.core.app.ActivityCompat;


public class assistant extends Activity {

    public int helpfeature;
    public String phonenumber;
    public String customurl;
    public Context wakeContext;
    public Button helpButton;
    public Uri mymp3 = null;
    public String mp3;
    public MediaPlayer mediaPlayer = new MediaPlayer();
    public boolean alarm_bool, api_bool, sms_bool, call_bool;

    public long lastCall = 0;


    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.assistant);
        wakeContext = this.getApplicationContext();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("settings", Context.MODE_MULTI_PROCESS); // 0 - for private mode
        phonenumber = pref.getString("phonenumber", "");
        customurl = pref.getString("customurl","");
        helpfeature = pref.getInt("hlp", 0);
        mp3 = pref.getString("mp3URI","mp3URI");
        alarm_bool = pref.getBoolean("alarm",false);
        api_bool = pref.getBoolean("api",false);
        sms_bool = pref.getBoolean("sms",false);
        call_bool = pref.getBoolean("call",false);

        if(mp3.equals("mp3URI")){
            mymp3 = Uri.parse(mp3);
        }

        helpButton = findViewById(R.id.button);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getHelp();
            }
        });

    }

    public void callAPI(){
        try{
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = customurl;

// Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Display the first 500 characters of the response string.
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            });

// Add the request to the RequestQueue.
            queue.add(stringRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playAlarm(){
        try {

            AssetFileDescriptor descriptor = wakeContext.getAssets().openFd("alarm.mp3");
            if(mymp3 != null){
                try {
                    mediaPlayer.setDataSource(wakeContext, Uri.parse(mp3));
                } catch(Exception e){
                    e.printStackTrace();
                    mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                }
            } else {
                mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            }

            descriptor.close();

            AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

            try {
                mediaPlayer.prepare();
            } catch(Exception e){
                e.printStackTrace();
            }
            mediaPlayer.setVolume(1f, 1f);
            mediaPlayer.setLooping(false);
            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                int maxCount = 0;
                int count = 0; // initialise outside listener to prevent looping

                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    if (count < maxCount) {
                        count = count + 1;
                        mediaPlayer.seekTo(0);
                        mediaPlayer.start();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendSMS(){
        try {
            GPSTracker gpsTracker;
            gpsTracker = new GPSTracker(wakeContext);
            if (gpsTracker.getLocation() != null) {
                if (gpsTracker.getLatitude() != 0 && gpsTracker.getLongitude() != 0) {
                    String lat = Double.toString(gpsTracker.getLatitude());
                    String longi = Double.toString(gpsTracker.getLongitude());
                    String message = "Help me, this is my location: https://www.google.com/maps/search/?api=1&query=" + lat + "," + longi;
                    SmsManager smsManager = SmsManager.getDefault();
                    String[] phonenumbers = phonenumber.split(",");
                    for (String p : phonenumbers) {
                        smsManager.sendTextMessage(p, null, message, null, null);
                    }
                    Toast.makeText(wakeContext, "Send SMS", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startCall(){
        String[] phonenumbers = phonenumber.split(",");

        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phonenumbers[0]));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivity(intent);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void getHelp() {

        if(lastCall == 0 || (System.currentTimeMillis() - lastCall) > 1000){
            lastCall = System.currentTimeMillis();
            try{
                mediaPlayer.reset();
            } catch(Exception e){
                e.printStackTrace();
            }

            if(sms_bool){
                sendSMS();
            }

            if(api_bool){
                callAPI();
            }

            if(alarm_bool){
                playAlarm();
            }

            if(call_bool){
                startCall();
            }
        }

    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
        try {
            mediaPlayer.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
