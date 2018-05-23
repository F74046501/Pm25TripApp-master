package com.example.r_ni.maptest;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //////////the thing of server///////////
    /*主 变量*/
    // 主线程Handler
    // 用于将从server获取的消息显示出来
    private Handler mMainHandler;

    // Socket变量
    private Socket socket;

    // 线程池
    // 为了方便展示,此处直接采用线程池进行线程管理,而没有一个个开线程
    private ExecutorService mThreadPool;

    /*接收server消息 变量*/
    // 输入流对象
    InputStream is;

    // 输入流读取器对象
    InputStreamReader isr ;
    BufferedReader br ;

    // 接收server发送过来的消息
    String response;
    private JSONObject json_read;
    public static JSONArray myjs_array;

    //////////the things of Now_Weather///////////
    //哪一個城市的資訊
    int index = 0;
    String[] temperature_morning = new String[7];
    String[] temperature_night = new String[7];
    String[] weather_morning = new String[7];
    String[] weather_night = new String[7];

    ImageView ImageView_now;
    ImageView ImageView_d1;
    ImageView ImageView_d2;
    ImageView ImageView_d3;
    TextView textView_d1 ;
    TextView textView_d2;
    TextView textView_d3;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        /////////////////上面是它一開始有的東西///////////////////////////
        textView_d1 = (TextView)findViewById(R.id.textView_D1);
        textView_d2 = (TextView)findViewById(R.id.textView_D2);
        textView_d3 = (TextView)findViewById(R.id.textView_D3);
        ImageView_d1  = (ImageView)findViewById(R.id.image_d1);
        ImageView_d2  = (ImageView)findViewById(R.id.image_d2);
        ImageView_d3  = (ImageView)findViewById(R.id.image_d3);
        for(int i = 0; i < 7; i++){
            weather_morning[i] = "";
            weather_night[i] = "";
            temperature_morning[i] = "";
            temperature_night[i] = "";
        }
        server_connect();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_map) {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();//the package of the things i want to pass
            intent.setClass(this,MapsActivity.class);
            startActivity(intent);
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @SuppressLint("HandlerLeak")
    private void server_connect(){
        // 初始化线程池
        mThreadPool = Executors.newCachedThreadPool();
        /**
         * 创建客户端 & server的连接
         * 並且接收server的訊息
         */
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 创建Socket对象 & 指定服务端的IP 及 端口号
                    socket = new Socket("192.168.100.41", 9999);
                    // 判断客户端和服务器是否连接成功
                    System.out.println(socket.isConnected());

                    /**
                     * 接收server消息
                     */
                    try {
                        // 步骤1：创建输入流对象InputStream
                        is = socket.getInputStream();

                        // 步骤2：创建输入流读取器对象 并传入输入流对象
                        // 该对象作用：获取服务器返回的数据
                        isr = new InputStreamReader(is);
                        br = new BufferedReader(isr);

                        // 步骤3：通过输入流读取器对象 接收服务器发送过来的数据 （response是String型別）
                        response = br.readLine();

                        // 步骤4:通知主线程,将接收的消息显示到界面
                        Message msg = Message.obtain();//Message是thread的東西
                        msg.what = 0;
                        mMainHandler.sendMessage(msg);

                        if(response!=null){
                            try{
                                myjs_array  = new JSONArray(response);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // 实例化主线程,""用于更新接收过来的消息""
        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show();
                if (response != null) {
                    try {
                        myjs_array = new JSONArray(response);
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), "response is null", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }

                for (int i = 0; i < 7; i++) {
                    JSONObject myjObject = null;
                    try {
                        myjObject = myjs_array.getJSONObject(23 + index * 16 + i);
                        temperature_morning[i] = myjObject.getString("temperature");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    ////////////////////////////////////////////////////////////////////
                    try {
                        myjObject = myjs_array.getJSONObject(31 + index * 16 + i);
                        temperature_night[i] = myjObject.getString("temperature");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    ////////////////////////////////////////////////////////////////////
                    try {
                        myjObject = myjs_array.getJSONObject(374 + index * 14 + i);
                        weather_morning[i] = myjObject.getString("Rain");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    ////////////////////////////////////////////////////////////////////
                    try {
                        myjObject = myjs_array.getJSONObject(381 + index * 14 + i);
                        weather_night[i] = myjObject.getString("Rain");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                textView_d1.setText("\t\t\tDay 1\n早上\n" + "氣溫：" + temperature_morning[0] + "\n天氣：" + weather_morning[0] +
                        "\n\n晚上\n" + "氣溫：" + temperature_night[0] + "\n天氣：" + weather_night[0]);
                textView_d2.setText("\t\t\tDay 2\n早上\n" + "氣溫：" + temperature_morning[1] + "\n天氣：" + weather_morning[1] +
                        "\n\n晚上\n" + "氣溫：" + temperature_night[1] + "\n天氣：" + weather_night[1]);
                textView_d3.setText("\t\t\tDay 3\n早上\n" + "氣溫：" + temperature_morning[2] + "\n天氣：" + weather_morning[2] +
                        "\n\n晚上\n" + "氣溫：" + temperature_night[2] + "\n天氣：" + weather_night[2]);

                Boolean[] d1d2d3_cloud = new Boolean[3];
                Boolean[] d1d2d3_sun = new Boolean[3];
                Boolean[] d1d2d3_rain = new Boolean[3];
                Boolean[] d1d2d3_thunder = new Boolean[3];
                for(int i = 0; i < 3; i++){
                    d1d2d3_cloud[i] = false;
                    d1d2d3_rain[i] = false;
                    d1d2d3_sun[i] = false;
                    d1d2d3_thunder[i] = false;
                }
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < weather_morning[i].length(); j++) {
                        if (weather_morning[i].charAt(j) == '雲') d1d2d3_cloud[i] = true;
                        if (weather_morning[i].charAt(j) == '晴') d1d2d3_sun[i] = true;
                        if (weather_morning[i].charAt(j) == '雨') d1d2d3_rain[i] = true;
                        if (weather_morning[i].charAt(j) == '雷') d1d2d3_thunder[i] = true;
                    }

                    if (d1d2d3_cloud[i]==true && d1d2d3_sun[i]==false && d1d2d3_rain[i]==false && d1d2d3_thunder[i]==false) {
                        if(i==0) ImageView_d1.setImageResource(R.drawable.cloud);
                        else if(i==1) ImageView_d2.setImageResource(R.drawable.cloud);
                        else if(i==2) ImageView_d3.setImageResource(R.drawable.cloud);
                    }
                    else if (d1d2d3_cloud[i]==true && d1d2d3_sun[i]==true && d1d2d3_rain[i]==false && d1d2d3_thunder[i]==false) {
                        if(i==0) ImageView_d1.setImageResource(R.drawable.sun_cloud);
                        else if(i==1) ImageView_d2.setImageResource(R.drawable.sun_cloud);
                        else if(i==2) ImageView_d3.setImageResource(R.drawable.sun_cloud);
                    }
                    else if (d1d2d3_cloud[i]==false && d1d2d3_sun[i]==true && d1d2d3_rain[i]==false && d1d2d3_thunder[i]==false) {
                        if(i==0) ImageView_d1.setImageResource(R.drawable.sun);
                        else if(i==1) ImageView_d2.setImageResource(R.drawable.sun);
                        else if(i==2) ImageView_d3.setImageResource(R.drawable.sun);
                    }
                    else if (d1d2d3_thunder[i]==true) {
                        if(i==0) ImageView_d1.setImageResource(R.drawable.cloud_thunder_rain);
                        else if(i==1) ImageView_d2.setImageResource(R.drawable.cloud_thunder_rain);
                        else if(i==2) ImageView_d3.setImageResource(R.drawable.cloud_thunder_rain);
                    }
                    else if (d1d2d3_rain[i]==false && d1d2d3_thunder[i]==false) {
                        if(i==0) ImageView_d1.setImageResource(R.drawable.rain);
                        else if(i==1) ImageView_d2.setImageResource(R.drawable.rain);
                        else if(i==2) ImageView_d3.setImageResource(R.drawable.rain);
                    }
                }
            }
        };
    }

    private void place_renew() throws JSONException {
        if(myjs_array!=null) {
            for(int i=0; i<7; i++){
                JSONObject myjObject = myjs_array.getJSONObject(23+index*30+i);
                temperature_morning[i] = myjObject.getString("temperature");

                myjObject = myjs_array.getJSONObject(31+index*30+i);
                temperature_night[i] = myjObject.getString("temperature");

                myjObject = myjs_array.getJSONObject(38+index*30+i);
                weather_morning[i] = myjObject.getString("Rain");

                myjObject = myjs_array.getJSONObject(45+index*30+i);
                weather_night[i] = myjObject.getString("Rain");

                Toast.makeText(getApplicationContext(),"inside", Toast.LENGTH_SHORT).show();
            }

            textView_d1.setText("早上\n" + "氣溫：" + temperature_morning[0] + "\n天氣：" + weather_morning[0] +
                    "晚上\n" + "氣溫：" + temperature_night[0]   + "\n天氣：" + weather_night[0]);
            textView_d2.setText("早上\n" + "氣溫：" + temperature_morning[1] + "\n天氣：" + weather_morning[1] +
                    "晚上\n" + "氣溫：" + temperature_night[1]   + "\n天氣：" + weather_night[1]);
            textView_d3.setText("早上\n" + "氣溫：" + temperature_morning[0] + "\n天氣：" + weather_morning[0] +
                    "晚上\n" + "氣溫：" + temperature_night[1]   + "\n天氣：" + weather_night[1]);
        }

    }
}
