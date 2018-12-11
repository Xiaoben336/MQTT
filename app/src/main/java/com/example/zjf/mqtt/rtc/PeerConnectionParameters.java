package com.example.zjf.mqtt.rtc;

public class PeerConnectionParameters {
    public static boolean videoCallEnabled;
    public static boolean loopback;
    public static boolean tracing;
    public static int videoWidth;
    public static int videoHeight;
    public static int videoFps;
    public static int videoMaxBitrate;
    public static String videoCodec;
    public static boolean videoCodecHwAcceleration;
    public static boolean videoFlexfecEnabled;
    public static int audioStartBitrate;
    public static String audioCodec;
    public static boolean noAudioProcessing;
    public static boolean aecDump;
    public static boolean useOpenSLES;
    public static boolean disableBuiltInAEC;
    public static boolean disableBuiltInAGC;
    public static boolean disableBuiltInNS;
    public static boolean enableLevelControl;

    public PeerConnectionParameters(boolean videoCallEnabled, boolean loopback, boolean tracing,
                                    int videoWidth, int videoHeight, int videoFps, int videoMaxBitrate, String videoCodec,
                                    boolean videoCodecHwAcceleration, boolean videoFlexfecEnabled, int audioStartBitrate,
                                    String audioCodec, boolean noAudioProcessing, boolean aecDump, boolean useOpenSLES,
                                    boolean disableBuiltInAEC, boolean disableBuiltInAGC, boolean disableBuiltInNS,
                                    boolean enableLevelControl) {
        this.videoCallEnabled = videoCallEnabled;
        this.loopback = loopback;
        this.tracing = tracing;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.videoFps = videoFps;
        this.videoMaxBitrate = videoMaxBitrate;
        this.videoCodec = videoCodec;
        this.videoFlexfecEnabled = videoFlexfecEnabled;
        this.videoCodecHwAcceleration = videoCodecHwAcceleration;
        this.audioStartBitrate = audioStartBitrate;
        this.audioCodec = audioCodec;
        this.noAudioProcessing = noAudioProcessing;
        this.aecDump = aecDump;
        this.useOpenSLES = useOpenSLES;
        this.disableBuiltInAEC = disableBuiltInAEC;
        this.disableBuiltInAGC = disableBuiltInAGC;
        this.disableBuiltInNS = disableBuiltInNS;
        this.enableLevelControl = enableLevelControl;
    }
}
