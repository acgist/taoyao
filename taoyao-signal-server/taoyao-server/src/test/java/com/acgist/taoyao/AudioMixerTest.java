package com.acgist.taoyao;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class AudioMixerTest {

    @Test
    public void testMixer() throws IOException {
//      ffmpeg -i audio.mp3 -f s32le audio.pcm
//      ffplay -i audio.pcm -f s32le -ar 48000 -ac 1
        final File fileA = new File("D:\\tmp\\mixer\\1.pcm");
        final File fileB = new File("D:\\tmp\\mixer\\2.pcm");
        final byte[] bytesA = Files.readAllBytes(fileA.toPath());
        final byte[] bytesB = Files.readAllBytes(fileB.toPath());
        final int length = Math.min(bytesA.length, bytesB.length);
        final byte[] target = new byte[length];
        int a, b;
        for (int i = 0; i < length; i++) {
            a = bytesA[i];
            b = bytesB[i];
            target[i] = (byte) ((a + b) / 2);
        }
        Files.write(Paths.get("D:\\tmp\\mixer\\3.pcm"), target);
    }
    
}
