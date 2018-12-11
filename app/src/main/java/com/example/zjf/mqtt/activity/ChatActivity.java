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

import com.example.zjf.mqtt.R;
import com.example.zjf.mqtt.adapter.SubscriberAdapter;
import com.example.zjf.mqtt.bean.Message;
import com.example.zjf.mqtt.callback.PublishCallBackHandler;
import com.example.zjf.mqtt.callback.SubscriberCallBackHandler;
import com.example.zjf.mqtt.event.MessageEvent;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ChatActivity";
    /**订阅*/
    private EditText edTopic;
    private Button btnStartSub;
    /**发布*/
    private EditText edPubTopic;
    private EditText edPubMessage;
    private Button btnStartPub;
    /**记录*/
    private RecyclerView rlvContent;

    private SubscriberAdapter subscriberAdapter;

    public String subtopic;
    private String pubTopic;
    private String pubMessage;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setTitle("聊天");
        initView();
        initData();
        btnStartSub.setOnClickListener(this);
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
        edTopic = (EditText) findViewById(R.id.ed_topic);
        btnStartSub = (Button) findViewById(R.id.btn_start_sub);

        edPubTopic = (EditText) findViewById(R.id.ed_pub_topic);
        edPubMessage = (EditText) findViewById(R.id.ed_pub_message);
        btnStartPub = (Button) findViewById(R.id.btn_start_pub);

        rlvContent = (RecyclerView) findViewById(R.id.rlv_content_chat);
    }
    private void initData() {
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
        Intent intent = new Intent(context,ChatActivity.class);
        context.startActivity(intent);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start_sub:
                    //subscriberTopic();
                break;
            case R.id.btn_start_pub:
                    //publishTopic();
                break;
                default:
                    break;
        }
    }

    /**
     * 订阅主题
     */
    /*private void subscriberTopic() {
        subtopic = edTopic.getText().toString().trim();//获取要订阅的主题
        if (subtopic == null || "".equals(subtopic)) {
            Toast.makeText(this,"请输入订阅的主题",Toast.LENGTH_SHORT).show();
            return;
        }

        String[] split = subtopic.split(",");
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
                    Log.d(TAG,"topic === " + subtopic);
                    client.subscribe(subtopic,0,null,new SubscriberCallBackHandler(this));
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG,"MqttAndroidClient === null");
        }
    }*/

    /*private void publishTopic(){
        //获取发布的主题
        pubTopic = edPubTopic.getText().toString().trim();
        //获取发布的消息
        pubMessage = edPubMessage.getText().toString().trim();
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
            Toast.makeText(this,"发布的主题不能为空",Toast.LENGTH_SHORT).show();
        }
    }*/
}
