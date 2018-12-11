package com.example.zjf.mqtt.notification.common;

public class IceServer implements Cloneable {
    final public String uri;
    final public String username;
    final public String password;


    public IceServer(String uri, String username, String password) {
        this.uri = uri;
        this.username = username;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getUri() {
        return uri;
    }

    public String getUsername() {
        return username;
    }

    @Override
    protected IceServer clone() throws CloneNotSupportedException {
        return (IceServer) super.clone();
    }
}
