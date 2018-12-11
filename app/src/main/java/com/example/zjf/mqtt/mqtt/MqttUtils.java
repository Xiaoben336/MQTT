package com.example.zjf.mqtt.mqtt;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.zjf.mqtt.MyApplication;
import com.example.zjf.mqtt.activity.MainActivity;
import com.example.zjf.mqtt.callback.ConnectCallBackHandler;
import com.example.zjf.mqtt.callback.MqttCallbackHandler;
import com.example.zjf.mqtt.callback.PublishCallBackHandler;
import com.example.zjf.mqtt.callback.SubscriberCallBackHandler;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MqttUtils {
    private String host = "47.107.110.212";
    private String port = "1883";
    private static final String TAG = "MqttUtils";
    private Context context;
    private static MqttUtils instance;
    private MqttConnection mqttConnection;
    private String clientId;
    private Lock lock = new ReentrantLock();
    private Set<String> topicSet = new HashSet<String>();
    private MqttConnection.ConnectionStatus connectionStatus = MqttConnection.ConnectionStatus.DISCONNECTED;

    public MqttUtils(Context context) {
        this.context = context;
    }

    public static MqttUtils getInstance(Context context){
        if (instance == null) {
            instance = new MqttUtils(context);
        }
        return instance;
    }

    public static void startConnect(String clientId){
        instance.init(clientId);
    }

    private void init(String clientId) {
        CreateConnect(clientId);
    }

    /**
     * 连接到服务器
     * @param clientId
     */
    private void CreateConnect(String clientId) {
        this.clientId = clientId;

        //服务器地址
        String  uri ="tcp://";
        uri = uri + host + ":" + port;
        Log.d(TAG,"URI === " + uri + "   ClientID === " + clientId);

        MqttAndroidClient client = new MqttAndroidClient(context,uri,clientId);

        mqttConnection = new MqttConnection(clientId,host,port,client,false);

        MqttConnectOptions conOpt = new MqttConnectOptions();

        conOpt.setConnectionTimeout(30);//连接超时时间
        conOpt.setKeepAliveInterval(60);//心跳间隔时间 100 S

        client.setCallback(new MqttCallbackHandler(context,clientId));//连接后的回调
        //开始连接到服务器
        try {
            client.connect(conOpt,null,new ConnectCallBackHandler(context));
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     *              发布功能
     * @param topic
     * @param message
     */
    public void publish(String topic,String message){
        lock.lock();
        try {
            if (mqttConnection == null) {
                Log.d(TAG,"mqttConnection为空");
                return;
            }

            try {
                mqttConnection.getClient().publish(topic,message.getBytes(),0,false,null, new PublishCallBackHandler(context));
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }


    /**
     *              订阅功能
     * @param topic
     */
    public void subscribe(String topic) {
        lock.lock();
        try {
            if (topicSet.contains(topic)) {
                return;
            }
            if (topic == null || topic.isEmpty()) {
                return;
            }
            topicSet.add(topic);
            if (mqttConnection == null)
                return;
            String[] split = topic.split(",");
            int length = split.length;//主题数
            String[] topics = new String[length];//订阅的主题
            int[] qoss = new int[length];//服务器的质量
            for (int i = 0;i < length;i++) {
                topics[i] = split[i];
                qoss[i] = 0;
            }
            try {

                if (length > 0) {
                    //订阅多个主题，服务器质量默认为0
                    Log.d(TAG,"topics === " + Arrays.toString(topics));
                    mqttConnection.getClient().subscribe(topics,qoss,context,new SubscriberCallBackHandler(context));
                } else {
                    Log.d(TAG,"topic === " + topic);
                    mqttConnection.getClient().subscribe(topic,0,context,new SubscriberCallBackHandler(context));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }


    public String getClientId() {
        if (clientId != null) {
            Log.d(TAG,"getClientId == " + clientId);
            return clientId;
        }
        return "";
    }
}
