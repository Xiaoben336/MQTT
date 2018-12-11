package com.example.zjf.mqtt.mqtt;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.PropertyResourceBundle;

public class MqttConnection {
    private String clientID = null;
    private String host = null;
    private String port = null;
    private MqttAndroidClient client;
    private boolean sslConnection = false;
    private ConnectionStatus status = ConnectionStatus.NONE;
    private MqttConnectOptions conOpt;
    public MqttConnection(String clientID, String host, String port, MqttAndroidClient client,boolean sslConnection){
        this.clientID = clientID;
        this.host = host;
        this.port = port;
        this.client = client;
        this.sslConnection = sslConnection;
    }

    public enum ConnectionStatus {
        /** Client is Connecting **/
        CONNECTING,
        /** Client is Connected **/
        CONNECTED,
        /** Client is Disconnecting **/
        DISCONNECTING,
        /** Client is Disconnected **/
        DISCONNECTED,
        /** Client has encountered an Error **/
        ERROR,
        /** Status is unknown **/
        NONE
    }

    public MqttAndroidClient getClient() {
        return client;
    }

    public String getClientID() {
        return clientID;
    }

    public boolean isConnection(){
        return status == ConnectionStatus.CONNECTED;
    }

    public void changeConnectionStatus(ConnectionStatus connectionStatus){
        status = connectionStatus;
    }
}
