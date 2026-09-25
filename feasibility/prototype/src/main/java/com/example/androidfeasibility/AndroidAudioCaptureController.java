package com.example.androidfeasibility;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.util.concurrent.atomic.AtomicBoolean;

/** Permission-gated microphone capture with deterministic release on stop/release. */
public final class AndroidAudioCaptureController implements AudioCaptureController {
    private final Context context;
    private final int sampleRate;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile AudioRecord recorder;
    private volatile Thread worker;

    public AndroidAudioCaptureController(Context context) { this(context, 16_000); }

    public AndroidAudioCaptureController(Context context, int sampleRate) {
        if (context == null) throw new IllegalArgumentException("context is null");
        this.context = context.getApplicationContext();
        this.sampleRate = sampleRate;
    }

    @Override public synchronized void start(final Listener listener) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            listener.onError("microphone permission denied");
            return;
        }
        if (running.get()) throw new IllegalStateException("capture already active");
        int min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) { listener.onError("microphone buffer unavailable"); return; }
        AudioRecord current = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, sampleRate));
        if (current.getState() != AudioRecord.STATE_INITIALIZED) {
            current.release();
            listener.onError("microphone initialization failed");
            return;
        }
        recorder = current;
        running.set(true);
        current.startRecording();
        worker = new Thread(new Runnable() {
            @Override public void run() {
                short[] buffer = new short[Math.max(512, sampleRate / 10)];
                try {
                    while (running.get()) {
                        int count = recorder.read(buffer, 0, buffer.length);
                        if (count > 0) {
                            byte[] bytes = new byte[count * 2];
                            for (int i = 0; i < count; i++) {
                                bytes[i * 2] = (byte) (buffer[i] & 0xff);
                                bytes[i * 2 + 1] = (byte) ((buffer[i] >> 8) & 0xff);
                            }
                            listener.onAudio(bytes);
                        } else if (count < 0) {
                            listener.onError("microphone read failed");
                            break;
                        }
                    }
                } finally {
                    stop();
                }
            }
        }, "mochi-audio-capture");
        worker.setDaemon(true);
        worker.start();
    }

    @Override public synchronized void stop() {
        running.set(false);
        AudioRecord current = recorder;
        if (current != null) {
            try { current.stop(); } catch (IllegalStateException ignored) {}
            current.release();
            recorder = null;
        }
        worker = null;
    }

    @Override public synchronized void release() { stop(); }
}
