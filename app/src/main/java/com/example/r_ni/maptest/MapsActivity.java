package com.example.r_ni.maptest;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;

import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.app.Activity;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Marker;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.charts.LineChart;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

public class MapsActivity extends Activity implements OnMapReadyCallback{

    ///////////the thing of server///////////
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

    //////////////the things of map///////////////
    GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        MapFragment mapFrag;
        final MapFragment mapfragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapfragment.getMapAsync(this);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("/////////////////////////////");
                try {
                    MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://user:cssalab2017@140.116.154.88/?authSource=cssalab"));
                    MongoDatabase db = mongoClient.getDatabase("pm25");
                    //for(String colName: db.listCollectionNames())
                    //System.out.println(colName);
                    MongoCollection<Document> collection = db.getCollection("airbox");
                    /**
                     * 1. 获取迭代器FindIterable<Document>
                     * 2. 获取游标MongoCursor<Document>
                     * 3. 通过游标遍历检索出的文档集合
                     * */
                    FindIterable<Document> findIterable = collection.find();
                    MongoCursor<Document> mongoCursor = findIterable.iterator();
                    MongoCursor<Document> cursor = collection.find().iterator();
                    try{
                        while (cursor.hasNext()) {
                            cursor.next().toJson();
                            JSONObject myjObject = new JSONObject(cursor.next().toJson());
                            System.out.println(cursor.next().toJson());
                            System.out.println(myjObject.getString("gps_lon"));

                            String lat = myjObject.getString("gps_lat");
                            String lon = myjObject.getString("gps_lon");
                            String pm25 = myjObject.getString("Pm25");
                            final String place = myjObject.getString("site");
                            final float f_lat = Float.parseFloat(lat);
                            final float f_lon = Float.parseFloat(lon);
                            final float f_pm25 = Float.parseFloat(pm25);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DoMarker(f_lat, f_lon, f_pm25, place);
                                }
                            });

                            //DoMarker(f_lat, f_lon, f_pm25, place);
                        }
                    }catch (Exception e) {
                        System.out.println(e.toString());
                    }
                } catch (Exception e) {
                    System.err.println(e.getClass().getName() + ": " + e.getMessage());
                    System.out.println(e.toString());
                }
            }
        });

        thread.start();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Add a marker in Sydney and move the camera
        mMap = googleMap;
        LatLng sydney = new LatLng(23.973875, 120.982024);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        setUpMap();
    }

    public void DoMarker(final float lat, final float lon, float pm25, String site){
        System.out.println("********lat is "+lat+"********lon is "+lon);

        BitmapDescriptor descriptor = null;
        if(pm25<=50.0) descriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        else if(pm25<=100.0) descriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        else if(pm25<=150.0) descriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
        else if(pm25<=200.0) descriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        else descriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);


        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).icon(descriptor).snippet("the pm2.5 data").title(site));

        //編輯marker內的資訊
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            //使用版面配置marker.xml的資料
            @Override
            public View getInfoContents(Marker marker) {
                View v = getLayoutInflater().inflate(R.layout.marker, null);

                TextView title = (TextView)v.findViewById(R.id.title);
                TextView snippet = (TextView)v.findViewById(R.id.snippet);
                TextView place = (TextView)v.findViewById(R.id.place);
                LineChart chart_line = (LineChart)v.findViewById(R.id.chart_line);

                title.setText("Place: " + marker.getTitle());
                snippet.setText(marker.getSnippet());
                place.setText("經度: " + lon + "緯度: " + lat);
                chart_line.setData(line_chart_initialize());

                return v;
            }
        });
    }

    // 移動地圖到參數指定的位置
    private void moveMap(LatLng place) {
        // 建立地圖攝影機的位置物件
        CameraPosition cameraPosition =
                new CameraPosition.Builder()
                        .target(place)
                        .zoom(7)
                        .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void setUpMap() {
        // 建立位置的座標物件
        LatLng place = new LatLng(23.973875, 120.982024);
        // 移動地圖
        moveMap(place);
    }
    /*
    //建立折線圖的資料
    private List<Entry> getChartData(){
        final int DATA_COUNT = 5;

        List<Entry> chartData = new ArrayList<>();
        for(int i=0;i<DATA_COUNT;i++){
            chartData.add(new Entry(i*2, i));
        }
        return chartData;
    }

    //建立X Label
    private List<String> getLabels(){
        List<String> chartLabels = new ArrayList<>();
        for(int i=0;i<5;i++){
            chartLabels.add("X"+i);
        }
        return chartLabels;
    }

    //產生一組DataSet，將它整合到圖表資料(LineData)裡
    private LineData getLineData(){
        LineDataSet dataSetA = new LineDataSet(getChartData(), "LabelA");

        List<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSetA); // add the datasets

        return new LineData(getLabels(), dataSets);
    }
    */
    private LineData line_chart_initialize(){
        ArrayList<Entry> lineEntries = new ArrayList<Entry>();
        lineEntries.add(new Entry(0, 100));
        lineEntries.add(new Entry(1, 50));
        lineEntries.add(new Entry(2, 22));
        lineEntries.add(new Entry(3, 8));
        lineEntries.add(new Entry(4, 12));
        lineEntries.add(new Entry(5, 10));

        LineDataSet lineDataSet1 = new LineDataSet(lineEntries, "Line1");

        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(lineDataSet1);

        LineData data = new LineData(dataSets);

        return data;
    }

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
                    socket = new Socket("172.20.10.5", 9998);
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
                if(response!=null){
                    try{
                        myjs_array  = new JSONArray(response);
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(),"response is null", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }

                for(int i=0; i<myjs_array.length(); i++) {
                    JSONObject myjObject = null;
                    try {
                        myjObject = myjs_array.getJSONObject(i);
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), "jsonarray[i] is null", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }

                    try {
                        String lat = myjObject.getString("gps_lat");
                        String lon = myjObject.getString("gps_lon");
                        String pm25 = myjObject.getString("Pm25");
                        String place = myjObject.getString("site");
                        final float f_lat = Float.parseFloat(lat);
                        final float f_lon = Float.parseFloat(lon);
                        float f_pm25 = Float.parseFloat(pm25);
                        BitmapDescriptor descriptor = null;
                        if(f_pm25<=50.0) descriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
                        else if(f_pm25<=100.0) descriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
                        else if(f_pm25<=150.0) descriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
                        else if(f_pm25<=200.0) descriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                        else descriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);
                        mMap.addMarker(new MarkerOptions().position(new LatLng(f_lat, f_lon)).icon(descriptor).snippet("the pm2.5 data").title(place));

                        //編輯marker內的資訊
                        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                            @Override
                            public View getInfoWindow(Marker marker) {
                                return null;
                            }

                            //使用版面配置marker.xml的資料
                            @Override
                            public View getInfoContents(Marker marker) {
                                View v = getLayoutInflater().inflate(R.layout.marker, null);

                                TextView title = (TextView)v.findViewById(R.id.title);
                                TextView snippet = (TextView)v.findViewById(R.id.snippet);
                                TextView place = (TextView)v.findViewById(R.id.place);
                                LineChart chart_line = (LineChart)v.findViewById(R.id.chart_line);

                                title.setText("Place: " + marker.getTitle());
                                snippet.setText(marker.getSnippet());
                                place.setText("經度: " + f_lon + "緯度: " + f_lat);
                                chart_line.setData(line_chart_initialize());

                                return v;
                            }
                        });

                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), "can't find that index", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
        };
    }


}
