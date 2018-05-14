# LanSoEditor_common  ---android平台的视频编辑SDK

### 我们有基于【容器和图层架构】的专业版本
 https://github.com/LanSoSdk/LanSoEditor_advance
 欢迎您的使用,专业版本采用类似photoshop一样的图层架构， 各种视频， 图片，文字， UI等都被处理成一个个的图层 增加到DrawPad（容器）中。可以实现滤镜, 叠加,混合,标记,涂鸦,贴纸等各种视频编辑效果.

### 当前版本是LanSoEditor-2.9.0
*   优化视频处理: 默认先硬件编码,如硬件编码遇到问题, 自动切换为软件编码;可指定编码器;
*   增加VideoLayout类, 多个视频合成一个的效果;
### 功能介绍
*  主要使用在音视频的: 裁剪,剪切,分离,合并,转换,拼接,水印,叠加,混合,转码等场合;
*  
*  我们是针对android平台对ffmpeg做了硬件加速优化,经过多款手机的测试,优化性能大概提升4倍左右
*  
*  我们在项目中提供了大约40个常用的方法并写了详细的说明注释,基本满足一般视频编辑的需求
*  
*  我们另外提供了扩展接口,您完全可以根据强大的ffmpeg命令来扩展您需要的功能
*  
*  【新】增加视频分段录制的功能， 支持在录制过程中回删操作，并开放Java端源代码，您可以任意的更改内部参数，以符合您的录制需求。
*  
*  【新】提供音视频的编解码器， 针对一些特殊的使用场合使用， 您可以根据实际需求来自己编码或解码。
*  
*  此SDK采用低价收费授权,公司性质的合作,为了您项目更好的进行,欢迎和我们联系.谢谢!

### 我们的专业版本下载地址：
*	https://github.com/LanSoSdk/LanSoEditor_advance

### 举例功能的界面展示
![](https://github.com/LanSoSdk/LanSoEditor_common/blob/master/editor_common_main.jpg)

### 下载地址: 
*  https://github.com/LanSoSdk/LanSoEditor_common

### 联系方式:
*   QQ 1852600324 
*   Email:support@lansongtech.com
*   Phone:0571-89052701
*   网址:www.lansongtech.com

### 本SDK的编解码有:
*  软解码器H264
*  硬件加速解码器lansoh264_dec
*  软编码器libx264
*  硬件加速编码器lansoh264_enc
*  音频mp3解码器mp3
*  音频mp3编码器libmp3lame
*  音频aac解码器aac
*  音频aac编码器 faac
*  音频pcm编解码器 pcm_s16le
*  编解码器 gif

## 使用案例
*   我们从事的是：商业SDK开发、更新和维护；
*   当前包括500强 大公司在内的大约80多个上线APP在使用，行业涉及 社交、微商、广场舞、直播、工具、母婴、舞蹈、厨艺、金融、炫酷等多种行业
*   欢迎联系我们，索取相关案例信息和授权说明
