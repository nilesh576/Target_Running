package com.example.target_running;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    static TextView dis_cov_tv, dis_remain_tv, time_rem_tv,results_tv, time_taken_tv;
    EditText dis_et, min_et, sec_et, repeat_et;
    Button start_btn;
    TextToSpeech textToSpeech;
    GPStracker gpStracker;
    Location location;
    double longi1,longi2,lati1,lati2,disbtw, req_dis, per_sec_dis;
    int given_time,time_taken,dis_target,repeat_time;
    boolean running;
    volatile boolean force_stopped;
    SeekBar vol_bar;
    AudioManager audioManager;

    int prv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) MainActivity.this.getSystemService(AUDIO_SERVICE);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},123);

        dis_cov_tv = findViewById(R.id.tv_dis_cov);
        dis_remain_tv = findViewById(R.id.tv_dis_rem);
        time_rem_tv = findViewById(R.id.tv_time_rem);
        results_tv = findViewById(R.id.tv_result);
        time_taken_tv = findViewById(R.id.tv_time_taken);
        vol_bar = findViewById(R.id.volume_bar);

        vol_bar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        vol_bar.setMin(0);

        min_et = findViewById(R.id.et_total_min);
        sec_et = findViewById(R.id.et_total_sec);
        repeat_et = findViewById(R.id.et_repeat);
        dis_et = findViewById(R.id.et_total_dis);
        start_btn = findViewById(R.id.btn_start);

        prv = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        running = false;
        force_stopped = false;

        editTextMaxValChecker(min_et,300);
        editTextMaxValChecker(sec_et,59);
        editTextMaxValChecker(dis_et,30000);
        editTextMaxValChecker(repeat_et,60);

        editTextMinValChecker(min_et,1);
        editTextMinValChecker(repeat_et,1);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                textToSpeech.setLanguage(Locale.ENGLISH);
            }
        });
        gpStracker = new GPStracker(MainActivity.this);
        location = gpStracker.getLocation();

        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                location = gpStracker.getLocation();
                if(dis_et.getText().toString().equals("")||min_et.getText().toString().equals("")||repeat_et.getText().toString().equals("")||sec_et.getText().toString().equals("")){
                    Toast.makeText(MainActivity.this, "fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (location==null){

                    Timer timer = new Timer();
                    start_btn.setText("fetching location...");
                    start_btn.setEnabled(false);
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            if(location!=null){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        textToSpeech.speak("location fetched. you can start running",TextToSpeech.QUEUE_FLUSH, null);
                                        Toast.makeText(MainActivity.this, "location fetched \n you can start your run", Toast.LENGTH_LONG).show();
                                        start_btn.setText("start");
                                        start_btn.setEnabled(true);
                                    }
                                });
                                this.cancel();
                            }
                            else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        location = gpStracker.getLocation();
                                        if(!gpStracker.onOff()){
                                            start_btn.setText("switch on gps");
                                        }
                                        else{
                                            start_btn.setText("fetching...");
                                        }
                                    }
                                });
                            }
                        }
                    };
                    timer.schedule(timerTask,0,2000);
                    return;
                }
                if (!running ){
                    disbtw = 0;
                    time_taken = 0;
                    force_stopped = false;
                    location = gpStracker.getLocation();
                    start_btn.setText("end");
                    running = true;
                    results_tv.setText(R.string.running_result);

                    given_time = Integer.parseInt(min_et.getText().toString())*60+(Integer.parseInt(sec_et.getText().toString()));
                    dis_target = Integer.parseInt(dis_et.getText().toString());
                    repeat_time = Integer.parseInt(repeat_et.getText().toString());

                    dis_et.setEnabled(false);
                    repeat_et.setEnabled(false);
                    min_et.setEnabled(false);
                    sec_et.setEnabled(false);
                    start_running_process();
                    return;
                }
                else {
                    dis_et.setEnabled(true);
                    repeat_et.setEnabled(true);
                    min_et.setEnabled(true);
                    sec_et.setEnabled(true);

                    start_btn.setText("start");
                    running = false;
                    force_stopped = true;
                    return;
                }
            }
        });

        vol_bar.setProgress(prv);
        vol_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress>=prv){
                    audioManager.adjustVolume(1, AudioManager.FLAG_VIBRATE);

                }
                else{
                    audioManager.adjustVolume(-1, AudioManager.FLAG_VIBRATE);
                }
                prv = progress;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void start_running_process() {
        per_sec_dis = dis_target/given_time;
        Toast.makeText(this, "running started", Toast.LENGTH_SHORT).show();
        Toast.makeText(this, ""+location.getLatitude()+"\n"+location.getLongitude(), Toast.LENGTH_SHORT).show();
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                req_dis = (time_taken*per_sec_dis);
                if(time_taken % repeat_time==0 && time_taken!=0){
                    if(disbtw < req_dis){
                        int x = (int) (req_dis-disbtw);
                        textToSpeech.speak("you are "+x+" meters behind you target", TextToSpeech.QUEUE_FLUSH, null);
                    }
                    else{
                        int y = (int) (disbtw-req_dis);
                        textToSpeech.speak("you are "+y+"meters above you target", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }

                longi1 = location.getLongitude();
                lati1 = location.getLatitude();
                if(gpStracker.getLocation() !=null){
                   location = gpStracker.getLocation();
                }

                longi2 = location.getLongitude();
                lati2 = location.getLatitude();

                disbtw = disbtw+(latlongdis(lati1,longi1,lati2,longi2));

                longi1 = longi2;
                lati1 = lati2;

                time_taken++;

                if(force_stopped || disbtw > dis_target || time_taken >given_time ||gpStracker.getLocation()==null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(gpStracker.getLocation()==null){
                                results_tv.setText("location gone");
                                Toast.makeText(MainActivity.this, "location gone", Toast.LENGTH_SHORT).show();
                            }
                            else if(disbtw >= dis_target){
                                results_tv.setText("success");
                            }
                            else{
                                results_tv.setText("mission failed, we'll get'em next time");
                            }
                            dis_cov_tv.setText("total distance coverd : ");
                            dis_remain_tv.setText("total distance coverd : ");
                            time_rem_tv.setText("total distance coverd : ");
                            time_rem_tv.setText("time taken : ");
                        }
                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            start_btn.performClick();
                        }
                    });
                    running = true;
                    this.cancel();

                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int i = (given_time-time_taken);
                        String x = ""+((int)(i/3600))+" : "+((int)(i/60))+" : "+(i%60);
                        dis_cov_tv.setText("distance coverd : "+disbtw);
                        dis_remain_tv.setText("distance remaining : "+(dis_target-disbtw));
                        time_taken_tv.setText("time remaining : "+x);
                        x = ""+((int)(time_taken/3600))+" : "+((int)(time_taken/60))+" : "+(time_taken%60);
                        time_rem_tv.setText("time taken : "+x);
                    }
                });

            }
        };
        timer.schedule(timerTask,0,1000);
    }

    public void editTextMaxValChecker(EditText editText, int maxVal){
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try{
                    if(Integer.parseInt(s.toString())>maxVal){
                        editText.setText(String.valueOf(maxVal));
                        Toast.makeText(MainActivity.this, "choose between 0 to "+maxVal, Toast.LENGTH_SHORT).show();
                        editText.clearFocus();
                        InputMethodManager imm = (InputMethodManager)getSystemService(MainActivity.this.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    }
                }
                catch (Exception e){

                }

            }
        });
    }
    public void editTextMinValChecker(EditText editText, int minVal){
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try{
                    if(Integer.parseInt(s.toString())<minVal){
                        editText.setText(String.valueOf(minVal));
                        editText.clearFocus();
                        InputMethodManager imm = (InputMethodManager)getSystemService(MainActivity.this.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    }
                }
                catch (Exception e){

                }

            }
        });
    }
    public int latlongdis(double lat1,double lon1, double lat2, double lon2){
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        dist = dist * 1.609344;
        dist = dist*1000;

        return (int)dist;
    }
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }
    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        int key_code = event.getKeyCode();
        int action = event.getAction();

        if (key_code == KeyEvent.KEYCODE_VOLUME_DOWN && KeyEvent.ACTION_DOWN == action) {
            return true;
        }
        if (key_code == KeyEvent.KEYCODE_VOLUME_DOWN && KeyEvent.ACTION_UP == action) {
            if(start_btn.isEnabled()){
                start_btn.performClick();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", false);
        sendBroadcast(intent);
        super.onDestroy();
    }


}