# sonata-music

索纳塔10车机破解后安装第三方音视频软件无声音，所以该软件仅用来测试索纳塔10破解后的声道信息。
索纳塔10第三方音乐软件可使用二次封装洛雪音乐的lx-sonata(https://github.com/shubihu/lx-sonata)

- 实现原理：默认音视频软件的声音通道都是通过STREAM_MUSIC声道播放媒体声音，但是索纳塔10第三方软件无法通过该声道播放，经测试STREAM_SYSTEM、STREAM_VOICE_CALL、STREAM_ALARM声道可以用来播放第三方媒体的声音。

- 最终版：强制把STREAM_MUSIC声道设置为导航用途，即USAGE_ASSISTANCE_NAVIGATION_GUIDANCE，这样即解决了声音播放没有声音的问题，同时也解决了音量无法调整的问题。
- 音量调整需要通过方向盘上的音量调节按钮来调整。软件自带的音量调节按钮无法进行调节。
