package com.example.zjf.mqtt.notification;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.zjf.mqtt.MyApplication;
import com.example.zjf.mqtt.activity.VideoCallActivity;
import com.example.zjf.mqtt.notification.common.IceServer;
import com.example.zjf.mqtt.util.JsonUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class NotificationService {
    private Context context;
    private static final String TAG = "NotificationService";
    private static final int STATUS_NOTIFICATION_SERVICE_INIT = 1;
    private static final int STATUS_NOTIFICATION_SERVICE_START = 2;
    private static final int STATUS_NOTIFICATION_SERVICE_READY = 3;

    private static NotificationService inst;
    //通知处理器列表
    List<NotificationItem> notifyList = null;
    //通知处理锁
    Lock notifyLock = new ReentrantLock();
    //使用主线程处理通知
    Handler handlerMessage;
    int status = STATUS_NOTIFICATION_SERVICE_INIT;

    public NotificationService(Context context){
        this.context = context;
        status = STATUS_NOTIFICATION_SERVICE_START;
        notifyList = new ArrayList<NotificationItem>();
        startService();
        status = STATUS_NOTIFICATION_SERVICE_READY;
    }

    private void startService(){
        handlerMessage = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                NotificationProcessItem processItem = (NotificationProcessItem)message.obj;
                processItem.process();
                return false;
            }
        });
        addNotificationProcessor(NotificationTypeDef.C2C_NOTIFY_TYPE_WEBRTC,new WebRTCNotificationProcessImp());
        addNotificationProcessor(NotificationTypeDef.C2C_NITIFY_TYPE_CALL,new CallNotificationProcessImp());
        setProcessor(new Handler());
    }

    Handler callbackHandler = null;
    public void setProcessor(Handler callbackHandler){
        this.callbackHandler = callbackHandler;
    }

    public void processMessage(String data){
        Log.d(TAG,"CURRENT STATUS === " + status);
        if (status != STATUS_NOTIFICATION_SERVICE_READY)
            return;

        notifyLock.lock();
        try{
            Notification notification = (Notification) JsonUtils.jsonToBean(data,Notification.class);

            if (notification.getNotify_type() == NotificationTypeDef.C2C_NOTIFY_TYPE_WEBRTC) {
                WebRtcNotification obj = (WebRtcNotification) JsonUtils.jsonToBean(data,WebRtcNotification.class);
                LinkedList<IceServer> iceServers = new LinkedList<IceServer>();

                List<IceServer> p_iceServers = obj.getIceServers();
                if (p_iceServers != null) {
                    for (IceServer p_server : p_iceServers) {
                        IceServer server = new IceServer(p_server.uri,p_server.username,p_server.password);//有问题
                        iceServers.add(server);
                    }
                    obj.setIceServers(iceServers);
                }
                data = JsonUtils.objectToJson(obj);
            } else if (notification.getNotify_type() == NotificationTypeDef.C2C_NITIFY_TYPE_CALL) {
                CallNotification obj = (CallNotification) JsonUtils.jsonToBean(data,CallNotification.class);
                data = JsonUtils.objectToJson(obj);
                /*if (msg.equals("CALL_OUT")) {
                    Log.d(TAG,"收到了CALL_OUT");
                    VideoCallActivity.startActivity(context,false);
                } else if (msg.equals("CALL_IN")){
                    Log.d(TAG,"没有收到了CALL_OUT");
                }*/
            }

            for (NotificationItem item : notifyList) {
                if (item.notificationType == notification.getNotify_type()) {
                    final NotificationProcessItem processItem = new NotificationProcessItem();
                    processItem.item = item;
                    processItem.msg = new String(data);

                    if (callbackHandler != null) {
                        callbackHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                processItem.process();
                            }
                        });
                    } else {
                        Message msg = new Message();
                        msg.obj = processItem;
                        handlerMessage.sendMessage(msg);
                    }
                }
            }
        }finally {
            notifyLock.unlock();
        }
    }

    public void addNotificationProcessor(int notiId, NotificationProcessItf processor) {
        for(NotificationItem t:notifyList){
            if((t.notificationType == notiId) && (t.processor.getClass().getName().equals(processor.getClass().getName()))){
                return;
            }
        }
        NotificationItem item = new NotificationItem();
        item.notificationType = notiId;
        item.processor = processor;
        notifyList.add(item);
    }
    public static NotificationService getInstance(Context context){
        if (inst == null) {
            inst = new NotificationService(context);
        }
        return inst;
    }

    private class NotificationItem{
        int notificationType;
        NotificationProcessItf processor;
    }

    private class NotificationProcessItem{
        NotificationItem item;
        String msg;

        public void process(){
            item.processor.process(msg);
            msg = null;
        }
    }
}
