package com.acgist.taoyao.media;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;
import java.util.UUID;

/**
 * 声音播报
 *
 * @author acgist
 */
public final class Broadcaster {

    private static final Broadcaster INSTANCE = new Broadcaster();

    public static final Broadcaster getInstance() {
        return INSTANCE;
    }

    private TextToSpeech textToSpeech;

    private Broadcaster() {
    }

    public void init(Context context) {
        this.textToSpeech = new TextToSpeech(context, new TextToSpeechInitListener());
    }

    public void broadcast(String text) {
//      this.textToSpeech.stop();
        this.textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
    }

    public void shutdown() {
        this.textToSpeech.shutdown();
    }

    private class TextToSpeechInitListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int status) {
            Log.i(Broadcaster.class.getSimpleName(), "加载TTS：" + status);
            if(status == TextToSpeech.SUCCESS) {
                Broadcaster.this.textToSpeech.setLanguage(Locale.CANADA);
                Broadcaster.this.textToSpeech.setPitch(1.0F);
                Broadcaster.this.textToSpeech.setSpeechRate(1.0F);
            }
        }
    }

}
