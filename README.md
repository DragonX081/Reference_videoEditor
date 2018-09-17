## 可能是android系统中对ffmpeg封装最好的免费SDK; 

### 主要特点:
  -  增加 H264硬件编码器 和 H264硬件解码器,  处理速度极大提升;
  -  增加40多个常见方法,编写各种辅助处理类.
  -  SDK已运行二年左右,商用APP200+.
  -  音频处理为收费定制功能,请注意.
  
### 免费策略:
-  此SDK已运行二年左右,商用APP200+, 现在免费.
-  SDK不访问网络,完全本地处理.
-  个人和公司都可以商用,不需要和我们说明.
-  随我们专业版定期升级,维护.
-  遇到问题在issue中解答;
-  我们提供有偿技术支持和定制服务,详情见下面.

### 使用:
```
-  1, 一条命令即可完成,阻塞执行:  
	       VideoEditor editor=VideoEditor();
   举例1: 视频增加水印:
              "处理后的视频"=editor.executeOverLayVideoFrame("视频路径","增加的图片路径","x坐标","y坐标");
   举例2: 裁剪视频时长:
               dstResult=editor.executeCutVideo("视频路径","开始时间S","结束时间S");
   举例3: 画面裁剪:
               dstResult=editor.executeCropVideoFrame("视频路径","x坐标","y坐标","裁剪宽度","裁剪高度");
               
-  2, 我们封装好的功能有
		 替换背景音乐, 声音混合, 多段视频的拼接, 不同视频源的拼接, 视频画面拼接, 读取视频帧, 获取所有帧, 图片转视频, 
               倒序,加减速,镜像,音频混合,音频转码,画面填充,缩放,压缩,镜像,增加文字,视频转码,图片视频转GIF等;
               
-  3, 如果列举的功能不能满足您的需求, 您可以根据ffmpeg的命令自行扩展,代码中有说明;  
```
### 辅助代码:    
```
    1, VideoEditor其他公共方法:
    	   1. setonProgressListener();  //executeXXX 正在执行的百分比进度;
    	   2 setEncoderBitrate();  //给executeXXX 指定码率;
    	   3 VideoEditor.isForceSoftWareEncoder=true; //强制executeXXX 在编码时,采用软编码器;
    	   4 cancel(); //取消正在执行的executeXXX;
    	   5,当返回null, 我们提供了log采集, 可通过getErrorLog()得到错误信息;大部分是因为视频参数错误引起的.
    	   
    2, 写了MediaInfo辅助类, 用来很快的获取视频的基本信息,以方便你实际参数的参考, 使用如下:
            MediaInfo info=new MediaInfo("要获取视频的路径");
             if(info.prepare()){
               	; 如果返回true,得到视频宽度,高度,码率,帧率,时长,编码器,总帧数,是否有B帧,旋转角度, 音频采样率,音频通道数, 音频码率,是否有音频,视频,视频是否旋转 等参数;
             }
	     也可以先用MediaInfo判断当前视频的各种信息;
    3, 文件创建,删除类 LanSongFileUtil.java 方便你在编辑时的各种文件创建,删除, 判断等操作.	     
``` 
### 常见问题解决：
```
   方法1： 80%的问题是因为文件传输错误，可用MediaInfo.checkFile("您的文件绝对路径")； 来查看打印信息， 根据打印信息的提示，排除问题。
   方法2： 查看打印信息, 如返回 xxx pixel -1之类, 说明找不到对应的节码器; 如其他信息, 查看对应的关键字, 大部分是因为命令错误,或没有覆盖之前的文件等等;
   
```
### 集成步骤
-  直接导入lansongsdk 这个module后; 在代码开始的时候,增加SDK初始化代码:
```
	LanSoEditor.initSDK(getApplicationContext())；
	在您工程的settting.gradle中增加: ,'LanSongSDK' ; 并在app.gradle中 增加: implementation project(':LanSongSDK')
	
```
我们提供了3种指令集:armeabi/armeabi-v7a/arm64-v8a,以方便不同的需求.但大部分是需要一个,我们推荐保留armeabi-v7a,在您导入项目后删除另外两个;


# 增值服务:
```
  -  我们提供有偿技术支持, 费用12000元. 包括:
	  1, 提供 定制APK的开放功能代码.
	  2, 提供为期一年的 技术支持和功能定制(一年内可定制各种功能.) 
	  3, 定制的功能永久使用,我们负责维护, 速度优化. 
	  4, 规划需求时的视频技术咨询,可以在规划需求的时候,就向我们咨询,或许您感觉很复杂的效果,在我们看来,只需几行代码就可以实现,可以让您少走很多弯路;
	  
  -  提供的定制APK安装包,可以在当前SDK下载的文件夹中找到:
  -   包含:
      -  美颜录制. 类似微信的录制界面,包括聚焦,亮度调节,按下录制,拍摄图片.5级美颜.
      -  编辑功能: 涂鸦, 增加文字, 增加图片,变速,时长裁剪,画面裁剪.
      -  定制APK的功能,会一直更新, 速度和画质会一直优化,并采用GPU来渲染.欢迎你的使用.
```
   -  定制APK的界面如下:
![](https://github.com/LanSoSdk/LanSoEditor_common/blob/master/customAPK.png)
	
	

### 我们有专业版SDK,图层架构,可以做各种视频特效, 并支持AE模板,微商小视频,抖音趣拍等效果: 
     [android 专业版] (https://github.com/LanSoSdk/LanSoEditor_advance)
     [ios 专业版] (https://github.com/LanSoSdk/LanSongEditor_IOS)
     欢迎您的评估使用.

### 联系方式:
   -  我们是杭州蓝松科技有限公司,需要盈利来维持公司运营, 免费的用户请在issue中提交你的问题,我们会一一解答,敬请谅解;
   -  联系方式:
      -  网站: www.lansongtech.com
      -  邮箱: support@lansongtech.com
      -  QQ : 1852600324
               
