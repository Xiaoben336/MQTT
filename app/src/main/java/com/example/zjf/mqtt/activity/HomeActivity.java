package com.example.zjf.mqtt.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.zjf.mqtt.R;
import com.example.zjf.mqtt.mqtt.MqttUtils;
import com.example.zjf.mqtt.util.PreferenceUtil;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener{
    private Button btn_sub,btn_video_chat;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle("首页");
        btn_sub = (Button) findViewById(R.id.btn_sub);

        btn_video_chat = (Button) findViewById(R.id.btn_video_chat);
        btn_sub.setOnClickListener(this);
        btn_video_chat.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_sub:
                SubscriberActivity.startActivity(HomeActivity.this);
                break;
            case R.id.btn_video_chat:
                alert_edit();
                break;
                default:
                    break;
        }
    }

    public void alert_edit(){
        final EditText et = new EditText(this);
        new AlertDialog.Builder(this).setTitle("请输入对方ClientId")
                .setIcon(android.R.drawable.sym_def_app_icon)
                .setView(et)
                .setPositiveButton("呼叫", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        PreferenceUtil.getInstance().setString("friendClientId",et.getText().toString().trim());
                        MqttUtils.getInstance(HomeActivity.this).subscribe(et.getText().toString().trim());
                        VideoCallActivity.startActivity(HomeActivity.this,et.getText().toString().trim(),true);
                    }
                }).setNegativeButton("取消",null).show();
    }

    public static void startActivity(Context context){
        Intent intent = new Intent(context,HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
