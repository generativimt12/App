package com.generativimt12.incomingtone;

import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.telecom.*;
import java.util.*;

public class IncomingCallService extends InCallService {
    private static IncomingCallService instance;
    private TonePlayer tonePlayer;
    private final Handler mainHandler=new Handler(Looper.getMainLooper());
    private final Set<Call> ringingCalls=new HashSet<>();

    public static IncomingCallService getInstance(){return instance;}
    public static Call getCurrentCall(){return instance==null?null:instance.findActiveCall();}
    @Override public void onCreate(){super.onCreate();instance=this;}

    @Override public void onCallAdded(Call call){
        super.onCallAdded(call);
        call.registerCallback(new Call.Callback(){
            @Override public void onStateChanged(Call c,int state){
                if(state==Call.STATE_RINGING){ringingCalls.add(c);silenceSystemRinger();startTone();showCallUi();}
                else if(state==Call.STATE_ACTIVE){ringingCalls.remove(c);stopTone();showCallUi();}
                else if(state==Call.STATE_DISCONNECTED){ringingCalls.remove(c);stopTone();if(findActiveCall()==null)InCallActivity.finishIfOpen();}
                else if(state==Call.STATE_DIALING||state==Call.STATE_CONNECTING){stopTone();showCallUi();}
            }
        },mainHandler);
        if(call.getState()==Call.STATE_RINGING){ringingCalls.add(call);silenceSystemRinger();startTone();}
        showCallUi();
    }

    @Override public void onCallRemoved(Call call){ringingCalls.remove(call);stopTone();if(findActiveCall()==null)InCallActivity.finishIfOpen();super.onCallRemoved(call);}
    @Override public void onBringToForeground(boolean showDialpad){showCallUi();}
    @Override public void onSilenceRinger(){silenceSystemRinger(); /* keep independent ringtone alive */}

    private Call findActiveCall(){for(Call c:getCalls()){int s=c.getState();if(s!=Call.STATE_DISCONNECTED&&s!=Call.STATE_DISCONNECTING)return c;}return null;}
    private void showCallUi(){mainHandler.post(()->{Call c=findActiveCall();if(c!=null){Intent i=new Intent(this,InCallActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(i);}});}

    private void silenceSystemRinger(){
        try{
            TelecomManager tm=getSystemService(TelecomManager.class);
            if(tm!=null)tm.silenceRinger();
        }catch(Exception ignored){}
    }

    private void startTone(){
        if(!SettingsStore.toneEnabled(this))return;
        if(tonePlayer==null){
            tonePlayer=new TonePlayer(this);
            try{tonePlayer.start();}catch(Exception e){tonePlayer=null;}
        }
    }
    private void stopTone(){mainHandler.post(()->{if(tonePlayer!=null){tonePlayer.stop();tonePlayer=null;}});}

    public static void answerIncoming(){if(instance!=null){Call c=instance.findRingingCall();if(c!=null)c.answer(VideoProfile.STATE_AUDIO_ONLY);}}
    public static void openRecentCalls(Context context){Intent i=new Intent(context,MainActivity.class);i.putExtra("open_recent",true);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);context.startActivity(i);}
    public static void toggleMute(){if(instance!=null)try{instance.setMuted(!instance.isMutedNow());}catch(Exception ignored){}}
    public static void cycleAudioRoute(){if(instance!=null)instance.cycleAudioRouteInternal();}
    public void cycleAudioRouteInternal(){
        try{
            if(android.os.Build.VERSION.SDK_INT>=34){
                java.util.List<CallEndpoint> eps=getAvailableCallEndpoints();
                if(eps==null||eps.isEmpty())return;
                CallEndpoint cur=getCurrentCallEndpoint(); int pos=-1;
                for(int i=0;i<eps.size();i++)if(cur!=null&&eps.get(i).getEndpointType()==cur.getEndpointType()){pos=i;break;}
                for(int step=1;step<=eps.size();step++){
                    CallEndpoint e=eps.get((pos+step+eps.size())%eps.size());
                    int t=e.getEndpointType();
                    if(t==CallEndpoint.TYPE_EARPIECE||t==CallEndpoint.TYPE_SPEAKER||t==CallEndpoint.TYPE_BLUETOOTH){
                        requestCallEndpointChange(e,getMainExecutor(),new android.os.OutcomeReceiver<Void,CallEndpointException>(){
                            public void onResult(Void v){}
                            public void onError(CallEndpointException e){}
                        });
                        return;
                    }
                }
            }else{
                CallAudioState st=getCallAudioState(); if(st==null)return;
                if((st.getSupportedRouteMask()&CallAudioState.ROUTE_BLUETOOTH)!=0){
                    java.util.Set<android.bluetooth.BluetoothDevice> bt=st.getSupportedBluetoothDevices();
                    if(bt!=null&&!bt.isEmpty()){requestBluetoothAudio(bt.iterator().next());return;}
                }
                setAudioRoute(st.getRoute()==CallAudioState.ROUTE_SPEAKER?CallAudioState.ROUTE_EARPIECE:CallAudioState.ROUTE_SPEAKER);
            }
        }catch(Exception ignored){}
    }
    public boolean isMutedNow(){try{return getCallAudioState()!=null&&getCallAudioState().isMuted();}catch(Exception e){return false;}}
    public void setSpeakerNow(boolean on){try{setAudioRoute(on?CallAudioState.ROUTE_SPEAKER:CallAudioState.ROUTE_EARPIECE);}catch(Exception ignored){}}
    public boolean isSpeakerNow(){try{return getCallAudioState()!=null&&getCallAudioState().getRoute()==CallAudioState.ROUTE_SPEAKER;}catch(Exception e){return false;}}

    private Call findRingingCall(){for(Call c:getCalls())if(c.getState()==Call.STATE_RINGING)return c;return null;}
    @Override public void onDestroy(){stopTone();instance=null;super.onDestroy();}

    private static final class TonePlayer {
        private final Context context; private MediaPlayer media; private AudioTrack track; private Thread thread; private volatile boolean running; private Vibrator vibrator;
        TonePlayer(Context c){context=c.getApplicationContext();}

        void start(){
            String uri=SettingsStore.toneUri(context);
            if(uri!=null&&!uri.isEmpty()){startFile(Uri.parse(uri));return;}
            startGenerated();
        }

        private void startFile(Uri uri){
            try{
                media=new MediaPlayer();
                media.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                media.setDataSource(context,uri);
                media.setLooping(true);
                media.setVolume(SettingsStore.volume(context)/100f,SettingsStore.volume(context)/100f);
                media.setOnPreparedListener(mp->{if(running){mp.start();}});
                running=true; media.prepareAsync(); startVibrate();
            }catch(Exception e){if(media!=null){try{media.release();}catch(Exception ignored){}}media=null;startGenerated();}
        }

        private void startGenerated(){
            final int sr=44100,frames=sr;final short[] pcm=new short[frames];int tone=SettingsStore.tone(context);
            double[] f1={880,660,988,523},f2={660,880,784,659};
            for(int i=0;i<frames;i++){double t=i/(double)sr,on;if(tone==1)on=((t%0.75)<0.24?1:0)+((t%0.75)>0.31&&(t%0.75)<0.55?0.75:0);else if(tone==2)on=(t%1.0<0.42?1:0);else if(tone==3)on=(t%1.15<0.58?1:0);else on=((t%0.85)<0.25?1:0)+((t%0.85)>0.31&&(t%0.85)<0.56?0.72:0);double freq=(tone==0&&t%0.85>0.31)?f2[tone]:f1[tone];double env=Math.min(1.0,i/900.0)*Math.min(1.0,(frames-i)/2500.0);pcm[i]=(short)(Math.sin(2*Math.PI*freq*t)*12000*env*Math.min(1.0,on));}
            AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            AudioFormat fmt=new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
            int min=AudioTrack.getMinBufferSize(sr,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            track=new AudioTrack(attrs,fmt,Math.max(min,pcm.length*2),AudioTrack.MODE_STREAM,AudioManager.AUDIO_SESSION_ID_GENERATE);track.setVolume(SettingsStore.volume(context)/100f);running=true;startVibrate();
            thread=new Thread(()->{try{track.play();while(running)track.write(pcm,0,pcm.length);}catch(Exception ignored){}},"IncomingTonePlayer");thread.start();
        }

        private void startVibrate(){if(SettingsStore.vibrate(context)){vibrator=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);if(vibrator!=null&&vibrator.hasVibrator())vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0,350,250,350,1000},0));}}
        void stop(){running=false;if(vibrator!=null)try{vibrator.cancel();}catch(Exception ignored){}try{if(thread!=null)thread.join(300);}catch(InterruptedException e){Thread.currentThread().interrupt();}if(track!=null){try{track.stop();}catch(Exception ignored){}track.release();track=null;}if(media!=null){try{media.stop();}catch(Exception ignored){}media.release();media=null;}}
    }
}
