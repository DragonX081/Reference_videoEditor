package com.lansosdk.videoeditor;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;




import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;


/**
 * 如果您想扩展ffmpeg的命令, 可以继承这个类,然后在其中想我们的各种executeXXX的举例一样来使用,不要直接修改我们的这个文件, 以方便以后的sdk更新升级.
 *
 *  此类的executeXXX的方法，是阻塞性执行， 即调用后，会一直阻塞在这里执行，直到执行完退出后，才执行下一行代码。您可以仿照我们的例子，采用ASynctask的形式或new Thread的形式来做。
 *
 *  提示二:   最简单的调用形式是:(easy demo):
 *
 *  在一个线程中,或AsyncTask中执行如下操作:
 *
 *  VideoEditor veditor=new VideoEditor();
 *
 *  veditor.setOnProgessListener(XXXXX);
 *
 *  mEditor.executeXXXXX();
 *
 *
 *  提示三:
 *  这些方法的底层，虽然方法在执行中，处于阻塞状态，但我们已经开启了另一个异步处理线程去执行。
 *  建议不要用多线程操作, 因为没有意义, 需要用到编解码的方法, 因硬件在大部分的手机SoC中就一个编解码器, 多个线程一样要排队执行.
 *  不需要用到编解码器的方法, 耗时很小,基本等于数据拷贝的时间, 也没有意义开多个线程.
 *
 *
 * 提示四:
 * 以下所有的需要用到filter的方法(注释中有:【此方法用到编解码】的)， 都可以用一条命令来完成， 比如你要同时执行倒叙+裁剪+水印， 可以用一个方法一次性执行完成，不必要执行两三次的编解码操作。
 * 可以联系我们， 在合作后为您定制方法。
 */
public class VideoEditor {


private static final String TAG="VideoEditor";

public static final int VIDEO_EDITOR_EXECUTE_SUCCESS1 =0;
public static final int VIDEO_EDITOR_EXECUTE_SUCCESS2 =1;
public static final int VIDEO_EDITOR_EXECUTE_FAILED =-101;  //文件不存在。



private final int VIDEOEDITOR_HANDLER_PROGRESS=203;
private final int VIDEOEDITOR_HANDLER_COMPLETED=204;


//	  #define MEDIACODEC_ERROR_NONE  0
//
////   在高通 骁龙616（MSM8939）主要是这个
//	  #define MEDIACODEC_ERROR_DEQUEUE_OUTPUT_BUFFER  0x6801
//	  #define MEDIACODEC_ERROR_GET_OUTPUT_FORMAT 0x6802
//	  #define MEDIACODEC_ERROR_GET_OUTPUT_BUFFER 0x6803
//	  #define MEDIACODEC_ERROR_QUEUE_INPUT_BUFFER  0x6804
//	  #define MEDIACODEC_ERROR_GET_INPUT_BUFFER  0x6805
//	  #define MEDIACODEC_ERROR_DEQUEUE_INPUT_BUFFER  0x6806
/**
 * 构造方法.
 * 如果您想扩展ffmpeg的命令, 可以继承这个类,然后在其中像我们的各种executeXXX的举例一样来拼接ffmpeg的命令;不要直接修改我们的这个文件, 以方便以后的sdk更新升级.
 *
 */

public VideoEditor() {
	// TODO Auto-generated constructor stub
	Looper looper;
	if ((looper = Looper.myLooper()) != null) {
		mEventHandler = new EventHandler(this, looper);
	} else if ((looper = Looper.getMainLooper()) != null) {
		mEventHandler = new EventHandler(this, looper);
	} else {
		mEventHandler = null;
		Log.w(TAG,"cannot get Looper handler. may be cannot receive video editor progress!!");
	}
}


public onVideoEditorProgressListener mProgressListener=null;
/**
 * @param listener
 */
public void setOnProgessListener(onVideoEditorProgressListener listener)
{
	mProgressListener=listener;
}
private void doOnProgressListener(int timeMS)
{
	if(mProgressListener!=null)
		mProgressListener.onProgress(this,timeMS);
}
private EventHandler mEventHandler;
private  class EventHandler extends Handler {
	private final WeakReference<VideoEditor> mWeakExtract;
	
	public EventHandler(VideoEditor mp, Looper looper) {
		super(looper);
		mWeakExtract = new WeakReference<VideoEditor>(mp);
	}
	
	@Override
	public void handleMessage(Message msg) {
		VideoEditor videoextract = mWeakExtract.get();
		if(videoextract==null){
			Log.e(TAG,"VideoExtractBitmap went away with unhandled events");
			return ;
		}
		switch (msg.what) {
			case VIDEOEDITOR_HANDLER_PROGRESS:
				videoextract.doOnProgressListener(msg.arg1);
				break;
			default:
				break;
		}
	}
}
/**
 * 异步线程执行的代码.
 */
public int executeVideoEditor(String[] array)  {
	return execute(array);
}

@SuppressWarnings("unused") /* Used from JNI */
private void postEventFromNative(int what,int arg1, int arg2) {
	Log.i(TAG,"postEvent from native  is:"+what);
	
	if(mEventHandler!=null){
		Message msg=mEventHandler.obtainMessage(VIDEOEDITOR_HANDLER_PROGRESS);
		msg.arg1=what;
		mEventHandler.sendMessage(msg);
	}
}
/**
 * 执行成功,返回0, 失败返回错误码.
 * @param cmdArray  ffmpeg命令的字符串数组, 可参考此文件中的各种方法举例来编写.
 * @return  执行成功,返回0, 失败返回错误码.
 */
private native int execute(Object cmdArray);

/**
 * 新增 在执行过程中取消的方法.
 * 如果在执行中调用了这个方法, 则会直接终止当前的操作.
 * 此方法仅仅是在ffmpeg线程中设置一个标志位,当前这一帧处理完毕后, 会检测到这个标志位,从而退出.
 * 因为execute是阻塞执行, 你可以判断execute有没有执行完,来判断是否完成.
 *
 */
public native void cancel();
/**
 *
 * 【此方法用到编解码】
 * @param srcPath
 * @param dstPath
 * @param duration
 * @param bitrate
 * @return
 */
public int executePicture2Video(String srcPath,String dstPath,float duration,int bitrate)
{
	//ffmpeg -loop 1 -i 9.jpg -t 5 -c:v libx264 out.mp4
	if(fileExist(srcPath))
	{
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-loop");
		cmdList.add("1");
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(duration));
		
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-pix_fmt");   //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
		cmdList.add("yuv420p");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}

/**
 * 拷贝文件, 成功返回0,失败返回-1;
 * @param srcPath
 * @param dstPath
 * @return
 */
public static native int copyFile(String srcPath,String dstPath);
public static native int getLimitYear();
public static native int getLimitMonth();
public static native String getSDKVersion();

//-------------------------------------------------------------------------------
/**
 * 两个pcm格式的音频数据,(裸数据)混合.
 *
 * @param srcPach1 pcm格式的主音频
 * @param samplerate  主音频采样率
 * @param channel  主音频通道数
 * @param srcPach2 pcm格式的次音频
 * @param samplerate2  次音频采样率
 * @param channel2  次音频通道数
 * @param value1    主音频的音量
 * @param value2 次音频的音量
 * @param dstPath  输出文件.输出也是pcm格式的音频文件.
 * @return
 */
public int executePcmMix(String srcPach1,int samplerate,int channel,String srcPach2,int samplerate2,int channel2,
						 float value1,float value2,String dstPath)
{
	List<String> cmdList=new ArrayList<String>();
	
	String filter=String.format(Locale.getDefault(),"[0:a]volume=volume=%f[a1]; [1:a]volume=volume=%f[a2]; [a1][a2]amix=inputs=2:duration=first:dropout_transition=2",value1,value2);
	
	cmdList.add("-f");
	cmdList.add("s16le");
	cmdList.add("-ar");
	cmdList.add(String.valueOf(samplerate));
	cmdList.add("-ac");
	cmdList.add(String.valueOf(channel));
	cmdList.add("-i");
	cmdList.add(srcPach1);
	
	cmdList.add("-f");;
	cmdList.add("s16le");
	cmdList.add("-ar");
	cmdList.add(String.valueOf(samplerate2));
	cmdList.add("-ac");
	cmdList.add(String.valueOf(channel2));
	cmdList.add("-i");
	cmdList.add(srcPach2);
	
	cmdList.add("-y");
	cmdList.add("-filter_complex");
	cmdList.add(filter);
	cmdList.add("-f");
	cmdList.add("s16le");
	cmdList.add("-acodec");
	cmdList.add("pcm_s16le");
	cmdList.add(dstPath);
	
	
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return executeVideoEditor(command);
}
/**
 * 把pcm格式的音频文件编码成AAC
 * @param srcPach  源pcm文件
 * @param samplerate  pcm的采样率
 * @param channel pcm的通道数
 * @param dstPath  输出的aac文件路径, 需要后缀是aac或m4a
 * @return
 */
public int executePcmEncodeAac(String srcPach,int samplerate,int channel,String dstPath)
{
	List<String> cmdList=new ArrayList<String>();
	
	cmdList.add("-f");
	cmdList.add("s16le");
	cmdList.add("-ar");
	cmdList.add(String.valueOf(samplerate));
	cmdList.add("-ac");
	cmdList.add(String.valueOf(channel));
	cmdList.add("-i");
	cmdList.add(srcPach);
	
	
	cmdList.add("-acodec");
	cmdList.add("libfaac");
	cmdList.add("-b:a");
	cmdList.add("64000");
	cmdList.add("-y");
	
	cmdList.add(dstPath);
	
	
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return executeVideoEditor(command);
}
/**
 * 把 pcm和视频文件合并在一起, pcm数据会编码成aac格式.
 * 注意:需要原视频文件里没有音频部分, 如果有, 则需要先用 {@link #executeDeleteAudio(String, String)}删除后, 在输入到这里.
 * @param srcPcm  原pcm音频文件,
 * @param samplerate pcm的采样率
 * @param channel  pcm的通道数
 * @param srcVideo  原视频文件, 没有音频部分
 * @param dstPath  输出的视频文件路径, 需后缀是mp4格式.
 * @return
 */
public int executePcmComposeVideo(String srcPcm,int samplerate,int channel,String srcVideo,String dstPath)
{
	List<String> cmdList=new ArrayList<String>();
	
	cmdList.add("-f");
	cmdList.add("s16le");
	cmdList.add("-ar");
	cmdList.add(String.valueOf(samplerate));
	cmdList.add("-ac");
	cmdList.add(String.valueOf(channel));
	cmdList.add("-i");
	cmdList.add(srcPcm);
	
	cmdList.add("-i");
	cmdList.add(srcVideo);
	
	cmdList.add("-acodec");
	cmdList.add("libfaac");
	cmdList.add("-b:a");
	cmdList.add("64000");
	cmdList.add("-y");
	
	cmdList.add("-vcodec");
	cmdList.add("copy");
	
	cmdList.add(dstPath);
	
	
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return executeVideoEditor(command);
}
/**
 * 两个音频文件延迟混合, 即把第二个音频延迟多长时间后, 与第一个音频混合.
 *  混合后的编码为aac格式的音频文件.
 * 注意,如果两个音频的时长不同, 以第一个音频的音频为准. 如需修改可联系我们或查询ffmpeg命令即可.
 *
 * @param audioPath1
 * @param audioPath2
 * @param leftDelayMS  第二个音频的左声道 相对 于第一个音频的延迟时间
 * @param rightDelayMS 第二个音频的右声道 相对 于第一个音频的延迟时间
 * @param dstPath   目标文件, 保存为aac格式.
 * @return
 */
public int executeAudioDelayMix(String audioPath1,String audioPath2,int leftDelayMS,int rightDelayMS,String dstPath)
{
	List<String> cmdList=new ArrayList<String>();
	String overlayXY=String.format(Locale.getDefault(),"[1:a]adelay=%d|%d[delaya1]; [0:a][delaya1]amix=inputs=2:duration=first:dropout_transition=2",leftDelayMS,rightDelayMS);
	
	
	cmdList.add("-i");
	cmdList.add(audioPath1);
	
	cmdList.add("-i");
	cmdList.add(audioPath2);
	
	cmdList.add("-filter_complex");
	cmdList.add(overlayXY);
	
	cmdList.add("-acodec");
	cmdList.add("libfaac");
	
	cmdList.add("-y");
	cmdList.add(dstPath);
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
}
/**
 * 两个音频文件混合.
 * 混合后的文件压缩格式是aac格式, 故需要您dstPath的后缀是aac或m4a.
 *
 *
 * @param audioPath1  主音频的完整路径
 * @param audioPath2  次音频的完整路径
 * @param value1  主音频的音量, 浮点类型, 大于1.0为放大音量, 小于1.0是减低音量.比如设置0.5则降低一倍.
 * @param value2  次音频的音量, 浮点类型.
 * @param dstPath  输出保存的完整路径.需要文件名的后缀是aac 或 m4a格式.
 * @return
 */
public int executeAudioVolumeMix(String audioPath1,String audioPath2,float value1,float value2,String dstPath)
{
	List<String> cmdList=new ArrayList<String>();
	
	String filter=String.format(Locale.getDefault(),"[0:a]volume=volume=%f[a1]; [1:a]volume=volume=%f[a2]; [a1][a2]amix=inputs=2:duration=first:dropout_transition=2",value1,value2);
	
	
	cmdList.add("-i");
	cmdList.add(audioPath1);
	
	cmdList.add("-i");
	cmdList.add(audioPath2);
	
	cmdList.add("-filter_complex");
	cmdList.add(filter);
	
	cmdList.add("-acodec");
	cmdList.add("libfaac");
	
	cmdList.add("-y");
	cmdList.add(dstPath);
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
}
//--------------------------------------------------------------------------
/**
 * 把h264裸码流数据包装成MP4格式,因为是裸码流,未知帧率, 包装成MP4默认帧率是25帧/秒
 *
 *
 * 注意,这里面没有音频数据.,H264裸码流是通过编码得到的数据直接写入文件的数据.
 * @param srcPath
 * @param dstPath
 * @return
 */
public static int executeH264WrapperMp4(String srcPath,String dstPath)
{
	if(fileExist(srcPath)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-vcodec");
		cmdList.add("copy");
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		VideoEditor veditor=new VideoEditor();
		return veditor.executeVideoEditor(command);
	}
	return VIDEO_EDITOR_EXECUTE_FAILED;
}
//--------------------------------------------------------------------------
/**
 * 视频转码.
 * 通过调整视频的bitrate来对视频文件大小的压缩,降低视频文件的大小, 注意:压缩可能导致视频画质下降.
 *
 * 此命令为单纯压缩命令, 如需对视频进行裁剪/增加水印/增加文字等需要编解码的场合, 可以在执行的方法中直接压缩,这样节省一倍的时间, 没有必要等其他命令执行完后,再执行此方法.
 * 比如如下方法:
 * {@link #executeCropOverlay(String, String, String, int, int, int, int, int, int, String, int)}
 * {@link #executeVideoCutCropOverlay(String, String, String, float, float, int, int, int, int, int, int, String, int)}
 * {@link #executeAddWaterMark(String, String, int, int, String, int)}
 * {@link #executeAddWaterMark(String, String, float, float, int, int, String, int)}
 *
 * 【此方法用到编解码】
 * @param srcPath 源视频
 * @param dstPath 目的视频
 * @param percent 压缩百分比.值从0--1
 * @return
 */
public int executeVideoCompress(String srcPath,String dstPath,float percent)
{
	if(fileExist(srcPath)){
		
		MediaInfo info=new MediaInfo(srcPath,false);
		if(info.prepare())
		{
			List<String> cmdList=new ArrayList<String>();
			
			cmdList.add("-vcodec");
			cmdList.add(info.vCodecName);
			
			cmdList.add("-i");
			cmdList.add(srcPath);
			cmdList.add("-acodec");
			cmdList.add("copy");
			
			cmdList.add("-vcodec");
			cmdList.add("lansoh264_enc");
			
			cmdList.add("-b:v");
			float bitrate=info.vBitRate*percent;
			int nbitrate=(int)bitrate;
			cmdList.add(checkBitRate(nbitrate));
			
			cmdList.add("-pix_fmt");  //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
			cmdList.add("yuv420p");
			
			cmdList.add("-y");
			cmdList.add(dstPath);
			String[] command=new String[cmdList.size()];
			for(int i=0;i<cmdList.size();i++){
				command[i]=(String)cmdList.get(i);
			}
			return executeVideoEditor(command);
		}
	}
	return VIDEO_EDITOR_EXECUTE_FAILED;
}
/**
 * 分离mp4文件中的音频,并返回音频的路径,这个音频路径是放到{@link SDKDir#TMP_DIR}下的以当前时间为文件名的文件路径.
 *
 * @param srcMp4Path
 * @return
 */
public static String spliteAudioFile(String srcMp4Path)
{
	MediaInfo  info=new MediaInfo(srcMp4Path,false);
	info.prepare();
	
	String audioPath=null;
	if(info.aCodecName.equalsIgnoreCase("aac")){
		audioPath=SDKFileUtils.createFile(SDKDir.TMP_DIR, ".aac");
	}else if(info.aCodecName.equalsIgnoreCase("mp3"))
		audioPath=SDKFileUtils.createFile(SDKDir.TMP_DIR, ".mp3");
	
	if(audioPath!=null){
		VideoEditor veditor=new VideoEditor();
		veditor.executeDeleteVideo(srcMp4Path, audioPath);
	}
	return audioPath;
}
/**
 * 把原视频文件中的音频部分, 增加到新的视频中,
 *
 * @param oldMp4   源视频, 需要内部有音频部分, 如没有音频则则方法无动作.
 * @param newMp4   通过视频录制后,保存的新视频.里面只有视频部分或h264裸码流,需确保里面没有音频部分.
 * @param tmpDir  此方法处理过程中生成的临时文件存放地, 临时文件夹路径.
 * @param dstMp4   方法处理完后, 增加音频后的文件目标路径.
 * @return  执行成功,返回true, 失败返回false(一般源视频中没有音频会执行失败)
 */
public static boolean encoderAddAudio(String oldMp4,String newMp4,String tmpDir,String dstMp4)
{
	//
	MediaInfo  info=new MediaInfo(oldMp4,false);
	if(info.prepare())
	{
		String audioPath=null;
		if(info.aCodecName!=null)  //只有在有音频的场合,才增加.
		{
			if(info.aCodecName.equalsIgnoreCase("aac")){
				audioPath=SDKFileUtils.createFile(tmpDir, ".aac");
			}else if(info.aCodecName.equalsIgnoreCase("mp3"))
				audioPath=SDKFileUtils.createFile(tmpDir, ".mp3");
			
			if(audioPath!=null){
				VideoEditor veditor=new VideoEditor();
				veditor.executeDeleteVideo(oldMp4, audioPath);  //获得音频
				veditor.executeVideoMergeAudio(newMp4, audioPath, dstMp4);  //合并到新视频文件中.
				SDKFileUtils.deleteFile(audioPath);
				return true;
			}
		}else{
			Log.w(TAG,"old mp4 file no audio . do not add audio");
		}
	}else{
		Log.w(TAG,"old mp4 file prepare error!!,do not add audio");
	}
	return false;
}
/**
 * 用新的音频文件, 替换掉原来视频文件中的音频轨道.
 *
 * @param oldMp4
 * @param audioPath
 * @param dstMp4
 * @return
 */
public static boolean videoReplaceNewAudio(String oldMp4,String audioPath,String dstMp4)
{
	//
	MediaInfo  info=new MediaInfo(oldMp4,false);
	if(info.prepare() && audioPath!=null)
	{
		VideoEditor veditor=new VideoEditor();
		String videoPath=SDKFileUtils.createMp4FileInBox();
		//先删除原来的音频.
		veditor.executeDeleteAudio(oldMp4, videoPath);  //获得音频
		//再增加上新的音频文件.
		veditor.executeVideoMergeAudio(videoPath, audioPath, dstMp4);  //合并到新视频文件中.
		
		SDKFileUtils.deleteFile(videoPath);
		return true;
	}else{
		Log.w(TAG,"old mp4 file prepare error!!,do not add audio");
	}
	return false;
}

private static boolean fileExist(String absolutePath)
{
	if(absolutePath==null)
		return false;
	else
		return (new File(absolutePath)).exists();
}

private static boolean filesExist(String[] fileArray)
{
	
	for(String file: fileArray)
	{
		if(fileExist(file)==false)
			return false;
	}
	return true;
}
/**
 * 删除多媒体文件中的音频,把多媒体中的视频部分提取出来，这样提出的视频播放，就没有声音了，
 * 适用在当想给一个多媒体文件更换声音的场合的。您可以用这个方法删除声音后，通过{@link executeVideoEditor} 重新为视频增加一个声音。
 * @param srcFile  输入的MP4文件
 * @param dstFile 删除音频后的多媒体文件的输出绝对路径,路径的文件名类型是.mp4
 * @return 返回执行的结果.
 */
public int executeDeleteAudio(String srcFile,String dstFile)
{
	if(fileExist(srcFile)){
		List<String> cmdList=new ArrayList<String>();
		cmdList.add("-i");
		cmdList.add(srcFile);
		cmdList.add("-vcodec");
		cmdList.add("copy");
		cmdList.add("-an");
		cmdList.add("-y");
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 删除多媒体文件中的视频部分，一个mp4文件如果是音频和视频一起的，等于提取多媒体文件中的音频，
 *
 * @param srcFile  要处理的多媒体文件,里面需要有视频
 * @param dstFile  删除视频部分后的音频保存绝对路径, 注意:如果多媒体中是音频是aac压缩,则后缀必须是aac. 如果是mp3压缩,则后缀必须是mp3,
 * @return 返回执行的结果.
 */
public int executeDeleteVideo(String srcFile,String dstFile)
{
	if(fileExist(srcFile)==false)
		return VIDEO_EDITOR_EXECUTE_FAILED;
	
	List<String> cmdList=new ArrayList<String>();
	cmdList.add("-i");
	cmdList.add(srcFile);
	cmdList.add("-acodec");
	cmdList.add("copy");
	cmdList.add("-vn");
	cmdList.add("-y");
	cmdList.add(dstFile);
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
}
/**
 * 音频和视频合成为多媒体文件，等于给视频增加一个音频。
 *
 * 2017年4月5日 增加: 默认以视频的时长为最终目标视频的长度.
 * @param videoFile 输入的视频文件,需视频文件中不存储音频部分. 如有音频部分, 建议用 {@link #executeDeleteAudio(String, String)}把音频删除后的目标文件作为当前的输入.
 * @param audioFile 输入的音频文件
 * @param dstFile  合成后的输出，文件名的后缀是.mp4
 * @return 返回执行的结果.
 *
 */
public int executeVideoMergeAudio(String videoFile,String audioFile,String dstFile)
{
	boolean isAAC=false;
	
	MediaInfo vInfo=new MediaInfo(videoFile,false);
	MediaInfo aInfo=new MediaInfo(audioFile,false);
	if(vInfo.prepare() && aInfo.prepare()){
		
		if(aInfo.aCodecName.equals("aac")){
			isAAC=true;
		}
		List<String> cmdList=new ArrayList<String>();
		cmdList.add("-i");
		cmdList.add(videoFile);
		cmdList.add("-i");
		cmdList.add(audioFile);
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(vInfo.vDuration));
		
		cmdList.add("-vcodec");
		cmdList.add("copy");
		cmdList.add("-acodec");
		cmdList.add("copy");
		if(isAAC){
			cmdList.add("-absf");
			cmdList.add("aac_adtstoasc");
		}
		cmdList.add("-y");
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 音频和视频合成为多媒体文件，等于给视频增加一个音频。
 *
 * 2017年4月5日 增加: 默认以视频的时长为最终目标视频的长度.
 * @param videoFile 输入的视频文件,需视频文件中不存储音频部分. 如有音频部分, 建议用 {@link #executeDeleteAudio(String, String)}把音频删除后的目标文件作为当前的输入.
 * @param audioFile 输入的音频文件
 * @param dstFile  合成后的输出，文件名的后缀是.mp4
 * @return 返回执行的结果.
 *
 */
public int executeVideoMergeAudio2(String videoFile,String audioFile,String dstFile)
{
	boolean isAAC=false;
	
	MediaInfo vInfo=new MediaInfo(videoFile,false);
	MediaInfo aInfo=new MediaInfo(audioFile,false);
	if(vInfo.prepare() && aInfo.prepare()){
		
		if(aInfo.aCodecName.equals(".aac")){
			isAAC=true;
		}
		List<String> cmdList=new ArrayList<String>();
		cmdList.add("-i");
		cmdList.add(videoFile);
		cmdList.add("-i");
		cmdList.add(audioFile);
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(vInfo.vDuration));
		
		cmdList.add("-vcodec");
		cmdList.add("copy");
		cmdList.add("-acodec");
		cmdList.add("copy");
		if(isAAC){
			cmdList.add("-absf");
			cmdList.add("aac_adtstoasc");
		}
		cmdList.add("-y");
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 *
 * 给视频MP4增加上音频，audiostartS表示从从音频的哪个时间点开始增加，单位是秒
 * 注意:原视频文件里必须是没有音频部分.
 *
 * @param videoFile  原视频文件,只有视频部分的多媒体文件.
 * @param audioFile  需要增加的音频文件
 * @param dstFile  处理后保存的路径 文件名的后缀需要.mp4格式
 * @param audiostartS  音频增加的时间点，单位秒，类型float，可以有小数，比如从音频的2.35秒开始增加到视频中。
 * @return
 */
public int executeVideoMergeAudio(String videoFile,String audioFile,String dstFile,float audiostartS)
{
	boolean isAAC=false;
	if(fileExist(videoFile) && fileExist(audioFile)){
		
		if(audioFile.endsWith(".aac")){
			isAAC=true;
		}
		List<String> cmdList=new ArrayList<String>();
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-ss");
		cmdList.add(String.valueOf(audiostartS));
		
		cmdList.add("-i");
		cmdList.add(audioFile);
		cmdList.add("-vcodec");
		cmdList.add("copy");
		cmdList.add("-acodec");
		cmdList.add("copy");
		if(isAAC){
			cmdList.add("-absf");
			cmdList.add("aac_adtstoasc");
		}
		cmdList.add("-y");
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 给视频文件增加一个音频, 注意,这里是因音频的时长为目标视频文件的时长.
 * 输出文件后缀是.mp4格式.
 * @param videoFile
 * @param audioFile
 * @param dstFile
 * @param audiostartS  音频开始时间, 单位秒,可以有小数, 比如2.5秒
 * @param audiodurationS 音频增加的总时长.您可以只增加音频中一部分，比如增加音频的2.5秒到--180秒这段声音到视频文件中，则这里的参数是180
 * @return
 */
public int executeVideoMergeAudio(String videoFile,String audioFile,String dstFile,float audiostartS,float audiodurationS)
{
	boolean isAAC=false;
	if(fileExist(videoFile) && fileExist(audioFile)){
		
		if(audioFile.endsWith(".aac")){
			isAAC=true;
		}
		List<String> cmdList=new ArrayList<String>();
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-i");
		cmdList.add(audioFile);
		
		cmdList.add("-ss");
		cmdList.add(String.valueOf(audiostartS));
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(audiodurationS));
		
		cmdList.add("-vcodec");
		cmdList.add("copy");
		cmdList.add("-acodec");
		cmdList.add("copy");
		if(isAAC){
			cmdList.add("-absf");
			cmdList.add("aac_adtstoasc");
		}
		cmdList.add("-y");
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 音频裁剪,截取音频文件中的一段.
 * 需要注意到是: 尽量保持裁剪文件的后缀名和源音频的后缀名一致.
 * @param srcFile   源音频
 * @param dstFile  裁剪后的音频
 * @param startS  开始时间,单位是秒. 可以有小数
 * @param durationS  裁剪的时长.
 * @return
 */
public int executeAudioCutOut(String srcFile,String dstFile,float startS,float durationS)
{
	if(fileExist(srcFile)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-ss");
		cmdList.add(String.valueOf(startS));
		
		cmdList.add("-i");
		cmdList.add(srcFile);
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(durationS));
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		cmdList.add("-y");
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 *
 * 剪切mp4文件.(包括视频文件中的音频部分和视频部分),即把mp4文件中的一段剪切成独立的一个视频文件, 比如把一个1分钟的视频,裁剪其中的10秒钟等.
 *
 * 注意: 此方法裁剪不是精确裁剪,而是从视频的IDR帧开始裁剪的, 没有精确到您指定的那一帧的时间, 如果您指定的时间不是IDR帧上的时间,则退后到上一个IDR帧开始.
 *
 * @param videoFile  原视频文件 文件格式是mp4
 * @param dstFile   裁剪后的视频路径， 路径的后缀名是.mp4
 * @param startS   开始裁剪位置，单位是秒，
 * @param durationS  需要裁剪的时长，单位秒，比如您可以从原视频的8.9秒出开始裁剪，裁剪2分钟，则这里的参数是120
 * @return
 */
public int executeVideoCutOut(String videoFile,String dstFile,float startS,float durationS)
{
	if(fileExist(videoFile)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-ss");
		cmdList.add(String.valueOf(startS));
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(durationS));
		
		cmdList.add("-vcodec");
		cmdList.add("copy");
		cmdList.add("-acodec");
		cmdList.add("copy");
		cmdList.add("-y");
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 *  对视频时长进行 精确裁剪， 把mp4文件中的一段剪切成独立的一个视频文件, 比如把一个1分钟的视频,裁剪其中的10秒钟等.
 *  我们高级版本有直接投递到drawPad中的方法, 如果您使用我们的高级版本, 则建议用DrawPad的方式来处理,那样更快.
 *
 *  因为视频编码原理是根据IDR来裁剪, 要做到精确裁剪到指定时间, 则有可能指定的时间不是IDR帧的时间戳, 这时就需要先解码,然后编码的操作.
 *  这里的精确裁剪是先解码然后编码来统一完成精确裁剪.
 *  注意:此方法为精确裁剪, 流程是:先解码,然后编码. 如果您还实际中还需要别的功能,比如增加LOGO,缩放等操作,建议把两个命令合并在一起,从而减少一次编解码.
 *  【此方法用到编解码】
 *  举例:
 *editor.executeVideoCutExact(srcVideo, info.vCodecName, dstVideo, 0.6f, 1.0f, (int)((float)info.vBitRate*1.2f));
 *
 * @param videoFile  原视频
 * @param decoder  原视频用到的解码器, 如果有硬件解码器,则建议用硬件解码器.用MediaInfo可以获取.
 * @param dstFile  裁剪后目标文件路径，
 * @param startS  开始裁剪位置，单位是秒，
 * @param durationS  需要裁剪的时长，单位秒，比如您可以从原视频的8.9秒出开始裁剪，裁剪2分钟，则这里的参数是120
 * @param bitrate  因为需要编码， 设置编码的码率。 建议用MediaInfo中的vbitrate*1.2f
 * @return
 */
public int executeVideoCutExact(String videoFile,String decoder,String dstFile,float startS,float durationS,int bitrate)
{
	if(fileExist(videoFile)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-ss");
		cmdList.add(String.valueOf(startS));
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(durationS));
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-y");
		cmdList.add(dstFile);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 精确裁剪的同时,缩放到指定位置,不同于上面的命令,这个可以设置宽度和高度. 其中宽度和高度是采用缩放来完成.
 *
 *
 * 采用的是软缩放的形式.
 * @param videoFile
 * @param decoder
 * @param dstFile
 * @param startS
 * @param durationS
 *
 * @param width  要缩放到的宽度 建议是16的倍数 ,如果不是,则可以用 {@link #make16Multi(int)}来得到
 * @param height 要缩放到的高度, 建议是16的倍数
 * @param bitrate
 * @return
 */
public int executeVideoCutExact(String videoFile,String decoder,String dstFile,
								float startS,float durationS,int width,int height,int bitrate)
{
	if(fileExist(videoFile)){
		List<String> cmdList=new ArrayList<String>();
		
		String scalecmd=String.format(Locale.getDefault(),"scale=%d:%d",width,height);
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-ss");
		cmdList.add(String.valueOf(startS));
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(durationS));
		
		cmdList.add("-vf");
		cmdList.add(scalecmd);
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-y");
		cmdList.add(dstFile);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 *
 * 对视频时长进行 精确裁剪， 并在对视频精确编码的同时,对音频进行编码.
 *
 * 把mp4文件中的一段剪切成独立的一个视频文件, 比如把一个30分钟的视频,裁剪其中的10秒钟等.
 *
 * @param videoFile 原视频
 * @param dstFile  裁剪后目标文件路径，
 * @param startS    开始裁剪位置，单位是秒，
 * @param durationS 需要裁剪的时长，单位秒，比如您可以从原视频的8.9秒出开始裁剪，裁剪2分钟，则这里的参数是120
 * @param bitrate  因为需要编码， 设置编码的码率。 建议用MediaInfo中的vbitrate*1.2f
 * @param encodeAudio 是否对音频进行编码.
 * @return
 */
public int executeVideoExactCut(String videoFile,String dstFile,float startS,float durationS,int bitrate,boolean encodeAudio)
{
	if(fileExist(videoFile)){
		
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-ss");
		cmdList.add(String.valueOf(startS));
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(durationS));
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		cmdList.add("-acodec");
		if(encodeAudio){
			cmdList.add("libfaac");
		}else{
			cmdList.add("copy");
		}
		
		cmdList.add("-y");
		cmdList.add(dstFile);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 【此方法用到编解码】
 * 对视频时长剪切的同时, 对画面进行裁剪.
 * @param videoFile
 * @param dstFile
 * @param startS
 * @param durationS
 * @param cropWidth
 * @param cropHeight
 * @param x
 * @param y
 * @param bitrate
 * @param encodeAudio
 * @return
 */
public int executeVideoExactCut(String videoFile,String dstFile,float startS,float durationS,int cropWidth,int cropHeight,int x,int y,int bitrate,boolean encodeAudio)
{
	if(fileExist(videoFile)){
		
		String cropcmd=String.format(Locale.getDefault(),"crop=%d:%d:%d:%d",cropWidth,cropHeight,x,y);
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-ss");
		cmdList.add(String.valueOf(startS));
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(durationS));
		
		cmdList.add("-vf");
		cmdList.add(cropcmd);
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		cmdList.add("-acodec");
		if(encodeAudio){
			cmdList.add("libfaac");
		}else{
			cmdList.add("copy");
		}
		
		cmdList.add("-y");
		cmdList.add(dstFile);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 获取视频的所有帧图片,并保存到指定路径.
 * 所有的帧会按照后缀名字加上_001.jpeg prefix_002.jpeg的顺序依次生成, 如果发现之前已经有同样格式的文件,则在原来数字后缀的基础上增加, 比如原来有prefix_516.jpeg;则这个方法执行从
 * prefix_517.jpeg开始生成视频帧.
 *
 *
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *
 *
 * 这条命令是把视频中的所有帧都提取成图片，适用于视频比较短的场合，比如一秒钟是25帧，视频总时长是10秒，则会提取250帧图片，保存到您指定的路径
 * @param videoFile
 * @param dstDir  目标文件夹绝对路径.
 * @param jpgPrefix   保存图片文件的前缀，可以是png或jpg
 * @return
 */
public int executeGetAllFrames(String videoFile,String decoder,String dstDir,String jpgPrefix)
{
	String dstPath=dstDir+jpgPrefix+"_%3d.jpeg";
	if(fileExist(videoFile)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-qscale:v");
		cmdList.add("2");
		
		cmdList.add(dstPath);
		
		cmdList.add("-y");
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 根据设定的采样,获取视频的几行图片.
 * 假如视频时长是30秒,想平均取5张图片,则sampleRate=5/30;
 *
 *
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *
 *
 * @param videoFile
 * @param dstDir
 * @param jpgPrefix
 * @param sampeRate  一秒钟采样几张图片. 可以是小数.
 * @return
 */
public int executeGetSomeFrames(String videoFile,String dstDir,String jpgPrefix,float sampeRate)
{
	String dstPath=dstDir+jpgPrefix+"_%3d.jpeg";
	if(fileExist(videoFile)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_dec");
		
		cmdList.add("-i");
		cmdList.add(videoFile);

//					cmdList.add("-qscale:v");
//					cmdList.add("2");
		
		cmdList.add("-vsync");
		cmdList.add("1");
		
		cmdList.add("-r");
		cmdList.add(String.valueOf(sampeRate));

//					cmdList.add("-f");
//					cmdList.add("image2");
		
		cmdList.add("-y");
		
		cmdList.add(dstPath);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 *  读取视频中的关键帧(IDR帧), 并把关键帧保存图片. 因是IDR帧, 在编码时没有起帧做参考,故提取的最快.
 *
 *
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *如果您使用的是高级版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
 *
 *
 * 经过我们SDK编码后的视频, 是一秒钟一个帧,如果您视频大小是30秒,则大约会提取30张图片.
 *
 * @param videoFile  视频文件
 * @param dstDir  保持的文件夹
 * @param jpgPrefix  文件前缀.
 * @return
 */
public int executeGetKeyFrames(String videoFile,String dstDir,String jpgPrefix)
{
	//	  ffmpeg -i 22.MP4 -vf "select=eq(pict_type\,I)" -vsync vfr FF%04d.jpg
	String dstPath=dstDir+"/"+jpgPrefix+"_%3d.png";
	if(fileExist(videoFile)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-vf");
		cmdList.add("select=eq(pict_type\\,I)");
		
		cmdList.add("-vsync");
		cmdList.add("vfr");
		
		Log.i(TAG," vsync is vfr");
		
		cmdList.add("-y");
		
		cmdList.add(dstPath);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}

/**
 * 来自于网络, 没有全部测试.
 * 获取视频的缩略图
 * 提供了一个统一的接口用于从一个输入媒体文件中取得帧和元数据。
 * @param path 视频的路径
 * @param width 缩略图的宽
 * @param height 缩略图的高
 * @return 缩略图
 */
public static Bitmap createVideoThumbnail(String path, int width, int height) {
	Bitmap bitmap = null;
	MediaMetadataRetriever retriever = new MediaMetadataRetriever();
	if (TextUtils.isEmpty(path)) {
		return null;
	}
	
	File file = new File(path);
	if (!file.exists()) {
		return null;
	}
	
	try {
		retriever.setDataSource(path);
		bitmap = retriever.getFrameAtTime(-1); //取得指定时间的Bitmap，即可以实现抓图（缩略图）功能
	} catch (IllegalArgumentException ex) {
		// Assume this is a corrupt video file
	} catch (RuntimeException ex) {
		// Assume this is a corrupt video file.
	} finally {
		try {
			retriever.release();
		} catch (RuntimeException ex) {
			// Ignore failures while cleaning up.
		}
	}
	
	if (bitmap == null) {
		return null;
	}
	
	bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
	return bitmap;
}
/**
 * 从视频的指定位置中获取一帧图片. 因为这个是精确提取视频的一帧, 不建议作为提取缩略图来使用,用mediametadataRetriever最好.
 *
 * @param videoSrcPath 源视频的完整路径
 * @param decodeName   解码器, 由{@link MediaInfo#vCodecName}填入
 * @param postionS  时间点，单位秒，类型float，可以有小数，比如从视频的2.35秒的地方获取一张图片。
 * @param dstPng   得到目标图片的完整路径名.
 * @return
 */
public int executeGetOneFrame(String videoSrcPath,String decodeName,float postionS,String dstPng)
{
	//参考命令:ffmpeg -i input.mp4 -ss 10 -vframes 1 out.png
	if(fileExist(videoSrcPath)){
		
		List<String> cmdList=new ArrayList<String>();
//
//					cmdList.add("-vcodec");  //获取一张图片, 不需要采用硬件编码.
//					cmdList.add(decodeName);
		
		cmdList.add("-i");
		cmdList.add(videoSrcPath);
		
		cmdList.add("-ss");
		cmdList.add(String.valueOf(postionS));
		
		cmdList.add("-vframes");
		cmdList.add("1");
		
		cmdList.add("-y");
		
		cmdList.add(dstPng);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 从视频的指定位置中获取一帧图片,得到图片后,把图片缩放到指定的宽高. 因为这个是精确提取视频的一帧, 不建议作为提取缩略图来使用.
 *
 * @param videoSrcPath 源视频的完整路径
 * @param decodeName  解码器, 由{@link MediaInfo#vCodecName}填入
 * @param postionS   时间点，单位秒，类型float，可以有小数，比如从视频的2.35秒的地方获取一张图片。
 * @param pngWidth   得到目标图片后缩放的宽度.
 * @param pngHeight  得到目标图片后需要缩放的高度.
 * @param dstPng   得到目标图片的完整路径名.
 * @return
 */
public int executeGetOneFrame(String videoSrcPath,String decodeName,float postionS,int pngWidth,int pngHeight,String dstPng)
{
	//参考命令:ffmpeg -i input.mp4 -ss 10 -s 480x480 -vframes 1 out.png
	if(fileExist(videoSrcPath)){
		
		List<String> cmdList=new ArrayList<String>();
		
		String resolution=String.valueOf(pngWidth);
		resolution+="x";
		resolution+=String.valueOf(pngHeight);
		
		
		cmdList.add("-vcodec");
		cmdList.add(decodeName);
		
		cmdList.add("-i");
		cmdList.add(videoSrcPath);
		
		cmdList.add("-ss");
		cmdList.add(String.valueOf(postionS));
		
		cmdList.add("-s");
		cmdList.add(resolution);
		
		cmdList.add("-vframes");
		cmdList.add("1");
		
		cmdList.add("-y");
		
		cmdList.add(dstPng);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 把mp3转为aac格式, 需要编解码, 因为是软解后软编码, 会需要一些时间.
 *
 * @param mp3Path
 * @param dstAacPath  目标文件路径, 后缀需要是 aac或m4a, 建议用m4a
 * @return
 */
public int executeConvertMp3ToAAC(String mp3Path,String dstAacPath)
{
	if(fileExist(mp3Path)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-i");
		cmdList.add(mp3Path);
		
		cmdList.add("-acodec");
		cmdList.add("libfaac");
		
		cmdList.add("-y");
		cmdList.add(dstAacPath);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 把视频解码成mjpeg格式的mp4文件.
 *
 * 两个都需要时mp4的后缀
 *
 * @param srcPath
 * @param dstPath  两个都需要时mp4的后缀
 * @return
 */
public int executeConvertToMJpeg(String srcPath,String dstPath)
{
	if(fileExist(srcPath)){
		
		MediaInfo info=new MediaInfo(srcPath,false);
		if(info.prepare())
		{
			List<String> cmdList=new ArrayList<String>();
			
			cmdList.add("-vcodec");
			cmdList.add(info.vCodecName);
			
			cmdList.add("-i");
			cmdList.add(srcPath);
			cmdList.add("-acodec");
			cmdList.add("copy");
			
			cmdList.add("-vcodec");
			cmdList.add("mjpeg");
			
			cmdList.add("-q:v");
			cmdList.add("1");
			
			cmdList.add("-b:v");
			cmdList.add("200m");
			
			cmdList.add("-y");
			cmdList.add(dstPath);
			String[] command=new String[cmdList.size()];
			for(int i=0;i<cmdList.size();i++){
				command[i]=(String)cmdList.get(i);
			}
			return executeVideoEditor(command);
		}
	}
	return VIDEO_EDITOR_EXECUTE_FAILED;
}
/**
 * 把mp4文件转换位TS流，
 * 此命令和{＠link #executeConvertTsToMp4}结合,可以实现把多个mp4文件拼接成一个mp4文件。
 * 适用在当你需要把录制好的多段视频拼接成一个mp4的场合，或者你先把一个mp4文件裁剪成多段，然后把其中几段视频拼接在一起
 * 或者你想把两个视频增加一个转场的效果，
 *
 * 注意:如您的操作是:视频拼接,请注意!
 * 拼接时一定要注意: 此处拼接不解码, 只是对多媒体重新封装,然后把H264 NAL数据拷贝而已.
 * 如不同来源的视频, 视频的编码器可能不同,拼接是正常的, 但拼接后, 目标视频里可能有多个编码格式的画面,某些手机播放器会不支持.(VLC播放器是支持的)
 * 建议只是在同一个编码器下产生的多个视频进行拼接,
 * 如一定需要来源不同的视频拼接, 建议先解码成yuv数据,然后yuv拼接后, 再次编码
 *
 *
 * @param mp4Path　输入的mp4文件路径
 * @param dstTs　转换后保存的ts路径，后缀名需要是.ts
 * @return
 */
public int executeConvertMp4toTs(String mp4Path,String dstTs)
{
	//./ffmpeg -i 0.mp4 -c copy -bsf:v h264_mp4toannexb -f mpegts ts0.ts
//		  ./ffmpeg -i 1.mp4 -c copy -bsf:v h264_mp4toannexb -f mpegts ts1.ts
//		  ./ffmpeg -i 2.mp4 -c copy -bsf:v h264_mp4toannexb -f mpegts ts2.ts
//		  ./ffmpeg -i 3.mp4 -c copy -bsf:v h264_mp4toannexb -f mpegts ts3.ts
//		  ./ffmpeg -i "concat:ts0.ts|ts1.ts|ts2.ts|ts3.ts" -c copy -bsf:a aac_adtstoasc out2.mp4
	
	if(fileExist(mp4Path)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-i");
		cmdList.add(mp4Path);
		
		cmdList.add("-c");
		cmdList.add("copy");
		
		cmdList.add("-bsf:v");
		cmdList.add("h264_mp4toannexb");
		
		cmdList.add("-f");
		cmdList.add("mpegts");
		
		cmdList.add("-y");
		cmdList.add(dstTs);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 把多段TS流拼接在一起，然后保存成mp4格式
 * 注意:输入的各个流需要编码参数一致,
 * 适用于断点拍照,拍照多段视频; 或者想在两段视频中增加一个转场的视频
 *
 * 注意:如您的操作是:视频拼接,请注意!
 * 拼接时一定要注意: 此处拼接不解码, 只是对多媒体重新封装,然后把H264 NAL数据拷贝而已.
 * 如不同来源的视频, 视频的编码器可能不同,拼接是正常的, 但拼接后, 目标视频里可能有多个编码格式的画面,某些手机播放器会不支持.(VLC播放器是支持的)
 * 建议只是在同一个编码器下产生的多个视频进行拼接,
 * 如一定需要来源不同的视频拼接, 建议先解码成yuv数据,然后yuv拼接后, 再次编码
 *
 * @param tsArray　多段ts流的数组
 * @param dstFile　　处理后保存的路径,文件后缀名需要是.mp4
 * @return
 */
public int executeConvertTsToMp4(String[] tsArray,String dstFile)
{
	if(filesExist(tsArray)){
		
		String concat="concat:";
		for(int i=0;i<tsArray.length-1;i++){
			concat+=tsArray[i];
			concat+="|";
		}
		concat+=tsArray[tsArray.length-1];
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-i");
		cmdList.add(concat);
		
		cmdList.add("-c");
		cmdList.add("copy");
		
		cmdList.add("-bsf:a");
		cmdList.add("aac_adtstoasc");
		
		cmdList.add("-y");
		
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 把多段mp4文件拼接 在一起.
 * 注意:这里的多段mp4文件, 需要是同一段代码录制而成,分辨率,码率一致的情况下, 如果多段mp4文件中的视频来源不同, 则合成是可以的,但有些播放器不支持播放(VLC可以播放)
 * 如果您视频分辨率在720P一下, 每段比较小,建议您不用采用录制后合并的方法, 可采用先保存原始YUV数据,再需要拼接的时候,再次拼接在一起.
 * @param mp4Array 多段mp4文件
 *
 * @param dstVideo  合成后的文件路径
 */
public void executeConcatMP4(String[] mp4Array,String dstVideo)
{
	
	//第一步,先把所有的mp4转换为ts流
	ArrayList<String>  tsPathArray=new ArrayList<String>();
	for(int i=0;i<mp4Array.length;i++)
	{
		String segTs1=SDKFileUtils.createFileInBox("ts");
		executeConvertMp4toTs(mp4Array[i], segTs1);
		tsPathArray.add(segTs1);
	}
	
	//第二步: 把ts流拼接成mp4
	String[] tsPaths=new String[tsPathArray.size()];
	for(int i=0;i<tsPathArray.size();i++){
		tsPaths[i]=(String)tsPathArray.get(i);
	}
	executeConvertTsToMp4(tsPaths , dstVideo);
	
	//第三步:删除临时生成的ts文件.
	for(int i=0;i<tsPathArray.size();i++)
	{
		SDKFileUtils.deleteFile(tsPathArray.get(i));
	}
}
/**
 * 裁剪一个mp4分辨率，把视频画面的某一部分裁剪下来，
 *
 * @param videoFile　需要裁剪的视频文件
 * @param cropWidth　裁剪后的目标宽度
 * @param cropHeight 　裁剪后的目标高度
 * @param x  　视频画面开始的Ｘ坐标，　从画面的左上角开始是0.0坐标
 * @param y 视频画面开始的Y坐标，
 * @param dstFile 处理后保存的路径,后缀需要是mp4
 * @param codecname  使用的解码器的名字
 * @param bitrate  <============注意:这里的bitrate在设置的时候, 因为是设置编码器的恒定码率, 推荐设置为 预设值的1.5倍为准, 比如视频原有的码率是1M,则裁剪一半,预设值可能是500k,
 * 这里推荐是为500k的1.5,因为原有的视频大部分是动态码率VBR,可以认为通过{@link MediaInfo} 得到的 {@link MediaInfo#vBitRate}是平均码率,这里要设置,推荐是1.5倍为好.
 * @return
 */
public int executeVideoFrameCrop(String videoFile,int cropWidth,int cropHeight,int x,int y,String dstFile,String codecname,int bitrate)
{
	if( fileExist(videoFile)){
		
		String cropcmd=String.format(Locale.getDefault(),"crop=%d:%d:%d:%d",cropWidth,cropHeight,x,y);
//
		int ret=executeFrameCrop(videoFile,codecname,cropcmd,dstFile,bitrate);
		if(ret!=0){  //执行失败
			Log.w(TAG,"video editor execute video frmae crop  error,switch to software decoder...");
			ret=executeFrameCrop(videoFile,"h264",cropcmd,dstFile,bitrate);  //采用软解
		}
		return ret;
//					return executeFrameCrop(videoFile,"h264",cropcmd,dstFile,bitrate);  //仅仅测试
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
//内部使用【此方法用到编解码】
private int executeFrameCrop(String videoFile,String codecname,String filter,String dstFile,int bitrate)
{
	List<String> cmdList=new ArrayList<String>();
//
	cmdList.add("-vcodec");
	cmdList.add(codecname);
	
	cmdList.add("-i");
	cmdList.add(videoFile);
	
	cmdList.add("-vf");
	cmdList.add(filter);
	
	cmdList.add("-acodec");
	cmdList.add("copy");
	
	cmdList.add("-vcodec");
	cmdList.add("lansoh264_enc");
	
	cmdList.add("-b:v");
	cmdList.add(checkBitRate(bitrate));
	
	cmdList.add("-pix_fmt");  //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
	cmdList.add("yuv420p");
	
	cmdList.add("-y");
	
	cmdList.add(dstFile);
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
}

/**
 *此视频缩放算法，采用是软缩放来实现，速度特慢, 不建议使用.　
 * 视频画面缩放, 务必保持视频的缩放后的宽高比,等于原来视频的宽高比.
 *
 * 【此方法用到编解码】
 *
 * @param videoFile
 * @param scaleWidth
 * @param scaleHeight
 * @param dstFile
 * @param bitrate  <============注意:这里的bitrate在设置的时候, 因为是设置编码器的恒定码率, 推荐设置为 预设值的1.5倍为准, 比如视频原有的码率是1M,则裁剪一半,预设值可能是500k,
 * 这里推荐是为500k的1.5,因为原有的视频大部分是动态码率VBR,可以认为通过{@link MediaInfo} 得到的 {@link MediaInfo#vBitRate}是平均码率,这里要设置,推荐是1.5倍为好.
 * @return
 */
public int executeVideoFrameScale(String videoFile,int scaleWidth,int scaleHeight,String dstFile,int bitrate){
	if(fileExist(videoFile)){
		
		List<String> cmdList=new ArrayList<String>();
		String scalecmd=String.format(Locale.getDefault(),"scale=%d:%d",scaleWidth,scaleHeight);
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_dec");
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-vf");
		cmdList.add(scalecmd);
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-pix_fmt");   //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
		cmdList.add("yuv420p");
		
		cmdList.add("-y");
		
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}

/**
 * 【此方法用到编解码】
 * 缩放的同时增加logo水印.
 * TODO 暂时没有测试.
 * @param videoFile
 * @param pngPath
 * @param scaleWidth
 * @param scaleHeight
 * @param overX
 * @param overY
 * @param dstFile
 * @param bitrate
 * @return
 */
public int executeVideoFrameScale(String videoFile, String pngPath,int scaleWidth,int scaleHeight,int overX,int overY,String dstFile,int bitrate){
	if(fileExist(videoFile)){
		
		List<String> cmdList=new ArrayList<String>();
		String filter=String.format(Locale.getDefault(),"[0:v]scale=%d:%d [scale];[scale][1:v] overlay=%d:%d",
				scaleWidth,scaleHeight,overX,overY);
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_dec");
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-i");
		cmdList.add(pngPath);
		
		cmdList.add("-filter_complex");
		cmdList.add(filter);
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-pix_fmt");   //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
		cmdList.add("yuv420p");
		
		cmdList.add("-y");
		
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}

/**
 *
 * 对视频画面进行裁剪,裁剪后叠加一个png类型的图片,
 *
 * 等于把裁剪,叠加水印,压缩三条命令放在一次执行, 这样只解码一次,和只编码一次,极大的加快了处理速度.
 *
 * @param videoFile 原视频
 * @param decCodec 解码器, 由{@link MediaInfo#vCodecName}填入
 * @param pngPath
 * @param cropX   画面裁剪的X坐标, 左上角为0:0
 * @param cropY    画面裁剪的Y坐标
 * @param cropWidth  画面裁剪宽度. 须小于等于源视频宽度
 * @param cropHeight  画面裁剪高度, 须小于等于源视频高度
 * @param overX   画面和png图片开始叠加的X坐标.
 * @param overY   画面和png图片开始叠加的Y坐标
 * @param dstFile  保存路径.
 * @param bitrate  在视频编码的过程中,调整视频的码率, 如降低码率, 可以压缩的效果,但如果比源画面过于小,则可能出现马赛克, 建议看我们的例子的计算方法.
 * @return
 */
public int executeCropOverlay(String videoFile,String decCodec, String pngPath,int cropX,int cropY,int cropWidth,int cropHeight,int overX,int overY,String dstFile,int bitrate)
{
	//ffmpeg -i test_720p.mp4 -i watermark.png -filter_complex "[0:v]crop=640:640:0:40 [crop];[crop][1:v] overlay=0:0" -acodec copy -y xx.mp4
	if(fileExist(videoFile))
	{
		String filter=String.format(Locale.getDefault(),"[0:v]crop=%d:%d:%d:%d [crop];[crop][1:v] overlay=%d:%d",cropWidth,cropHeight,cropX,cropY,overX,overY);
		int ret=framecropoverlay(videoFile, decCodec, pngPath, filter, dstFile, bitrate);
		if(ret!=0){
			ret=framecropoverlay(videoFile, "h264", pngPath, filter, dstFile, bitrate);
		}
		return ret;
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
//内部使用 【此方法用到编解码】
private int framecropoverlay(String videoFile,String decCodec, String pngPath,String filter,String dstFile,int bitrate)
{
	List<String> cmdList=new ArrayList<String>();
	
	cmdList.add("-vcodec");
	cmdList.add(decCodec);
	
	cmdList.add("-i");
	cmdList.add(videoFile);
	
	cmdList.add("-i");
	cmdList.add(pngPath);
	
	cmdList.add("-filter_complex");
	cmdList.add(filter);
	
	cmdList.add("-acodec");
	cmdList.add("copy");
	
	cmdList.add("-vcodec");
	cmdList.add("lansoh264_enc");
	
	cmdList.add("-b:v");
	cmdList.add(checkBitRate(bitrate));
	
	cmdList.add("-pix_fmt");
	cmdList.add("yuv420p");
	
	cmdList.add("-y");
	
	cmdList.add(dstFile);
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
	
}

public int executeVideoCutOverlay(String videoFile,String decCodec, String pngPath,float startTimeS,float duationS,int overX,int overY,String dstFile,int bitrate)
{
	if(fileExist(videoFile))
	{
		String filter=String.format(Locale.getDefault(),"overlay=%d:%d",overX,overY);
		int ret=videoCutCropOverlay(videoFile, decCodec, pngPath, startTimeS, duationS, filter, dstFile, bitrate);
		if(ret!=0){
			ret= videoCutCropOverlay(videoFile, "h264", pngPath, startTimeS, duationS, filter, dstFile, bitrate);
		}
		return ret;
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}

/**
 * 同时执行 视频时长剪切, 画面裁剪和增加水印的功能.
 * @param videoFile  源视频文件.
 * @param decCodec   源视频解码器
 * @param pngPath   增加的水印文件路径
 * @param startTimeS   时长剪切的开始时间
 * @param duationS   时长剪切的 总长度
 * @param cropX   画面裁剪的 X坐标,(最左边坐标是0)
 * @param cropY  画面裁剪的Y坐标,(最上面坐标是0)
 * @param cropWidth   画面裁剪宽度
 * @param cropHeight  画面裁剪高度
 * @param overX   增加水印的X坐标
 * @param overY   增加水印的Y坐标
 * @param dstFile  目标文件路径
 * @param bitrate   设置在压缩时采用的bitrate.
 * @return
 */
public int executeVideoCutCropOverlay(String videoFile,String decCodec, String pngPath,float startTimeS,float duationS,int cropX,int cropY,int cropWidth,int cropHeight,int overX,int overY,String dstFile,int bitrate)
{
	////ffmpeg -i test_720p.mp4 -i watermark.png -filter_complex "[0:v]crop=640:640:0:40 [crop];[crop][1:v] overlay=0:0" -acodec copy -y xx.mp4
	if(fileExist(videoFile))
	{
		String filter=String.format(Locale.getDefault(),"[0:v]crop=%d:%d:%d:%d [crop];[crop][1:v] overlay=%d:%d",cropWidth,cropHeight,cropX,cropY,overX,overY);
		int ret=videoCutCropOverlay(videoFile, decCodec, pngPath, startTimeS, duationS, filter, dstFile, bitrate);
		if(ret!=0){
			ret= videoCutCropOverlay(videoFile, "h264", pngPath, startTimeS, duationS, filter, dstFile, bitrate);
		}
		return ret;
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
//内部使用  【此方法用到编解码】
private int videoCutCropOverlay(String videoFile,String decCodec, String pngPath,float startTimeS,float duationS,String filter,String dstFile,int bitrate)
{
	List<String> cmdList=new ArrayList<String>();
	cmdList.add("-vcodec");
	cmdList.add(decCodec);
	
	cmdList.add("-ss");
	cmdList.add(String.valueOf(startTimeS));
	
	cmdList.add("-t");
	cmdList.add(String.valueOf(duationS));
	
	cmdList.add("-i");
	cmdList.add(videoFile);
	
	cmdList.add("-i");
	cmdList.add(pngPath);
	
	cmdList.add("-filter_complex");
	cmdList.add(filter);
	
	cmdList.add("-acodec");
	cmdList.add("copy");
	
	cmdList.add("-vcodec");
	cmdList.add("lansoh264_enc");
	
	cmdList.add("-b:v");
	cmdList.add(checkBitRate(bitrate));
	
	cmdList.add("-pix_fmt");   //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
	cmdList.add("yuv420p");
	
	cmdList.add("-y");
	
	cmdList.add(dstFile);
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
}


/**
 * 把多张图片转换为视频
 * 注意：　这里的多张图片必须在同一个文件夹下，并且命名需要有规律,比如名字是 r5r_001.jpeg r5r_002.jpeg, r5r_003.jpeg等
 * 多张图片，需要统一的分辨率，如分辨率不同，则以第一张图片的分辨率为准，后面的分辨率自动缩放到第一张图片的分辨率的大小
 * @param picDir　保存图片的文件夹
 * @param jpgprefix　图片的文件名有规律的前缀
 * @param framerate　每秒钟需要显示几张图片
 * @param dstPath　　处理后保存的路径，需要文件后缀是.mp4
 * @param bitrate  <============注意:这里的bitrate在设置的时候, 因为是设置编码器的恒定码率, 推荐设置为 预设值的1.5倍为准,
 * 因为原有的视频大部分是动态码率VBR,可以认为通过{@link MediaInfo} 得到的 {@link MediaInfo#vBitRate}是平均码率,这里要设置,推荐是1.5倍为好.
 * @return
 */
//./ffmpeg -framerate 1 -i r5r-%03d.jpeg -c:v libx264 -r 25 -pix_fmt yuv420p out33.mp4
public int executeConvertPictureToVideo(String picDir,String jpgprefix,float framerate,String dstPath,int bitrate){
	
	String picSet=picDir+jpgprefix+"_%3d.jpeg";
	
	List<String> cmdList=new ArrayList<String>();
	
	cmdList.add("-framerate");
	cmdList.add(String.valueOf(framerate));
	
	cmdList.add("-i");
	cmdList.add(picSet);
	
	cmdList.add("-c:v");
	cmdList.add("lansoh264_enc");
	
	cmdList.add("-r");
	cmdList.add("25");
	
	cmdList.add("-b:v");
	cmdList.add(checkBitRate(bitrate));
	
	cmdList.add("-pix_fmt"); //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
	cmdList.add("yuv420p");
	
	cmdList.add("-y");
	
	cmdList.add(dstPath);
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
}
/**
 * 为视频增加图片，图片可以是带透明的png类型，也可以是jpg类型;
 * 适用在为视频增加logo，或增加一些好玩的图片的场合，
 * 以下两条方法，也是叠加图片，不同的是可以指定叠加时间段
 * 我们的高级版本可以实现 在任意时刻叠加图片，叠加视频的类，可以实现视频或图片的缩放，移动，旋转等动作
 * @param videoFile　原视频
 * @param imagePngPath　　png图片的路径
 * @param x　　叠加图片相对于视频的Ｘ坐标，视频的左上角为坐标原点0.0
 * @param y　　叠加图片相对于视频的Ｙ坐标
 * @param dstFile　　处理后保存的路径，后缀需要是.mp4格式
 * @param bitrate  <============注意:这里的bitrate在设置的时候, 因为是设置编码器的恒定码率, 推荐设置为 预设值的1.2倍为准, 比如视频原有的码率是1M,则裁剪一半,预设值可能是500k,
 * 这里推荐是为500k的1.5,因为原有的视频大部分是动态码率VBR,可以认为通过{@link MediaInfo} 得到的 {@link MediaInfo#vBitRate}是平均码率,这里要设置,推荐是1.5倍为好.
 *
 * bitrate如果设置低一些, 可以起到压缩视频的效果.
 * @return
 */
public int executeAddWaterMark(String videoFile,String imagePngPath,int x,int y,String dstFile,int bitrate){
	
	if(fileExist(videoFile)){
		
		String filter=String.format(Locale.getDefault(),"overlay=%d:%d",x,y);
		
		int ret=videoAddWatermark(videoFile,"lansoh264_dec",imagePngPath, filter, dstFile, bitrate);
		Log.i(TAG,"executeAddWaterMark  ret =:"+ret);
		if(ret!=0){
			Log.i(TAG,"use soft decoder to add water mark");
			ret=videoAddWatermark(videoFile,"h264",imagePngPath, filter, dstFile, bitrate);
		}
		if(ret!=0){	//如果再不行, 就用软解和软编码来做.
			ret=videoAddWatermarkX264(videoFile, "", imagePngPath, filter, dstFile, bitrate);
		}
		return ret;
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}

/**
 * 为视频增加图片，图片可以是带透明的png类型，也可以是jpg类型;
 * 适用在为视频增加logo，或增加一些好玩的图片的场合，
 * 以下两条方法，也是叠加图片，不同的是可以指定叠加时间段
 * 在某段时间区间内叠加.
 * @param videoFile
 * @param imagePngPath
 * @param startTimeS　　开始时间，单位是秒，类型float，比如从20.8秒处开始
 * @param endTimeS　　　结束时间，单位是秒，类型float 比如在30秒处结束
 * @param x　　叠加图片相对于视频的Ｘ坐标，视频的左上角为坐标原点0.0
 * @param y　　叠加图片相对于视频的Ｙ坐标
 * @param dstFile  处理后保存的路径，后缀需要是mp4格式
 * @param bitrate  <============注意:这里的bitrate在设置的时候, 因为是设置编码器的恒定码率, 推荐设置为 预设值的1.5倍为准, 比如视频原有的码率是1M,则裁剪一半,预设值可能是500k,
 * 这里推荐是为500k的1.5,因为原有的视频大部分是动态码率VBR,可以认为通过{@link MediaInfo} 得到的 {@link MediaInfo#vBitRate}是平均码率,这里要设置,推荐是1.5倍为好.
 * @return
 */
public int executeAddWaterMark(String videoFile,String imagePngPath,float startTimeS,float endTimeS,int x,int y,String dstFile,int bitrate)
{
	if(fileExist(videoFile)){
		
		String filter=String.format(Locale.getDefault(),"overlay=%d:%d:enable='between(t,%f,%f)",x,y,startTimeS,endTimeS);
		
		
		int ret=videoAddWatermark(videoFile,"lansoh264_dec",imagePngPath, filter, dstFile, bitrate);
		if(ret!=0){
			ret=videoAddWatermark(videoFile,"h264",imagePngPath, filter, dstFile, bitrate);
		}
		if(ret!=0){	//如果再不行, 就用软解和软编码来做.
			ret=videoAddWatermarkX264(videoFile, "lansoh264_dec", imagePngPath, filter, dstFile, bitrate);
		}
		return ret;
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
//内部使用, 视频上增加水印. 【此方法用到编解码】
private int videoAddWatermark(String videoFile,String decName,String imagePngPath,String filter,String dstFile,int bitrate)
{
	List<String> cmdList=new ArrayList<String>();
	cmdList.add("-vcodec");
	cmdList.add(decName);
	
	cmdList.add("-i");
	cmdList.add(videoFile);
	
	cmdList.add("-i");
	cmdList.add(imagePngPath);
	
	cmdList.add("-filter_complex");
	cmdList.add(filter);
	
	cmdList.add("-acodec");
	cmdList.add("copy");
	
	cmdList.add("-vcodec");
	cmdList.add("lansoh264_enc");
	
	cmdList.add("-b:v");
	cmdList.add(checkBitRate(bitrate));
	
	cmdList.add("-pix_fmt");   //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
	cmdList.add("yuv420p");
	
	cmdList.add("-y");
	cmdList.add(dstFile);
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
}
//如果是NVIDIA的处理器,则使用软件来做.
private  int videoAddWatermarkX264(String videoFile,String decName,String imagePngPath,String filter,String dstFile,int bitrate)
{
	Log.i(TAG,"is nvidia codec. use x264 to encode data....");
	
	List<String> cmdList=new ArrayList<String>();
	cmdList.add("-vcodec");
	cmdList.add(decName);
	
	cmdList.add("-i");
	cmdList.add(videoFile);
	
	cmdList.add("-i");
	cmdList.add(imagePngPath);
	
	cmdList.add("-filter_complex");
	cmdList.add(filter);
	
	cmdList.add("-acodec");
	cmdList.add("copy");
	
	cmdList.add("-vcodec");
	cmdList.add("libx264");
	
	cmdList.add("-b:v");
	cmdList.add(String.valueOf(bitrate));
	
	
	cmdList.add("-y");
	cmdList.add(dstFile);
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
}
		  /*
			  * 同时叠加多个图片.
			  * ffmpeg -i Text.mp4 -i qzone.png -i qq2.png -i phiz5.png -i send.png -i cancel.png -i download.png  -filter_complex "overlay=25:25,overlay=0:0,overlay=35:35,overlay=45:45,overlay=55:55,overlay=65:65" -pix_fmt yuv420p -c:a copy HeT.mp4
			  * */
/**
 * 【此方法用到编解码】
 * 同时增加两个图片的水印.
 * @param videoFile
 * @param decName
 * @param imagePngPath1
 * @param imagePngPath2
 * @param X1
 * @param Y1
 * @param X2
 * @param Y2
 * @param dstFile
 * @param bitrate
 * @return
 */
public int executeAddWaterMark(String videoFile,String decName,String imagePngPath1,String imagePngPath2,int X1,int Y1,int X2,int Y2,String dstFile,int bitrate){
	
	if(fileExist(videoFile)){
		
		String filter=String.format(Locale.getDefault(),"overlay=%d:%d,overlay=%d:%d",X1,Y1,X2,Y2);
		
		List<String> cmdList=new ArrayList<String>();
		cmdList.add("-vcodec");
		cmdList.add(decName);
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-i");
		cmdList.add(imagePngPath1);
		
		cmdList.add("-i");
		cmdList.add(imagePngPath2);
		
		cmdList.add("-filter_complex");
		cmdList.add(filter);
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-pix_fmt");   //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
		cmdList.add("yuv420p");
		
		cmdList.add("-y");
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}

/**
 * 把视频填充成指定大小的画面, 比视频的宽高大的部分用黑色来填充.
 * 【此方法用到编解码】
 * @param videoFile 源视频路径
 * @param decCodec  视频用到的解码器, 通过MediaInfo得到.
 * @param padWidth  填充成的目标宽度 , 参数需要是16的倍数
 * @param padHeight 填充成的目标高度 , 参数需要是16的倍数
 * @param padX  把视频画面放到填充区时的开始X坐标
 * @param padY  把视频画面放到填充区时的开始Y坐标
 * @param dstFile  目标文件
 * @param bitrate  目标文件的码率, 可以用原视频的MediaInfo.vBitRate的1.2f倍表示即可.
 * @return
 */
public int executePadingVideo(String videoFile,String decCodec,int padWidth,int padHeight,int padX,int padY,String dstFile,int bitrate)
{
	//ffmpeg -i ping20s.mp4 -vf "pad=480:480:50:50:black" -y ping_pad3.mp4
	if(fileExist(videoFile))
	{
		//第一步检测设置填充的高度和宽度是否比原来+坐标的大, 如果小于,则出错.
		MediaInfo info=new MediaInfo(videoFile);
		if(info.prepare()){
			int minWidth=info.vWidth+padX;
			int minHeight=info.vHeight+padY;
			if( minWidth>padWidth || minHeight>padHeight)
			{
				Log.e(TAG,"pad set position is error. min Width>pading width.or min height > padding height");
				return -1;  //失败.
			}
		}else{
			Log.e(TAG,"media info prepare is error!!!");
			return -1;
		}
		
		//第二步: 开始padding.
		String filter=String.format(Locale.getDefault(),"pad=%d:%d:%d:%d:black",padWidth,padHeight,padX,padY);
		
		List<String> cmdList=new ArrayList<String>();
		cmdList.add("-vcodec");
		cmdList.add(decCodec);
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-vf");
		cmdList.add(filter);
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-pix_fmt");   //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
		cmdList.add("yuv420p");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstFile);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 【此方法用到编解码】
 * 精确裁剪视频, 并填充到指定的宽高, 工作过程参考 {@link #executePadingVideo(String, String, int, int, int, int, String, int)}
 * @param videoFile
 * @param decCodec
 * @param startS
 * @param durationS
 * @param padWidth
 * @param padHeight
 * @param padX
 * @param padY
 * @param dstFile
 * @param bitrate
 * @return
 */
public int executeCutPadingVideo(String videoFile,String decCodec,float startS,float durationS,int padWidth,int padHeight,int padX,int padY,String dstFile,int bitrate)
{
	//ffmpeg -i ping20s.mp4 -vf "pad=480:480:50:50:black" -y ping_pad3.mp4
	if(fileExist(videoFile))
	{
		//第一步检测设置填充的高度和宽度是否比原来+坐标的大, 如果小于,则出错.
		MediaInfo info=new MediaInfo(videoFile);
		if(info.prepare()){
			int minWidth=info.vWidth+padX;
			int minHeight=info.vHeight+padY;
			if( minWidth>padWidth || minHeight>padHeight)
			{
				Log.e(TAG,"pad set position is error. min Width>pading width.or min height > padding height");
				return -1;  //失败.
			}
		}else{
			Log.e(TAG,"media info prepare is error!!!");
			return -1;
		}
		
		//第二步: 开始padding.
		String filter=String.format(Locale.getDefault(),"pad=%d:%d:%d:%d:black",padWidth,padHeight,padX,padY);
		
		List<String> cmdList=new ArrayList<String>();
		cmdList.add("-vcodec");
		cmdList.add(decCodec);
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-ss");
		cmdList.add(String.valueOf(startS));
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(durationS));
		
		cmdList.add("-vf");
		cmdList.add(filter);
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-pix_fmt");   //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
		cmdList.add("yuv420p");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstFile);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 【此方法用到编解码】
 *
 * 先裁剪画面的一部分, 然后再填充成一个指定大小的画面, 等于把裁剪后的画面,放到指定大小的画面上, 如小于指定的画面,则用黑色填充.
 *
 * @param videoFile
 * @param decCodec  视频文件的解码器, 用MediaInfo得到
 * @param cropWidth  视频裁剪后的目标宽度
 * @param cropHeight 视频裁剪后的目标高度
 * @param cropX  视频裁剪的X开始位置
 * @param cropY  视频裁剪的Y开始位置
 * @param padWidth  视频裁剪后, 需要填充的目标宽度 , 参数需要是16的倍数
 * @param padHeight 视频裁剪后, 需要填充的目标高度, 参数需要是16的倍数
 * @param padX  把裁剪后的画面放入到padWidth中的开始位置
 * @param padY  把裁剪后的画面放入到padHeight中的开始位置
 * @param dstFile  目标文件
 * @param bitrate  目标文件的码率.可以根据cropWidth和cropHeight来评估(如是480x480或640x480,则建议1000*1000或1100*1000;), 因为虽然padwidth很大,pad填充的是黑色,黑色的码率可以忽略
 * @return
 */
public int executeCropPaddingVideo(String videoFile,String decCodec,int cropWidth,int cropHeight,int cropX,int cropY,
								   int padWidth,int padHeight,int padX,int padY,String dstFile,int bitrate)
{
	//ffmpeg -i ping20s.mp4 -vf "crop=300:300:0:0,pad=480:480:50:50:black" -y ping_pad3.mp4
	if(fileExist(videoFile))
	{
		//这里没有检测裁剪的坐标是否有效. 注意!!!
		
		//第一步检测设置填充的高度和宽度是否比原来+坐标的大, 如果小于,则出错.
		int minWidth=cropWidth+padX;
		int minHeight=cropHeight+padY;
		if( minWidth>padWidth || minHeight>padHeight)
		{
			Log.e(TAG,"pad set position is error. min Width>pading width.or min height > padding height");
			return -1;  //失败.
		}
		
		//第二步: 开始padding.
		String filter=String.format(Locale.getDefault(),"crop=%d:%d:%d:%d,pad=%d:%d:%d:%d:black",cropWidth,cropHeight,cropX,cropY,padWidth,padHeight,padX,padY);
		
		List<String> cmdList=new ArrayList<String>();
		cmdList.add("-vcodec");
		cmdList.add(decCodec);
		
		cmdList.add("-i");
		cmdList.add(videoFile);
		
		cmdList.add("-vf");
		cmdList.add(filter);
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-pix_fmt");   //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
		cmdList.add("yuv420p");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstFile);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 【此方法用到编解码】
 * 给视频旋转角度,注意这里 只是 旋转画面的的角度,而不会调整视频的宽高.
 * @param srcPath　需要旋转角度的原视频
 * @param decoder　　视频的解码器名字
 * @param angle　　角度
 * @param dstPath　　处理后的视频存放的路径,后缀需要是.mp4
 * @return
 */
public int executeRotateAngle(String srcPath,String decoder,float angle,int bitrate,String dstPath)
{
	if(fileExist(srcPath)){
		
		String filter=String.format(Locale.getDefault(),"rotate=%f*(PI/180),format=yuv420p",angle);
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-vf");
		cmdList.add(filter);
		
		cmdList.add("-metadata:s:v");
		cmdList.add("rotate=0");
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 *
 * 把拍摄的有角度值的视频, 矫正成没有角度的视频,如果在您代码流程中会用到另外设置bitrate需要编码的地方，则不需要调用这里，因为另外设置bitrate的方法会自动校正原视频的角度。
 *
 *
 * 如原来视频有90度或270度, 这样在有些播放器中, 会出现视频是横着播放的, 这是因为播放器没有检测视频角度; 为了兼容这样的播放器,需要把视频矫正成没有角度的并且画面正常显示的视频.
 * 此方法仅适用在单单需要校正角度，而不需要另外的编码操作，如有另外的编码操作， 则无需适用这个方法。
 *
 * 【此方法用到编解码】
 * @param srcPath 原视频.
 * @param decoder 原视频的解码器
 * @param bitrate  原视频的码率的1.5f, 即info.vBitRate的1.5f
 * @param dstPath  目标文件路径.
 * @return
 */
public int executeVideoZeroAngle(String srcPath,String decoder,int bitrate,String dstPath)
{
	if(fileExist(srcPath)){
		
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 设置多媒体文件中的 视频元数据的角度.
 * 一个多媒体文件中有很多种元数据, 包括音频轨道, 视频轨道, 各种元数据, 字幕,其他文字等信息,这里仅仅更改元数据中的视频播放角度, 当视频播放器播放该视频时, 会得到"要旋转多少度"播放的信息, 这样在播放时就会旋转后再播放画面
 *
 *  此设置不改变音视频的各种参数, 仅仅是告诉播放器,"要旋转多少度"来播放而已.
 *  适用在拍摄的视频有90度和270的情况, 想更改这个角度参数的场合.
 * @param srcPath  原视频
 * @param angle  需要更改的角度
 * @param dstPath  目标视频路径
 * @return
 */
public int executeSetVideoMetaAngle(String srcPath,int angle,String dstPath)
{
	//ffmpeg -i input.mp4 -c copy -metadata:s:v:0 rotate=90 output.mp4
	if(fileExist(srcPath)){
		
		List<String> cmdList=new ArrayList<String>();
		
		String filter=String.format(Locale.getDefault(),"rotate=%d",angle);
		
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-c");
		cmdList.add("copy");
		
		cmdList.add("-metadata:s:v:0");
		cmdList.add(filter);
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
//---------2016年9月19日16:31:35 测试增加:
private static boolean isNvidiaCodec()
{
	boolean contain=false;
//			 int numCodecs = MediaCodecList.getCodecCount();
//
//	            for (int i = 0; i < numCodecs; i++)
//	            {
//	                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
//
//	                if (codecInfo.isEncoder())
//	                    continue;
//
//	                String[] types = codecInfo.getSupportedTypes();
//	                if (types == null)
//	                    continue;
//
//	                if(codecInfo.getName().contains("OMX.Nvidia.h264"))
//	                	contain=true;
//
////	                for(String type: types)
////	                	Log.i(TAG,"is---"+codecInfo.getName()+ " types is"+ type);  //type="video/avc"
//	            }
	return contain;
}


//---------------------------
/**
 * 调整视频的播放速度，　可以把视频加快速度，或放慢速度。适用在希望缩短视频中不重要的部分的场景，比如走路等
 * 【此方法用到编解码】
 * @param srcPath　　源视频
 * @param decoder　　指定视频的解码器名字
 * @param speed　　　　源视频中　　画面和音频同时改变的倍数，比如放慢一倍，则这里是0.5;加快一倍，这里是2；建议速度在0.5--2.0之间。
 * @param dstPath　　处理后的视频存放路径，后缀需要是.mp4
 * @return
 */
public int  executeVideoAdjustSpeed( String srcPath,String decoder,float speed,int bitrate,String dstPath)
{
	
	if(fileExist(srcPath)){
		
		String filter=String.format(Locale.getDefault(),"[0:v]setpts=%f*PTS[v];[0:a]atempo=%f[a]",1/speed,speed);
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-filter_complex");
		cmdList.add(filter);
		
		cmdList.add("-map");
		cmdList.add("[v]");
		cmdList.add("-map");
		cmdList.add("[a]");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 调整视频的播放速度，　可以把视频加快速度，或放慢速度。适用在希望缩短视频中不重要的部分的场景，比如走路等
 * 【此方法用到编解码】
 *
 * 和#executeVideoAdjustSpeed 不同的是, 这方法仅仅是适用于没有音频的场合, 如果您视频中没有音频,可以用这个来做.
 * (用Mediainfo可以检测出是否有音频部分)
 * @param srcPath　　源视频
 * @param decoder　　指定视频的解码器名字
 * @param speed　　　　源视频中　　画面和音频同时改变的倍数，比如放慢一倍，则这里是0.5;加快一倍，这里是2；建议速度在0.5--2.0之间。
 * @param dstPath　　处理后的视频存放路径，后缀需要是.mp4
 * @return
 */
public int  executeVideoAdjustSpeed2( String srcPath,String decoder,float speed,int bitrate,String dstPath)
{
	if(fileExist(srcPath)){
		
		String filter=String.format(Locale.getDefault(),"[0:v]setpts=%f*PTS[v]",1/speed);
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-filter_complex");
		cmdList.add(filter);
		
		cmdList.add("-map");
		cmdList.add("[v]");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 视频水平镜像，即把视频左半部分镜像显示在右半部分
 * 【此方法用到编解码】
 * @param srcPath　源视频路径
 * @param decoder　　指定解码器
 * @param dstPath　　目标视频路径
 * @return
 */
public int  executeVideoMirrorH( String srcPath,String decoder,int bitrate,String dstPath)
{
	//ffmpeg -i 2x.mp4 -vf "crop=iw/2:ih:0:0,split[left][tmp];[tmp]hflip[right];[left][right] hstack" -acodec copy 2x_hmirror.mp4
	if(fileExist(srcPath)){
		
		String filter=String.format(Locale.getDefault(),"crop=iw/2:ih:0:0,split[left][tmp];[tmp]hflip[right];[left][right] hstack");
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-vf");
		cmdList.add(filter);
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 视频垂直镜像，即把视频上半部分镜像显示在下半部分
 * 【此方法用到编解码】
 * @param srcPath　源视频路径
 * @param decoder　　指定解码器
 * @param dstPath　　目标视频路径
 * @return
 */
public int  executeVideoMirrorV( String srcPath,String decoder,int bitrate,String dstPath)
{
	//ffmpeg -i 2x.mp4 -vf "crop=iw/2:ih:0:0,split[left][tmp];[tmp]hflip[right];[left][right] hstack" -acodec copy 2x_hmirror.mp4
	if(fileExist(srcPath)){
		
		String filter=String.format(Locale.getDefault(),"crop=iw:ih/2:0:0,split[top][tmp];[tmp]vflip[bottom];[top][bottom] vstack");
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-vf");
		cmdList.add(filter);
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 视频垂直方向反转
 * 【此方法用到编解码】
 * @param srcPath1　　原视频
 * @param decoder　　视频的解码器名字
 * @param dstPath　　目标视频　需要是mp4格式。
 * @return
 */
public int executeVideoRotateVertically( String srcPath,String decoder,int bitrate,String dstPath)
{
	//./ffmpeg -i input.mp4 -vf vflip -c:a copy output_v.mp4  //<----垂直方向上反转
	if(fileExist(srcPath)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-vf");
		cmdList.add("vflip");
		
		cmdList.add("-c:a");
		cmdList.add("copy");
		
		cmdList.add("-c:v");
		cmdList.add("lansoh264_enc");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 视频水平方向反转
 * 【此方法用到编解码】
 * @param srcPath1　　原视频
 * @param decoder　　视频的解码器名字
 * @param dstPath　　目标视频. 需要是mp4格式
 * @return
 */
public int executeVideoRotateHorizontally( String srcPath,String decoder,int bitrate,String dstPath)
{
	//./ffmpeg -i input.mp4 -vf hflip -c:a copy output_v.mp4  //<----水平方向上反转
	if(fileExist(srcPath)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-vf");
		cmdList.add("hflip");
		
		cmdList.add("-c:a");
		cmdList.add("copy");
		
		cmdList.add("-c:v");
		cmdList.add("lansoh264_enc");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 视频顺时针旋转90度
 * @param srcPath 原视频
 * @param decoder 视频的解码器名字
 * @param bitrate 希望目标视频的码率. 一般为原视频码率的1.2f为好.
 * @param dstPath 目标视频.需要是mp4格式
 * @return
 */
public int executeVideoRotate90Clockwise( String srcPath,String decoder,int bitrate,String dstPath)
{
	//ffmpeg -i INPUT -vf transpose=1 -c:a copy output.mp4  //<----顺时针旋转视频90度
	if(fileExist(srcPath)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-vf");
		cmdList.add("transpose=1");
		
		cmdList.add("-c:a");
		cmdList.add("copy");
		
		cmdList.add("-c:v");
		cmdList.add("lansoh264_enc");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 *
 * 视频逆时针旋转90度,也可以认为是顺时针旋转270度.
 * 【此方法用到编解码】
 * @param srcPath1　原视频
 * @param decoder　　视频的解码器名字
 * @param dstPath　　目标视频，需要是mp4格式
 * @return
 */
public int executeVideoRotate90CounterClockwise( String srcPath,String decoder,int bitrate,String dstPath)
{
	if(fileExist(srcPath)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-vf");
		cmdList.add("transpose=2");
		
		cmdList.add("-c:a");
		cmdList.add("copy");
		
		cmdList.add("-c:v");
		cmdList.add("lansoh264_enc");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 视频倒序；比如正常的视频画面是一个人从左边走到右边，倒序后，人从右边倒退到左边，即视频画面发生了倒序
 *
 * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
 * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
 *
 * 如您的视频过大, 则可能导致:Failed to inject frame into filter network: Out of memory;这个是正常的.因为已超过APP可使用的内容范围, 内存不足.
 *
 * @param srcPath1　原视频
 * @param decoder　　解码器的名字
 * @param dstPath　　目标视频，需要是mp4格式
 * @return
 */
public int executeVideoReverse( String srcPath,String decoder,int bitrate,String dstPath)
{
	if(fileExist(srcPath)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-vf");
		cmdList.add("reverse");
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-c:v");
		cmdList.add("lansoh264_enc");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}

/**
 * 音频倒序，和视频倒序类似，把原来正常的声音，处理成从后向前的声音。　适合在搞怪的一些场合。
 *
 * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
 * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
 *
 * @param srcPath  源文件完整路径. 原文件可以是mp4的视频, 也可以是mp3或m4a的音频文件
 * @param dstPath　目标文件完整路径. 需要输出路径名 和原文件的路径名 一致.
 * @return
 */
public int executeAudioReverse( String srcPath,String dstPath)
{
	if(fileExist(srcPath)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-af");
		cmdList.add("areverse");
		
		cmdList.add("-c:v");
		cmdList.add("copy");
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 把一个mp4文件中的音频部分和视频都倒序播放。
 *
 * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
 * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
 *
 * 【此方法用到编解码】
 *
 * 如您的视频过大, 则可能导致:Failed to inject frame into filter network: Out of memory;这个是正常的.因为已超过APP可使用的内容范围, 内存不足.
 *
 * @param srcPath1　　原mp4文件
 * @param decoder　　mp4文件中的视频解码器名字
 * @param dstPath　　目标mp4文件存放路径
 * @return
 */
public int executeAVReverse( String srcPath,String decoder,int bitrate,String dstPath)
{
	//ffmpeg -i 2x.mp4 -vf reverse -af areverse reversed.mp4
	if(fileExist(srcPath)){
		int ret=0;
		ret=doAVReverse(srcPath, decoder, bitrate, dstPath, true);
		if(ret!=0){
			Log.w(TAG,"executeAVReverse use hardware encoder is error,switch to software encoder");
			ret=doAVReverse(srcPath, decoder, bitrate, dstPath, false);
		}
		return ret;
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
private int doAVReverse( String srcPath,String decoder,int bitrate,String dstPath,boolean isHW)
{
	List<String> cmdList=new ArrayList<String>();
	
	cmdList.add("-vcodec");
	cmdList.add(decoder);
	
	cmdList.add("-i");
	cmdList.add(srcPath);
	
	cmdList.add("-vf");
	cmdList.add("reverse");
	
	cmdList.add("-af");
	cmdList.add("areverse");
	
	
	cmdList.add("-c:v");
	if(isHW){
		cmdList.add("lansoh264_enc");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
	}else{
		cmdList.add("libx264");
	}
	cmdList.add("-b:v");
	cmdList.add(checkBitRate(bitrate));
	
	cmdList.add("-y");
	cmdList.add(dstPath);
	
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
}
/**
 * 把mp4的视频, 转换为yuv格式 转换后, yuv的格式是YUV420P
 * @param srcPath  输入的 input.mp4文件
 * @param decodeName  输入的解码器, 由MediaInfo获取.
 * @param dstPath   得到的yuv数据, 后缀请务必使.yuv
 * @return
 */
public int executeDecodeVideoToYUV( String srcPath,String decodeName,String dstPath)
{
	if(fileExist(srcPath)){
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decodeName);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		
		cmdList.add("-f");
		cmdList.add("rawvideo");
		
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 把yuv格式的视频, 转换为MP4,并在转换过程中,增加图片叠加
 *
 * 注意:这里的yuv格式是YUV402P(如果是NV21或NV12需要转换下)
 *
 * 【此方法用到编解码】
 * @param yuvPath  yuv的路径
 * @param width  yuv的宽度
 * @param height yuv的高度
 * @param imagePngPath  图像路径
 * @param x  叠加的x开始坐标 左上角为0,0
 * @param y
 * @param dstFile  目标文件
 * @param bitrate  视频编码时的码率
 * @return
 */
public int executeYUVAddWaterMark(String yuvPath,int width,int height,String imagePngPath,int x,int y,String dstFile,int bitrate){
	
	if(fileExist(yuvPath)){
		
		String filter=String.format(Locale.getDefault(),"overlay=%d:%d",x,y);
		
		
		List<String> cmdList=new ArrayList<String>();
		
		String size=String.valueOf(width);
		size+="x";
		size+=String.valueOf(height);
		
		cmdList.add("-f");
		cmdList.add("rawvideo");
		
		cmdList.add("-video_size");
		cmdList.add(size);
		
		cmdList.add("-i");
		cmdList.add(yuvPath);
		
		
		
		cmdList.add("-i");
		cmdList.add(imagePngPath);
		
		cmdList.add("-filter_complex");
		cmdList.add(filter);
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-pix_fmt");   //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
		cmdList.add("yuv420p");
		
		cmdList.add("-y");
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * TODO  没有验证, 只是在PC端测试OK
 *
 * 把yuv420p格式的yuv视频文件, 编码成MP4
 * 【此方法用到编解码】
 *
 * @param srcPath  原文件
 * @param width  yuv视频的宽度
 * @param height 高度
 * @param bitrate 码率
 * @param dstPath 目标文件
 * @return
 */
public int executeEncodeYUV2MP4( String srcPath,int width,int height,int bitrate,String dstPath)
{
	//ffmpeg -f rawvideo -video_size 1920x1080 -i xs.yuv -vcodec libx264 xs1.mp4
	if(fileExist(srcPath)){
		
		List<String> cmdList=new ArrayList<String>();
		
		String size=String.valueOf(width);
		size+="x";
		size+=String.valueOf(height);
		
		cmdList.add("-f");
		cmdList.add("rawvideo");
		
		cmdList.add("-video_size");
		cmdList.add(size);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		
		cmdList.add("-c:v");
		cmdList.add("lansoh264_enc");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}

/**
 * 此方法仅仅是为了客户的需求,而临时性测试, 不建议使用, 仅供客户参考..请注意.
 * 【此方法用到编解码】
 * @param videoPath  视频路径
 * @param decoder  视频的解码器
 * @param subtilePath  字幕的路径
 * @param bitrate  目标文件的编码码率
 * @param dstPath  目标文件的路径.
 * @return
 */
public int testAddSubtitle2Video(String videoPath,String decoder,String subtilePath,int bitrate,String dstPath)
{
	//参考代码://ffmpeg -i 2x.mp4 -vf "subtitles=tenSub.srt" -y out3.mp4
	List<String> cmdList=new ArrayList<String>();
	
	String filter="subtitles=";
	filter+=subtilePath;
	
	cmdList.add("-vcodec");
	cmdList.add(decoder);
	
	cmdList.add("-i");
	cmdList.add(videoPath);
	
	cmdList.add("-vf");
	cmdList.add(filter);
	
	cmdList.add("-acodec");
	cmdList.add("copy");
	
	cmdList.add("-c:v");
	cmdList.add("lansoh264_enc");
	cmdList.add("-pix_fmt");
	cmdList.add("yuv420p");
	cmdList.add("-b:v");
	cmdList.add(checkBitRate(bitrate));
	
	cmdList.add("-y");
	cmdList.add(dstPath);
	
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
}

// -vf drawtext="fontfile=/usr/share/fonts/truetype/freefont/FreeSerif.ttf: text='Test Text'"
public int testVideoAddText(String videoPath,String decoder,int bitrate,String dstPath)
{
	//参考代码://ffmpeg -i 2x.mp4 -vf "subtitles=tenSub.srt" -y out3.mp4
	List<String> cmdList=new ArrayList<String>();
	
	cmdList.add("-vcodec");
	cmdList.add(decoder);
	
	cmdList.add("-i");
	cmdList.add(videoPath);
	
	cmdList.add("-vf");
	cmdList.add("drawtext=fontfile=/system/fonts/DroidSansFallback.ttf: text='杭州蓝松科技001abc'");
	
	cmdList.add("-acodec");
	cmdList.add("copy");
	
	cmdList.add("-c:v");
	cmdList.add("lansoh264_enc");
	cmdList.add("-pix_fmt");
	cmdList.add("yuv420p");
	cmdList.add("-b:v");
	cmdList.add(checkBitRate(bitrate));
	
	cmdList.add("-y");
	cmdList.add(dstPath);
	
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
}

/**
 * 把yuv的视频文件, 增加图片上去, 这里仅仅是增加图片,转换视频部分, 没有音频部分, 您如果需要音频部分,需要另外merge
 * 【此方法用到编解码】
 *  为客户测试使用.
 * @param yuvPath
 * @param width.
 * @param height
 * @param imagePngPath
 * @param x
 * @param y
 * @param dstFile
 * @param bitrate
 * @return
 */
public int executeYuvAddWaterMark(String yuvPath,int width,int height,String imagePngPath,int x,int y,String dstFile,int bitrate){
	//ffmpeg -f rawvideo -video_size 1280x720 -pix_fmt nv21 -i cachevideo.yuv vv.mp4
	if(fileExist(yuvPath)){
		
		String filter=String.format(Locale.getDefault(),"overlay=%d:%d",x,y);
		String size=String.format(Locale.getDefault(),"%dx%d",width,height);
		
		List<String> cmdList=new ArrayList<String>();
		cmdList.add("-f");
		cmdList.add("rawvideo");
		
		cmdList.add("-video_size");
		cmdList.add(size);
		
		cmdList.add("-pix_fmt");
		cmdList.add("nv21");
		
		cmdList.add("-i");
		cmdList.add(yuvPath);
		
		cmdList.add("-i");
		cmdList.add(imagePngPath);
		
		cmdList.add("-filter_complex");
		cmdList.add(filter);
		
		cmdList.add("-acodec");
		cmdList.add("copy");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-pix_fmt");   //<========请注意, 使用lansoh264_enc编码器编码的时候,请务必指定格式,因为底层设计只支持yuv420p的输出.
		cmdList.add("yuv420p");
		
		cmdList.add("-y");
		cmdList.add(dstFile);
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}

//		/**
//		 * 仅仅测试gif的编码。
//		 * @return
//		 */
//		public int executeImage2Gif()
//		{
//			//ffmpeg -f image2 -framerate 10 -i gif_%03d.jpg neat.gif 参考代码。
//					List<String> cmdList=new ArrayList<String>();
//
//					cmdList.add("-f");
//					cmdList.add("image2");
//
//					cmdList.add("-framerate");
//					cmdList.add("10");
//
//					cmdList.add("-i");
//					cmdList.add("/sdcard/test_gif/gif_%03d.jpg");
//
//
//					cmdList.add("-y");
//					cmdList.add("/sdcard/test_gif/m7_gif.gif");
//
//					String[] command=new String[cmdList.size()];
//				     for(int i=0;i<cmdList.size();i++){
//				    	 command[i]=(String)cmdList.get(i);
//				     }
//				    return  executeVideoEditor(command);
//		}

/**
 * 校对一下 bitrate, 因为一些2013年左右的SoC中的硬件编码器如果码率大于2000*1000(2M)的话, 则会崩溃, 故这里限制在2M范围内.
 * @param srcBitRate  源码率
 * @return  矫正后的码率
 */
public static String checkBitRate(int srcBitRate)
{
//			int bitrate=srcBitRate;
//
//			if(bitrate>2500*1000)
//	    		bitrate=2500*1000; //2.5M
//			else if(bitrate<500)
//				bitrate=500;
//
	return String.valueOf(srcBitRate);
}

/**
 * 临时测试
 * @param tsFile
 * @param dstFile
 * @return
 */
public int executeTsTextToMp4(String tsFile,String dstFile)
{
	
	
	List<String> cmdList=new ArrayList<String>();
	
	cmdList.add("-f");
	cmdList.add("concat");
	
	cmdList.add("-i");
	cmdList.add(tsFile);
	
	cmdList.add("-c");
	cmdList.add("copy");
	
	cmdList.add("-bsf:a");
	cmdList.add("aac_adtstoasc");
	
	cmdList.add("-y");
	
	cmdList.add(dstFile);
	String[] command=new String[cmdList.size()];
	for(int i=0;i<cmdList.size();i++){
		command[i]=(String)cmdList.get(i);
	}
	return  executeVideoEditor(command);
	
}
/**
 * 【此方法用到编解码】
 * @param srcPath
 * @param decoder
 * @param pngPath
 * @param xpos
 * @param ypos
 * @param speed
 * @param bitrate
 * @param dstPath
 * @return
 */
public int  executeAddMarkAdjustSpeed( String srcPath,String decoder,String  pngPath,int xpos,int ypos, float speed,int bitrate,String dstPath)
{
//ffmpeg -i 2x.mp4 -i watermark.png -filter_complex "[0:v][1:v] overlay=0:0[overlay]; [overlay]setpts=0.5*PTS[v];[0:a]atempo=2.0[a]" -map "[v]" -map "[a]" output3.mp4
	
	if(fileExist(srcPath)){
		
		String filter=String.format(Locale.getDefault(),"[0:v][1:v] overlay=%d:%d[overlay]; [overlay]setpts=%f*PTS[v];[0:a]atempo=%f[a]",xpos,ypos,1/speed,speed);
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-i");
		cmdList.add(pngPath);
		
		cmdList.add("-filter_complex");
		cmdList.add(filter);
		
		cmdList.add("-map");
		cmdList.add("[v]");
		cmdList.add("-map");
		cmdList.add("[a]");

//						cmdList.add("-acodec");  //音频采用默认编码.
//						cmdList.add("copy");
		
		cmdList.add("-vcodec");
		cmdList.add("lansoh264_enc");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
/**
 * 【此方法用到编解码】
 * @param srcPath
 * @param decoder
 * @param pngPath
 * @param xpos
 * @param ypos
 * @param speed
 * @param bitrate
 * @param dstPath
 * @return
 */
public int  executeAddMarkAdjustSpeed2( String srcPath,String decoder,String  pngPath,int xpos,int ypos, float speed,int bitrate,String dstPath)
{
//ffmpeg -i 2x.mp4 -i watermark.png -filter_complex "[0:v][1:v] overlay=0:0[overlay]; [overlay]setpts=0.5*PTS[v];[0:a]atempo=2.0[a]" -map "[v]" -map "[a]" output3.mp4
	
	if(fileExist(srcPath)){
		
		String filter=String.format(Locale.getDefault(),"[0:v][1:v] overlay=%d:%d[overlay]; [overlay]setpts=%f*PTS[v];[0:a]atempo=%f[a]",xpos,ypos,1/speed,speed);
		
		List<String> cmdList=new ArrayList<String>();
		
		cmdList.add("-vcodec");
		cmdList.add(decoder);
		
		cmdList.add("-i");
		cmdList.add(srcPath);
		
		cmdList.add("-i");
		cmdList.add(pngPath);
		
		cmdList.add("-filter_complex");
		cmdList.add(filter);
		
		cmdList.add("-map");
		cmdList.add("[v]");
		cmdList.add("-map");
		cmdList.add("[a]");

//						cmdList.add("-acodec");  //音频采用默认编码.
//						cmdList.add("copy");
		
		cmdList.add("-vcodec");
		cmdList.add("libx264");
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(bitrate));
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		
		String[] command=new String[cmdList.size()];
		for(int i=0;i<cmdList.size();i++){
			command[i]=(String)cmdList.get(i);
		}
		return  executeVideoEditor(command);
		
	}else{
		return VIDEO_EDITOR_EXECUTE_FAILED;
	}
}
//------------------------------------------------
private boolean isCheckBitRate=true;

private boolean isCheckPadSize=true;
/**
 * 是否在开始运行DrawPad的时候,检查您设置的码率和分辨率是否正常.
 *
 * 默认是检查, 如果您清楚码率大小的设置,请调用此方法,不再检查.
 *
 */
public void setNotCheckBitRate()
{
	isCheckBitRate=false;
}
/**
 * 是否在开始运行DrawPad的时候, 检查您设置的DrawPad宽高是否是16的倍数.
 * 默认是检查.
 */
public void setNotCheckDrawPadSize()
{
	isCheckPadSize=false;
}
/**
 * 当数据不是16的倍数的时候, 把他调整成16的倍数,
 *
 * 如果是18,19这样接近16,则等于16, 等于缩小了原有的画面,
 * 如果是25,28这样接近32,则等于32,  等于稍微拉伸了原来的画面,
 * 因为最多缩小或拉伸8个像素, 还不至于画面严重变形,而又兼容编码器的要求,故可以这样做.
 *
 * 16, 17, 18, 19,20,21,22,23 ==>16;
 * 24,25,26,27,28,29,30,31,32==>32;
 * @param value
 * @return
 */
private static int make16Multi(int value)
{
	
	if(value<16){
		return value;
	}else {
		value+=8;
		int  val2= value/16;
		val2*=16;
		return val2;
	}
	
}
/**
 *  获取lansosdk的建议码率;
 *  这个码率不是唯一的, 仅仅是我们建议这样设置, 如果您对码率理解很清楚或有一定的压缩要求,则完全可以不用我们的建议,自行设置.
 *
 * @param wxh  宽度和高度的乘积;
 * @return
 */
public static int getSuggestBitRate(int wxh)
{
	if(wxh <= 480 * 480){
		return 1000*1024;
	}else if(wxh<=640 * 480){
		return 1500*1024;
	}else if(wxh <=800 *480){
		return 1800*1024;
	}else if(wxh <=960 * 544){
		return 2000*1024;
	}else if(wxh <=1280 * 720){
		return 2500*1024;
	}else if(wxh<=1920 * 1088){
		return 3000*1024;
	}else{
		return 3500*1024;
	}
}
public static int checkSuggestBitRate(int wxh, int bitrate)
{
	int sugg=getSuggestBitRate(wxh);
	return bitrate < sugg ?  sugg: bitrate;   //如果设置过来的码率小于建议码率,则返回建议码率,不然返回设置码率
}
	
}
