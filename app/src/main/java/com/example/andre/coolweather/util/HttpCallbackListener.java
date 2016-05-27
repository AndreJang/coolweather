package com.example.andre.coolweather.util;

/**
 * Created by Andre on 2016/4/28.
 */
public interface HttpCallbackListener {
    void onFinish(String response);

    void onError(Exception e);
}
