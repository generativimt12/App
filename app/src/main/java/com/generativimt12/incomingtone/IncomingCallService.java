package com.generativimt12.incomingtone;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.InCallService;
import android.content.Intent;
import java.util.HashSet;
import java.util.Set;

public class IncomingCallService extends InCallService {
    private static IncomingCallService instance;
    private TonePlayer tonePlayer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<Call> ringingCalls = new HashSet<>();

    public static IncomingCallService getInstance(){ return instance; }

    public static Call getCurrentCall() {
        return instance == null ? null : instance.findActiveCall();
    }

    @Override public void onCreate() { super.onCreate(); instance=this; }

    @Override public void onCallAdded(Call call) {
        super.onCallAdded(call);
        call.registerCallback(new Call.Callback() {
            @Override public void onStateChanged(Call c,int state) {
                if(state==Call.STATE_RINGING){
                    ringingCalls.add(c); startTone(); showCallUi();
                } else if(state==Call.STATE_ACTIVE){
                    ringingCalls.remove(c); stopTone(); showCallUi();
                } else if(state==Call.STATE_DISCONNECTED){
                    ringingCalls.remove(c); stopTone();
                    if(findActiveCall()==null) InCallActivity.finishIfOpen();
                } else if(state==Call.STATE_DIALING || state==Call.STATE_CONNECTING){
                    stopTone(); showCallUi();
                }
            }
        },mainHandler);
        if(call.getState()==Call.STATE_RINGING){ ringingCalls.add(call); startTone(); }
        showCallUi();
    }

    @Override public void onCallRemoved(Call call){
        ringingCalls.remove(call); stopTone();
        if(findActiveCall()==null) InCallActivity.finishIfOpen();
        super.onCallRemoved(call);
    }

    @Override public void onBringToForeground(boolean showDialpad){ showCallUi(); }

    private Call findActiveCall(){
        for(Call c:getCalls()){
            int s=c.getState();
            if(s!=Call.STATE_DISCONNECTED && s!=Call.STATE_DISCONNECTING) return c;
        }
        return null;
    }

    private void showCallUi(){
        mainHandler.post(()->{
            Call c=findActiveCall();
            if(c!=null){
                Intent i=new Intent(this,InCallActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });
    }

    private void startTone(){ mainHandler.post(()->{
        if(tonePlayer==null){ tonePlayer=new TonePlayer(); tonePlayer.start(); }
    });}

    private void stopTone(){ mainHandler.post(()->{
        if(tonePlayer!=null){ tonePlayer.stop(); tonePlayer=null; }
    });}

    @Override public void onDestroy(){
        stopTone(); instance=null; super.onDestroy();
    }

    private static final class TonePlayer {
        private AudioTrack track; private Thread thread; private volatile boolean running;
        void start(){
            final int sr=44100, frames=sr/2; final short[] pcm=new short[frames];
            for(int i=0;i<frames;i++){
                double t=i/(double)sr;
                double env=Math.min(1.0,i/500.0)*Math.min(1.0,(frames-i)/1000.0);
                double f=(t<0.32||(t>0.36&&t<0.50))?880.0:0.0;
                pcm[i]=(short)(Math.sin(2*Math.PI*f*t)*12000*env);
            }
            AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            AudioFormat fmt=new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
            int min=AudioTrack.getMinBufferSize(sr,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            track=new AudioTrack(attrs,fmt,Math.max(min,pcm.length*2),AudioTrack.MODE_STREAM,AudioManager.AUDIO_SESSION_ID_GENERATE);
            running=true;
            thread=new Thread(()->{try{track.play();while(running)track.write(pcm,0,pcm.length);}catch(Exception ignored){}}, "IncomingTonePlayer");
            thread.start();
        }
        void stop(){
            running=false;
            try{if(thread!=null)thread.join(300);}catch(InterruptedException e){Thread.currentThread().interrupt();}
            if(track!=null){try{track.stop();}catch(Exception ignored){} track.release();track=null;}
        }
    }
}
