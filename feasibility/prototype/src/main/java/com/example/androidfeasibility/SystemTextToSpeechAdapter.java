package com.example.androidfeasibility;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Android system TTS implementation behind the provider-neutral adapter. */
public final class SystemTextToSpeechAdapter implements TextToSpeechAdapter {
    private final TextToSpeech textToSpeech;
    private final Map<String, Listener> listeners = new ConcurrentHashMap<>();
    private volatile boolean ready;

    public SystemTextToSpeechAdapter(Context context) {
        textToSpeech = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override public void onInit(int status) { ready = status == TextToSpeech.SUCCESS; }
        });
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {
                Listener listener = listeners.get(utteranceId);
                if (listener != null) listener.onStarted();
            }

            @Override public void onDone(String utteranceId) {
                Listener listener = listeners.remove(utteranceId);
                if (listener != null) listener.onCompleted();
            }

            @Override public void onError(String utteranceId) {
                Listener listener = listeners.remove(utteranceId);
                if (listener != null) listener.onError("Android TTS failed");
            }
        });
    }

    @Override public Playback speak(String text, Listener listener) {
        if (!ready) {
            listener.onError("Android TTS is not ready");
            return new Playback() { @Override public void stop() {} };
        }
        final String utteranceId = UUID.randomUUID().toString();
        listeners.put(utteranceId, listener);
        int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        if (result == TextToSpeech.ERROR) {
            listeners.remove(utteranceId);
            listener.onError("Android TTS rejected text");
        }
        return new Playback() {
            @Override public void stop() {
                listeners.remove(utteranceId);
                textToSpeech.stop();
            }
        };
    }

    public void shutdown() {
        listeners.clear();
        textToSpeech.stop();
        textToSpeech.shutdown();
    }
}
