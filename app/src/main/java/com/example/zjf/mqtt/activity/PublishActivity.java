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
import com.example.zjf.mqtt.callback.PublishCallBackHandler;
import com.example.zjf.mqtt.event.MessageEvent;
import com.example.zjf.mqtt.mqtt.MqttUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class PublishActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "PublishActivity";

    private EditText etPubTopic;
    private EditText etPubMessage;
    private Button btnStartPub;
    private RecyclerView rlvContent;

    private String pubTopic;
    private String pubMessage;
    private SubscriberAdapter subscriberAdapter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);
        setTitle("主题发布");
        initView();
        initData();
        btnStartPub.setOnClickListener(this);
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


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        String string = event.getStr();
        //接收到服务器推送的消息，显示在右边
        if ("".equals(string)) {
            String topic = event.getStrTopic();
            MqttMessage mqttMessage = event.getMqttMessage();
            String s = new String(mqttMessage.getPayload());
            topic = topic + " : " + s;
            subscriberAdapter.addListDate(new Message(topic,false));
        } else {//接收到订阅成功的通知,订阅成功，显示在左边
            subscriberAdapter.addListDate(new Message("Me : " + string,true));
        }
    }


    private void initView() {
        etPubTopic = (EditText) findViewById(R.id.ed_pub_topic);
        etPubMessage = (EditText) findViewById(R.id.ed_pub_message);
        btnStartPub = (Button) findViewById(R.id.btn_start_pub);
        rlvContent = (RecyclerView) findViewById(R.id.rlv_content_pub);
    }

    private void initData(){
        List<Message> list = new ArrayList<>();
        if (subscriberAdapter == null) {
            subscriberAdapter = new SubscriberAdapter(list,this);
            rlvContent.setLayoutManager(new LinearLayoutManager(this));
            rlvContent.setAdapter(subscriberAdapter);
        } else {
            subscriberAdapter.notifyDataSetChanged();
        }
    }


    public static void startActivity(Context context){
        Intent intent = new Intent(context,PublishActivity.class);
        context.startActivity(intent);
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start_pub:
                    //publishTopic();
                    MqttUtils.getInstance(MyApplication.application).publish(etPubTopic.getText().toString().trim(),etPubMessage.getText().toString().trim());
                break;
                default:
                    break;
        }
    }

    /*private void publishTopic(){
        //获取发布的主题
        pubTopic = etPubTopic.getText().toString().trim();
        //获取发布的消息
        pubMessage = etPubMessage.getText().toString().trim();
        //消息的服务质量
        int qos = 0;
        //消息是否保持
        boolean retain = false;
        //要发布的消息内容
        byte[] message = pubMessage.getBytes();

        if (pubTopic != null && !"".equals(pubTopic)) {//不为空且不为空格
            //获取Client对象
            MqttAndroidClient client = MainActivity.getMqttAndroidClientInstance();

            if (client != null) {
                try {
                    client.publish(pubTopic,message,qos,retain,null,new PublishCallBackHandler(this));
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG,"MqttAndroidClient === null");
            }
        } else {
            Toast.makeText(PublishActivity.this,"发布的主题不能为空",Toast.LENGTH_SHORT).show();
        }
    }*/
}
