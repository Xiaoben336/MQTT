package com.example.zjf.mqtt.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.zjf.mqtt.R;
import com.example.zjf.mqtt.bean.IceServerInfo;
import com.example.zjf.mqtt.event.OnAcceptMessageEvent;

import com.example.zjf.mqtt.event.OnByeMessageEvent;
import com.example.zjf.mqtt.event.OnConnectMessageEvent;
import com.example.zjf.mqtt.notification.common.IceServer;
import com.example.zjf.mqtt.service.CoreApi;
import com.example.zjf.mqtt.util.CallUtils;
import com.example.zjf.mqtt.util.PreferenceUtil;
import com.example.zjf.mqtt.util.PreferenceUtils;
import com.example.zjf.mqtt.view.PercentFrameLayout;
import com.tbruyelle.rxpermissions.RxPermissions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.LinkedList;

import rx.functions.Action1;

public class VideoCallActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "VideoCallActivity";
    private Context context;
    private String localClientId;//本地ClientId
    private String friendClientId;//远端ClientId
    private String topic;//客户端发布消息时的主题，和localClientId相同
    private boolean isCallOut;//判断呼入呼出
    private boolean iceConnected = false;//是否连接了Ice

    private RendererCommon.ScalingType scalingType;//充满布局或者自适应
    private RelativeLayout rl_video;//整个画面

    private RelativeLayout remoteRenderLayout;//远端视频
    private SurfaceViewRenderer remoteRender;//远端视频的SurfaceViewRenderer
    private TextView remoteVideoName;//远端视频的名字

    private PercentFrameLayout localRenderLayout;//本地视频
    private SurfaceViewRenderer localRender;//本地视频的SurfaceViewRenderer

    private RelativeLayout local_video_layout_2;
    private TextView local_video_name;//本地视频的名字
    //呼叫方的挂断按钮和挂断文字
    private ImageView iv_call_out_hangup;
    private TextView tv_call_out_hangup;

    private TextView tv_status;//显示呼叫状态

    private LinearLayout ll_call_in_iv;//显示接听界面的接听挂断按钮
    private ImageView iv_call_in_hangup;//接听方挂断按键
    private ImageView iv_call_in_accept;//接听方接听按钮

    private LinearLayout ll_call_in_tv;//显示接听界面的接听挂断文字
    private TextView tv_call_in_hangup;;//接听方挂断文字
    private TextView tv_call_in_accept;;//接听方接听文字

    private TextView tv_monitor;//监控信息
    private TextView tv_tips;
    private TextView tv_lowWifi;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_videocall);
        iceConnected = false;
        isCallOut = getIntent().getBooleanExtra("isCallOut",false);
        friendClientId = getIntent().getStringExtra("friendClientId");
        localClientId = PreferenceUtils.getValue(context,"localClientId","");

        CallUtils.getInst().setSendClientId(localClientId);
        initView();

        scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;//充满布局

        iv_call_out_hangup.setOnClickListener(this);
        iv_call_in_hangup.setOnClickListener(this);
        iv_call_in_accept.setOnClickListener(this);
        updateVideoView();
        /*remoteClientId = PreferenceUtils.getValue(context,"remoteClientId","");*/
        Log.d(TAG,"localClientId === " + localClientId +"   friendClientId === " + friendClientId);
        topic = localClientId;
        CoreApi.initCall(this,240,320,localRender);
        if (isCallOut) {
            startCall();
        } else {
            Log.d(TAG,"接听方");
        }
        requestPermissions();
    }

    private void requestPermissions() {
        RxPermissions.getInstance(context).request("android.permission.MODIFY_AUDIO_SETTINGS",
                "android.permission.RECORD_AUDIO",
                "android.permission.INTERNET",
                "android.permission.CAMERA")
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            Log.d(TAG,"请求权限成功");
                        } else {

                        }
                    }
                });
    }

    boolean iscall = false;
    private void startCall() {
        Log.d(TAG,"private void startCall()  TOPIC : friendClientId === " + topic + " : " + friendClientId);
        if (iscall) return;

        iscall = true;

        CoreApi.inviteCall(topic,friendClientId,remoteRender);
        //CallUtils.getInst().startCall(remoteRender,topic,localClientId,true);
    }


    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (localRender != null) {
            localRender.release();
            localRender = null;
        }

        if (remoteRender != null) {
            remoteRender.release();
            remoteRender = null;
        }
        finish();
    }

    private void initView(){
        rl_video = (RelativeLayout) findViewById(R.id.rl_video);

        remoteRenderLayout = (RelativeLayout) findViewById(R.id.remote_video_layout);
        remoteRender = (SurfaceViewRenderer) findViewById(R.id.remote_video_view);
        remoteVideoName = (TextView) findViewById(R.id.remote_video_name);

        localRenderLayout = (PercentFrameLayout) findViewById(R.id.local_video_layout);
        localRender = (SurfaceViewRenderer) findViewById(R.id.local_video_view);

        local_video_layout_2 = (RelativeLayout) findViewById(R.id.local_video_layout_2);
        local_video_name = (TextView) findViewById(R.id.local_video_name);

        iv_call_out_hangup = (ImageView) findViewById(R.id.iv_call_out_hangup);
        tv_call_out_hangup = (TextView) findViewById(R.id.tv_call_out_hangup);

        tv_status = (TextView) findViewById(R.id.tv_status);

        ll_call_in_iv = (LinearLayout) findViewById(R.id.ll_call_in_iv);
        iv_call_in_hangup = (ImageView) findViewById(R.id.iv_call_in_hangup);
        iv_call_in_accept = (ImageView) findViewById(R.id.iv_call_in_accept);

        ll_call_in_tv = (LinearLayout) findViewById(R.id.ll_call_in_tv);
        tv_call_in_hangup = (TextView) findViewById(R.id.tv_call_in_hangup);
        tv_call_in_accept = (TextView) findViewById(R.id.tv_call_in_accept);

        tv_monitor = (TextView) findViewById(R.id.tv_monitor);
        tv_tips = (TextView) findViewById(R.id.tv_tips);
        tv_lowWifi = (TextView) findViewById(R.id.tv_lowWifi);

        if (isCallOut) {//呼出
            Log.d(TAG,"呼出");
            ll_call_in_iv.setVisibility(View.GONE);
            ll_call_in_tv.setVisibility(View.GONE);
            iv_call_in_hangup.setVisibility(View.GONE);
            iv_call_in_accept.setVisibility(View.GONE);
            tv_call_in_accept.setVisibility(View.GONE);
            tv_call_in_hangup.setVisibility(View.GONE);
        } else {
            Log.d(TAG,"呼入");
            iv_call_out_hangup.setVisibility(View.GONE);
            tv_call_out_hangup.setVisibility(View.GONE);
        }
    }

    public static void startActivity(Context context,String friendClientId,boolean isCallOut){
        Intent intent = new Intent(context,VideoCallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("isCallOut",isCallOut);
        intent.putExtra("friendClientId",friendClientId);
        context.startActivity(intent);
    }

    private void accept() {
        if (iv_call_in_accept != null) {
            iv_call_in_accept.setEnabled(false);
        }
        ll_call_in_tv.setVisibility(View.GONE);
        ll_call_in_iv.setVisibility(View.GONE);
        iv_call_out_hangup.setVisibility(View.VISIBLE);
        tv_call_out_hangup.setVisibility(View.VISIBLE);

       //CoreApi.initCall(this,720,1280,localRender);
        new Thread(new Runnable() {
            @Override
            public void run() {
                CoreApi.acceptCall(topic,friendClientId,remoteRender);
            }
        }).start();
        CallUtils.getInst().initAudio();
       /* tv_status.setText("正在接通，请稍后...");*/
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccept(OnAcceptMessageEvent messageEvent){
        Log.d(TAG,"onAccept " + messageEvent.getMessage());
        this.getAccept(messageEvent.getMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnect(OnConnectMessageEvent messageEvent){
        Log.d(TAG,"onConnect");
        this.getConnect(messageEvent.getTopic());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBye(OnByeMessageEvent messageEvent){
        Log.d(TAG,"onBye");
        this.getBye(messageEvent.getMessage());
    }

    private void getBye(String message) {
        Log.d(TAG," getBye  message === " + message);
        if (message.equals("")) {
            iscall = false;
            tv_tips.setText("当前通话结束");

            if (localRender != null) {
                localRender.release();
                localRender = null;
            }

            if (remoteRender != null) {
                remoteRender.release();
                remoteRender = null;
            }

            finish();
        }
    }

    private void getAccept(String message) {
        tv_status.setText(message +"已接听，正在接通电话");
    }

    private void getConnect(String topic) {
        iceConnected = true;
        updateVideoView();
    }

    private void updateVideoView(){
        if (CallUtils.getInst().getFriendClientId() != null) {
            Log.d(TAG,"updateVideoView === ");
            remoteRender.setScalingType(scalingType);
            remoteRender.setMirror(false);
        } else {
            Log.d(TAG,"updateVideoView ======");
            remoteRenderLayout.setVisibility(View.GONE);
        }

        if (remoteRender != null) {
            remoteRender.requestLayout();
        }
        Log.d(TAG,"iceConnected === " + iceConnected);
        if (iceConnected) {
            if (localRenderLayout.getChildCount() != 0) {
                ((PercentFrameLayout)localRender.getParent()).removeAllViews();
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) local_video_layout_2.getLayoutParams();
                int screenWidth = getScreenWidth(this);
                int screenHeight = getScreenHeight(this);
                int actual = screenWidth > screenHeight ? screenHeight : screenWidth;
                lp.height = (int) (actual /4.0f);
                lp.width = (int) (actual / 4.0f);

                local_video_layout_2.setLayoutParams(lp);
                local_video_layout_2.addView(localRender);

                lp = new FrameLayout.LayoutParams((int) (actual / 4.0f),FrameLayout.LayoutParams.WRAP_CONTENT);
                lp.gravity = Gravity.BOTTOM;
                local_video_name.setLayoutParams(lp);
            }
            localRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            if (remoteVideoName != null) {
                remoteVideoName.setText(CallUtils.getInst().getFriendClientId());
            }

            if (local_video_name != null) {
                local_video_name.setText(localClientId);
            }
        } else {
            localRenderLayout.setPosition(0,0,150,250);
            localRender.setScalingType(scalingType);
        }
        localRender.setMirror(false);
        localRender.requestLayout();
    }

    private int getScreenWidth(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getWidth();
    }

    //获取屏幕的高度
    private int getScreenHeight(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getHeight();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_call_out_hangup:
                    Log.d(TAG,"iv_call_out_hangup一直发");
                    CoreApi.byeAll(topic);
                    finish();
                break;
            case R.id.iv_call_in_hangup:
                    Log.d(TAG,"iv_call_in_hangup一直发");
                    CoreApi.byeAll(topic);
                break;
            case R.id.iv_call_in_accept:
                accept();
                break;
            default:
                break;
        }
    }

}
