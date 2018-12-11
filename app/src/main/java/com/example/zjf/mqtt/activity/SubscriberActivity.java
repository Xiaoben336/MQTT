package com.example.zjf.mqtt.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.zjf.mqtt.MyApplication;
import com.example.zjf.mqtt.R;
import com.example.zjf.mqtt.adapter.SubscriberAdapter;
import com.example.zjf.mqtt.bean.Message;
import com.example.zjf.mqtt.callback.SubscriberCallBackHandler;
import com.example.zjf.mqtt.event.MessageEvent;
import com.example.zjf.mqtt.mqtt.MqttConnection;
import com.example.zjf.mqtt.mqtt.MqttUtils;
import com.example.zjf.mqtt.util.PreferenceUtil;
import com.example.zjf.mqtt.util.PreferenceUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubscriberActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SubscriberActivity";
    private EditText etTopic;
    private Context context;
    private Button btnStartSub;
    private RecyclerView rlvContent;
    private String topic = null;
    private SubscriberAdapter subscriberAdapter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriber);
        context = this;
        setTitle("订阅");
        initView();
        initData();
        btnStartSub.setOnClickListener(this);
    }

    private void initView() {
        etTopic = (EditText) findViewById(R.id.et_topic);
        btnStartSub = (Button) findViewById(R.id.btn_start_sub);
        rlvContent = (RecyclerView) findViewById(R.id.rlv_content_sub);
    }
    private void initData() {
        List<Message> list = new ArrayList<>();

        if (subscriberAdapter == null){
            subscriberAdapter = new SubscriberAdapter(list,this);
            rlvContent.setLayoutManager(new LinearLayoutManager(this));
            rlvContent.setAdapter(subscriberAdapter);
        } else {
            subscriberAdapter.notifyDataSetChanged();
        }
    }

    public static void startActivity(Context context){
        Intent intent = new Intent(context,SubscriberActivity.class);
        context.startActivity(intent);
    }


    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_start_sub:
                MqttUtils.getInstance(context).subscribe(etTopic.getText().toString().trim());
                break;
                default:
                    break;
        }
    }

    /*private void subscriberTopic() {
        topic = etTopic.getText().toString().trim();//获取要订阅的主题
        if (topic == null || "".equals(topic)) {
            Toast.makeText(SubscriberActivity.this,"请输入订阅的主题",Toast.LENGTH_SHORT).show();
            return;
        }

        String[] split = topic.split(",");
        int length = split.length;//主题数
        String[] topics = new String[length];//订阅的主题
        int[] qos = new int[length];//服务器的质量
        for (int i = 0;i < length;i++) {
            topics[i] = split[i];
            qos[i] = 0;
        }

        //获取Client对象
        MqttAndroidClient client = MainActivity.getMqttAndroidClientInstance();

        if (client != null) {
            try{
                if (length > 0) {
                    //订阅多个主题，服务器质量默认为0
                    Log.d(TAG,"topics === " + Arrays.toString(topics));
                    client.subscribe(topics,qos,null,new SubscriberCallBackHandler(this));
                } else {
                    Log.d(TAG,"topic === " + topic);
                    client.subscribe(topic,0,null,new SubscriberCallBackHandler(this));
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG,"MqttAndroidClient === null");
        }
    }*/

    /**
     * 运行在主线程
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        String string = event.getStr();
        /**接收到服务器推送的信息，显示在右边*/
        if("".equals(string)){
            String topic = event.getStrTopic();
            MqttMessage mqttMessage = event.getMqttMessage();
            String s = new String(mqttMessage.getPayload());
            topic = topic+" : "+s;
            Log.d(TAG,"topic + s === " + topic);
            subscriberAdapter.addListDate(new Message(topic,false));
            /**接收到订阅成功的通知,订阅成功，显示在左边*/
        }else{
            subscriberAdapter.addListDate(new Message("Me : " + string,true));
        }
    }
}
