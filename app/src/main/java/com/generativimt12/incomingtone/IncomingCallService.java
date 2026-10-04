package com.generativimt12.incomingtone;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.InCallService;

public class IncomingCallService extends InCallService {
    private TonePlayer tonePlayer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        call.registerCallback(new Call.Callback() {
            @Override public void onStateChanged(Call c, int state) {
                if (state == Call.STATE_RINGING) {
                    startTone();
                } else if (state == Call.STATE_ACTIVE || state == Call.STATE_DISCONNECTED || state == Call.STATE_DIALING) {
                    stopTone();
                }
            }
        }, mainHandler);
        if (call.getState() == Call.STATE_RINGING) startTone();
    }

    @Override
    public void onCallRemoved(Call call) {
        stopTone();
        super.onCallRemoved(call);
    }

    private void startTone() {
        mainHandler.post(() -> {
            if (tonePlayer == null) {
                tonePlayer = new TonePlayer();
                tonePlayer.start();
            }
        });
    }

    private void stopTone() {
        mainHandler.post(() -> {
            if (tonePlayer != null) {
                tonePlayer.stop();
                tonePlayer = null;
            }
        });
    }

    private static final class TonePlayer {
        private AudioTrack track;
        private Thread thread;
        private volatile boolean running;

        void start() {
            final int sampleRate = 44100;
            final int frameCount = sampleRate / 2;
            final short[] pcm = new short[frameCount];
            for (int i = 0; i < frameCount; i++) {
                double t = i / (double) sampleRate;
                double envelope = Math.min(1.0, i / 500.0)
                        * Math.min(1.0, (frameCount - i) / 1000.0);
                double f = (t < 0.32 || (t > 0.36 && t < 0.50)) ? 880.0 : 0.0;
                pcm[i] = (short)(Math.sin(2.0 * Math.PI * f * t) * 12000.0 * envelope);
            }

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build();

            int min = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            track = new AudioTrack(attrs, format, Math.max(min, pcm.length * 2),
                    AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            running = true;
            thread = new Thread(() -> {
                track.play();
                while (running) {
                    track.write(pcm, 0, pcm.length);
                }
            }, "IncomingTonePlayer");
            thread.start();
        }

        void stop() {
            running = false;
            try {
                if (thread != null) thread.join(300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            if (track != null) {
                try { track.stop(); } catch (Exception ignored) {}
                track.release();
                track = null;
            }
        }
    }
}
