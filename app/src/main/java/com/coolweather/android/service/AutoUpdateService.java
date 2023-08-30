package com.coolweather.android.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 后台自动更新天气服务
 */
public class AutoUpdateService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();

        //创建定时任务,每8小时执行一次
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 8 * 60 * 60 * 1000;//8小时毫秒值
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 更新天气数据
     */
    private void updateWeather() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherStr = preferences.getString("weather", null);
        if (weatherStr != null) {
            Weather weather = Utility.handleWeatherResponse(weatherStr);
            if (weather != null) {
                String weatherId = weather.basic.weatherId;
                String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=66e69147552e474da29595ac96f39856";
                HttpUtil.sendOKHttpRequest(weatherUrl, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String responseStr = response.body().string();
                        Weather weather = Utility.handleWeatherResponse(responseStr);
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                            edit.putString("weather", responseStr);
                            edit.apply();
                        }
                    }
                });
            }
        }
    }

    /**
     * 更新背景图
     */
    private void updateBingPic() {
        String requestBingPicUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOKHttpRequest(requestBingPicUrl, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseStr = response.body().string();
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                edit.putString("bing_pic", responseStr);
                edit.apply();
            }
        });
    }
}