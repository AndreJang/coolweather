package com.example.andre.coolweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.andre.coolweather.R;
import com.example.andre.coolweather.database.CoolWeatherDB;
import com.example.andre.coolweather.model.City;
import com.example.andre.coolweather.model.County;
import com.example.andre.coolweather.model.Province;
import com.example.andre.coolweather.util.HttpCallbackListener;
import com.example.andre.coolweather.util.HttpUtil;
import com.example.andre.coolweather.util.Utility;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Andre on 2016/5/1.
 */
public class ChooseAreaActivity extends Activity {
    long preTime;
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ListView listView;
    private TextView titleText;

    private List<String> dataList = new ArrayList<String>();
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;

    private ProgressDialog progressDialog;

    //是否从WeatherActivity中跳转过来
    private boolean isFromWeatherActivity;

    //省列表
    private List<Province> provinceList;

    //市列表
    private List<City> cityList;

    //县列表
    private List<County> countyList;


    //选中的省份
    private Province selectedProvince;

    //选中的城市
    private City selectedCity;

    //当前选中的级别
    private int currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity",false);

        //从ChooseAreaActivity中转到WeatherActivity
        SharedPreferences preferences = PreferenceManager.
                getDefaultSharedPreferences(ChooseAreaActivity.this);

        if (preferences.getBoolean("city_selected",false) && !isFromWeatherActivity){
            Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);

        listView = (ListView) findViewById(R.id.list_view);
        titleText = (TextView) findViewById(R.id.title_text);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);

        listView.setAdapter(adapter);
        coolWeatherDB = CoolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if (currentLevel == LEVEL_COUNTY){
                    String countyCode = countyList.get(position).getCountyCode();
                    Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                    finish();
                }
            }


        });
        //加载省级数据
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果数据库没有再去服务器上查询
     */
    private void queryProvinces(){
        provinceList = coolWeatherDB.loadProvinces();

        if (provinceList.size() > 0){
            dataList.clear();

            for (Province province:provinceList) {
                dataList.add(province.getProvinceName());
            }

            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");

            currentLevel = LEVEL_PROVINCE;
        }else{
            queryFromServer(null,"province");

        }
    }

    /**
     * 查询被选中的省内所有的城市，优先从数据库查询，如果数据库没有再去服务器上查询
     */
    private void queryCities(){
        cityList = coolWeatherDB.loadCities(selectedProvince.getId());

        if (cityList.size() > 0){
            dataList.clear();

            for (City city: cityList) {
                dataList.add(city.getCityName());
            }

            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());

            currentLevel = LEVEL_CITY;
        }else{
            queryFromServer(selectedProvince.getProvinceCode(),"city");
        }
    }

    /**
     * 查询被选中的市内所有的县，优先从数据库查询，如果数据库没有再去服务器上查询
     */
    private void queryCounties(){
        countyList = coolWeatherDB.loadCounties(selectedCity.getId());

        if (countyList.size() > 0){
            dataList.clear();

            for (County county:countyList) {
                dataList.add(county.getCountyName());
            }

            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());

            currentLevel = LEVEL_COUNTY;
        }else{
            queryFromServer(selectedCity.getCityCode(),"county");
        }
    }

    /**
     * 根据传入的代号和类型从服务器上查询省市县数据
     * @param code
     * @param type
     */
    private void queryFromServer(final String code,final String type){
        String address;

        if (!TextUtils.isEmpty(code)){
            address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
        }else {
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }

        showProgressDialog();

        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;

                if ("province".equals(type)){
                    result = Utility.handleProvincesResponse(coolWeatherDB,response);
                }else if("city".equals(type)){
                    result = Utility.handleCitiesResponse(coolWeatherDB,response,selectedProvince.getId());
                }else if("county".equals(type)){
                    result = Utility.handleCountiesResponse(coolWeatherDB,response,selectedCity.getId());
                }

                if (result){
                    //通过runOnUiThread( )方法回到主线程处理逻辑
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();

                            if ("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                //通过runOnUiThread( )方法回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this,"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }




    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            long currentTime = new Date().getTime();

            // 如果时间间隔大于2秒, 不处理

            if ((currentTime - preTime) > 2000) {
                // 显示消息
                Toast.makeText(this, "再按一次退出程序",
                        Toast.LENGTH_SHORT).show();

                // 更新时间
                preTime = currentTime;

                // 截获事件,不再处理
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


}
