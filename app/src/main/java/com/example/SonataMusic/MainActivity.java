package com.example.SonataMusic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.content.Context;
import java.io.IOException;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.text.TextUtils;
import android.os.Looper;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import java.util.List;
import java.util.ArrayList;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ImageButton;
import android.webkit.JavascriptInterface;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private TextView logTextView;
    private StringBuilder logBuilder = new StringBuilder();
    private int currentStreamType = AudioManager.STREAM_MUSIC;
    private WebView webView;
    private Spinner channelSpinner;
    private EditText customUrlEditText;
    private Button goButton;
    private ImageButton refreshButton;
    private Button playPauseButton;
    private Button btnVolumeDown;
    private Button btnVolumeUp;

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_NOTIFICATION_POLICY
    };

    private String[] predefinedUrls = {
        "https://tools.liumingye.cn/music",
        "https://music.91q.com/",
        "https://zz123.com/",
        "https://netease-music.fe-mm.com/",
        "http://www.htqyy.com",
        "https://www.vvvdj.com/",
        "http://music.163.com",
        "https://y.qq.com",
        "http://www.kugou.com",
        "https://www.qtfm.cn/",
        "https://www.bilibili.com/",
        "https://www.baidu.com"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化 WebView
        webView = findViewById(R.id.webView);
        setupWebView();

        // 初始化 AudioManager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initAudioFocusRequest();

        // 初始化其他 UI 组件
        logTextView = findViewById(R.id.logTextView);
        playPauseButton = findViewById(R.id.playPauseButton);
        btnVolumeDown = findViewById(R.id.btnVolumeDown);
        btnVolumeUp = findViewById(R.id.btnVolumeUp);
        channelSpinner = findViewById(R.id.channelSpinner);
        customUrlEditText = findViewById(R.id.customUrlEditText);
        goButton = findViewById(R.id.goButton);
        refreshButton = findViewById(R.id.refreshButton);

        // 设置声音信道的 Spinner
        ArrayAdapter<CharSequence> channelAdapter = ArrayAdapter.createFromResource(this,
                R.array.channel_options, android.R.layout.simple_spinner_item);
        channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelSpinner.setAdapter(channelAdapter);

        // 设置 URL Spinner
        ArrayAdapter<String> urlAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, predefinedUrls);
        urlAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner urlSpinner = findViewById(R.id.urlSpinner); // 假设您有一个名为 urlSpinner 的 Spinner
        urlSpinner.setAdapter(urlAdapter);

        // 设置 URL Spinner 的选择监听器
        urlSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedUrl = parent.getItemAtPosition(position).toString();
                loadUrl(selectedUrl); // 加载所选的 URL
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        channelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentStreamType = getStreamTypeFromSelection(parent.getItemAtPosition(position).toString());
                setAudioStream(currentStreamType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 设置 GO 按钮
        goButton.setOnClickListener(v -> {
            String customUrl = customUrlEditText.getText().toString();
            if (!customUrl.isEmpty()) {
                loadUrl(customUrl);
            }
        });

        // 设置刷新按钮
        refreshButton.setOnClickListener(v -> webView.reload());

        // 设置按钮点击监听器
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        btnVolumeDown.setOnClickListener(v -> adjustVolume(-1));
        btnVolumeUp.setOnClickListener(v -> adjustVolume(1));

        // 初始化 MediaPlayer
        initMediaPlayer();

        // 检查权限
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        addLog("正在检查权限...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissionsNeeded = new ArrayList<>();
            
            for (String permission : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(permission);
                }
            }
            
            if (!permissionsNeeded.isEmpty()) {
                addLog("请求以下权限: " + TextUtils.join(", ", permissionsNeeded));
                ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            } else {
                addLog("所有必要的权限已获得");
            }
        }
        addLog("权限检查完成");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    addLog("权限 " + permissions[i] + " 已获得");
                } else {
                    addLog("权限 " + permissions[i] + " 被拒绝");
                    Toast.makeText(this, "需要所有权限才能正常运行应用", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void initAudioFocusRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build();
        }
    }

    private AudioManager.OnAudioFocusChangeListener focusChangeListener =
        focusChange -> {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mediaPlayer == null) initMediaPlayer();
                    else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) 
                        mediaPlayer.setVolume(0.1f, 0.1f);
                    break;
            }
        };

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                addLog("音频已暂停");
            } else {
                mediaPlayer.start();
                addLog("音频开始播放");
            }
        } else {
            addLog("MediaPlayer 未初始化");
            Toast.makeText(this, "MediaPlayer 未初始化", Toast.LENGTH_SHORT).show();
        }
    }

    private void adjustVolume(int direction) {
        audioManager.adjustStreamVolume(
            currentStreamType,
            direction > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        );
    }

    private void setAudioStream(int streamType) {
        addLog("设置音频流类型: " + getStreamTypeName(streamType));
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(getUsageFromStreamType(streamType))
                .build();
            mediaPlayer.setAudioAttributes(audioAttributes);
            try {
                mediaPlayer.setDataSource(getResources().openRawResourceFd(R.raw.music));
                mediaPlayer.prepare();
                addLog("音频流类型设置成功");
            } catch (IOException e) {
                e.printStackTrace();
                addLog("设置音频流类型时出错: " + e.getMessage());
            }
        }
    }

    private int getStreamTypeFromSelection(String selectedChannel) {
        switch (selectedChannel) {
            case "STREAM_MUSIC": return AudioManager.STREAM_MUSIC;
            case "STREAM_SYSTEM": return AudioManager.STREAM_SYSTEM;
            case "STREAM_VOICE_CALL": return AudioManager.STREAM_VOICE_CALL;
            case "STREAM_RING": return AudioManager.STREAM_RING;
            case "STREAM_ALARM": return AudioManager.STREAM_ALARM;
            case "STREAM_NOTIFICATION": return AudioManager.STREAM_NOTIFICATION;
            default: return AudioManager.STREAM_MUSIC;
        }
    }

    private int getUsageFromStreamType(int streamType) {
        switch (streamType) {
            case AudioManager.STREAM_MUSIC: return AudioAttributes.USAGE_MEDIA;
            case AudioManager.STREAM_SYSTEM: return AudioAttributes.USAGE_ASSISTANCE_SONIFICATION;
            case AudioManager.STREAM_VOICE_CALL: return AudioAttributes.USAGE_VOICE_COMMUNICATION;
            case AudioManager.STREAM_RING: return AudioAttributes.USAGE_NOTIFICATION_RINGTONE;
            case AudioManager.STREAM_ALARM: return AudioAttributes.USAGE_ALARM;
            case AudioManager.STREAM_NOTIFICATION: return AudioAttributes.USAGE_NOTIFICATION;
            default: return AudioAttributes.USAGE_MEDIA;
        }
    }

    private String getStreamTypeName(int streamType) {
        switch (streamType) {
            case AudioManager.STREAM_MUSIC: return "STREAM_MUSIC";
            case AudioManager.STREAM_SYSTEM: return "STREAM_SYSTEM";
            case AudioManager.STREAM_VOICE_CALL: return "STREAM_VOICE_CALL";
            case AudioManager.STREAM_RING: return "STREAM_RING";
            case AudioManager.STREAM_ALARM: return "STREAM_ALARM";
            case AudioManager.STREAM_NOTIFICATION: return "STREAM_NOTIFICATION";
            default: return "UNKNOWN_STREAM_TYPE";
        }
    }

    private void addLog(String message) {
        logBuilder.append(message).append("\n");
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateLogTextView();
        } else {
            runOnUiThread(this::updateLogTextView);
        }
    }

    private void updateLogTextView() {
        if (logTextView != null) {
            logTextView.setText(logBuilder.toString());
            logTextView.post(() -> {
                if (logTextView.getLayout() != null) {
                    final int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
                    if (scrollAmount > 0)
                        logTextView.scrollTo(0, scrollAmount);
                    else
                        logTextView.scrollTo(0, 0);
                }
            });
        }
    }

    private void initMediaPlayer() {
        addLog("初始化MediaPlayer...");
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            );
            
            // 使用 raw 资源文件夹中的音频文件
            int resourceId = getResources().getIdentifier("music", "raw", getPackageName());
            if (resourceId != 0) {
                mediaPlayer.setDataSource(getResources().openRawResourceFd(resourceId));
                mediaPlayer.prepare();
                addLog("音频文件加载成功");
            } else {
                addLog("音频文件未找到");
                Toast.makeText(this, "音频文件未找到", Toast.LENGTH_SHORT).show();
            }
            
            mediaPlayer.setLooping(true);
        } catch (Exception e) {
            e.printStackTrace();
            addLog("初始化MediaPlayer失败: " + e.getMessage());
            Toast.makeText(this, "初始化MediaPlayer失败，请检查日志", Toast.LENGTH_LONG).show();
        }
    }

    private void loadUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        webView.loadUrl(url);
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");

        // 添加 JavaScript 接口
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // 注入 JavaScript 代码
                String js = "javascript:(function() {" +
                    "var audioElements = document.getElementsByTagName('audio');" +
                    "var videoElements = document.getElementsByTagName('video');" +
                    "function addListeners(elements) {" +
                    "  for(var i = 0; i < elements.length; i++) {" +
                    "    elements[i].addEventListener('play', function() {" +
                    "      Android.onMediaPlay(this.src);" + // 传递当前播放的音频 URL
                    "    });" +
                    "  }" +
                    "}" +
                    "addListeners(audioElements);" +
                    "addListeners(videoElements);" +
                "})()";
                view.loadUrl(js);
            }
        });

        webView.loadUrl("https://www.baidu.com");
    }

    // JavaScript 接口
    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void onMediaPlay(String mediaUrl) {
            runOnUiThread(() -> {
                setAudioStream(currentStreamType);
                playAudioFromUrl(mediaUrl); // 播放当前音频或视频
                addLog("播放音频/视频: " + mediaUrl);
            });
        }
    }

    private void playAudioFromUrl(String url) {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            try {
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepare();
                mediaPlayer.start();
                addLog("开始播放音频/视频: " + url);
            } catch (IOException e) {
                e.printStackTrace();
                addLog("播放音频/视频时出错: " + e.getMessage());
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop(); // 确保在释放之前停止播放
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        } else {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
        addLog("应用已销毁，资源已释放");
    }
}