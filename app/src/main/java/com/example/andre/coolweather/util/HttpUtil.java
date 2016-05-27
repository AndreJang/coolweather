package com.example.andre.coolweather.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 与服务器进行交互的工具类
 * Created by Andre on 2016/4/28.
 */
public class HttpUtil {
    /**
     * 定义一个sendHttpRequest()方法来进行发送请求操作。
     * 此方法一开始就在子线程中进行，其余步骤就是HttpURLClient的基本用法、IO流的基本操作
     * @param address 访问服务器的URL地址
     * @param listener 自定义的一个接口，该接口里面有2个简单的方法
     */
    public static void sendHttpRequest(final String address, final HttpCallbackListener listener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;

                try {
                    URL url = new URL(address);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream inputStream = connection.getInputStream();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    StringBuilder responseStream = new StringBuilder();

                    String line;

                    while ((line = reader.readLine())!= null){
                        responseStream.append(line);
                    }

                    if (listener != null){
                        //回调onFinish()方法
                        listener.onFinish(responseStream.toString());
                    }
                } catch (IOException e) {
                    if (listener != null){
                        //回调onError()方法
                        listener.onError(e);
                    }
                } finally {
                    if (connection != null){
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }
}
