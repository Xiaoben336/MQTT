package com.example.zjf.mqtt.activity;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.zjf.mqtt.MyApplication;
import com.example.zjf.mqtt.R;
import com.example.zjf.mqtt.callback.ConnectCallBackHandler;
import com.example.zjf.mqtt.callback.MqttCallbackHandler;
import com.example.zjf.mqtt.callback.SubscriberCallBackHandler;
import com.example.zjf.mqtt.mqtt.MqttUtils;
import com.example.zjf.mqtt.util.PreferenceUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Context context;
    private EditText etlocalClientId;
    private Button btnConnect;

    private String localClientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        etlocalClientId = (EditText) findViewById(R.id.et_local_client_id);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        setTitle("MQTT");
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"btnConnect Click");
                localClientId = etlocalClientId.getText().toString().trim();
                PreferenceUtils.putValue(context,"localClientId",localClientId);
                MqttUtils.startConnect(localClientId);
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
