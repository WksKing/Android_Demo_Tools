package com.wistron.demo.tool.snowboy_demo;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import ai.kitt.snowboy.SnowboyDetect;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "King";

    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";

    public static final int RECORDER_BPP = 16;
    public static int RECORDER_SAMPLERATE = 16000;
    public static int RECORDER_CHANNELS = 1;
    public static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private boolean isExit = false;

    private SnowboyDetect snowboyDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        // Assume you put the model related files under /sdcard/snowboy/
        snowboyDetector = new SnowboyDetect("/storage/emulated/legacy/common.res",
                /*"/storage/emulated/legacy/snowboy.umdl");*/
                "/storage/emulated/legacy/hello_teddy.pmdl");
        snowboyDetector.SetSensitivity("0.34");         // Sensitivity for each hotword
        snowboyDetector.SetAudioGain(1.0f);              // Audio gain for detection
        Log.i(TAG, "NumHotwords = "+snowboyDetector.NumHotwords()+", BitsPerSample = "+snowboyDetector.BitsPerSample()+", NumChannels = "+snowboyDetector.NumChannels()+", SampleRate = "+snowboyDetector.SampleRate());

        initial(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }

    private void initial(int sampleRate, int channels, int audioEncoding) {
        bufferSize = AudioRecord.getMinBufferSize
                (sampleRate, channels, audioEncoding) * 3;
        bufferSize = snowboyDetector.NumChannels() * snowboyDetector.SampleRate() * 5;
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate,
                channels,
                audioEncoding,
                bufferSize);

        startRecord();
    }

    public void startRecord() {
        if (isRecording){
            return;
        }

        int i = recorder.getState();
        if (i == AudioRecord.STATE_INITIALIZED) {
            recorder.startRecording();
        }

        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        /*FileOutputStream os = null;
        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

        short data[] = new short[bufferSize/2];

        int read = 0;
        try {
            while (isRecording) {
                read = recorder.read(data, 0, data.length);
                Log.i(TAG, "read length = " + read);
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    /*os.write(data, 0, read);
                    os.flush();*/
                    int result = snowboyDetector.RunDetection(data, data.length);
                    Log.i(TAG, " ----> result = "+result);
                }
                Thread.sleep(30);
            }
        }catch (InterruptedException e) {
            e.printStackTrace();
        }/* finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }
        Log.i(TAG, "detectSpeaking finished.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRecording = false;
        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }
    }
}
