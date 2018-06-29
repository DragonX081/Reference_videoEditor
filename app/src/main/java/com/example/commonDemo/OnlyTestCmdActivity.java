package com.example.commonDemo;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.lansoeditor.demo.R;
import com.lansosdk.videoeditor.CopyDefaultVideoAsyncTask;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;
import com.lansosdk.videoeditor.onVideoEditorProgressListener;

import java.io.IOException;

/**
 * 蓝松科技开发人员测试界面.
 *
 */
public class OnlyTestCmdActivity extends Activity{


	private final static String TAG="OnlyTestActivity";
	private final static  boolean VERBOSE = false;   
	
	String videoPath=null;
	VideoEditor mEditor = new VideoEditor();
	ProgressDialog  mProgressDialog;
	int videoDuration;
	MediaInfo mInfo;
	private String dstMP4="/sdcard/04.mp4";
	private String dstAAC="/sdcard/01.aac";
	
	
	private boolean isRunning=false; 
	private boolean isTestAudio=false;  //是否是测试音频
	  @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
			 setContentView(R.layout.video_edit_demo_layout);
			
				
			 TextView tvHint=(TextView)findViewById(R.id.id_video_editor_demo_hint);
			 tvHint.setText(R.string.video_editor_demo_hint);
			 
			 
			 findViewById(R.id.id_video_edit_testaudio).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					isTestAudio=true;
					
					CopyDefaultVideoAsyncTask.copyFile(getApplicationContext(), "niusanjin.mp3");
					
					CopyDefaultVideoAsyncTask.copyFile(getApplicationContext(),"aac20s.aac");
					
					startVideoEditorTask();
				}
			});
			 
	        findViewById(R.id.id_video_edit_btn).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					//videoPath= CopyDefaultVideoAsyncTask.copyFile(getApplicationContext(),"ping20s.mp4");
					
					
//					 mInfo=new MediaInfo(videoPath,true);
//					 mInfo.prepare();
//					 Log.i(TAG,"info true:"+mInfo.toString());
					 
					 isTestAudio=false;
					 startVideoEditorTask();
					
					
					//------------------
					
				}
			});
	        
	        findViewById(R.id.id_video_play_btn).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(isTestAudio){
						playAudio(dstAAC);
					}else{
						if(SDKFileUtils.fileExist(dstMP4)){
					    	Intent intent=new Intent(OnlyTestCmdActivity.this,VideoPlayerActivity.class);
					    	intent.putExtra("videopath", dstMP4);
					    	startActivity(intent);
						}else{
							Toast.makeText(OnlyTestCmdActivity.this, R.string.file_not_exist,Toast.LENGTH_SHORT).show();
						}
					}
				}
			});
	        
	        mEditor.setOnProgessListener(new onVideoEditorProgressListener() {
				
				@Override
				public void onProgress(VideoEditor v, int percent) {
					// TODO Auto-generated method stub
					Log.i(TAG,"current percent is:"+percent);
					if(mProgressDialog!=null)
						mProgressDialog.setMessage("正在处理中..."+String.valueOf(percent)+"%");
				}
			});
	        
	        mEditor.setOnProgessListener(new onVideoEditorProgressListener() {
				
				@Override
				public void onProgress(VideoEditor v, int percent) {
					// TODO Auto-generated method stub
					Log.i(TAG,"current percent is:"+percent);
					if(mProgressDialog!=null)
						mProgressDialog.setMessage("正在处理中..."+String.valueOf(percent)+"%");
				}
			});
	        isTestAudio=false;
	        
	        //删除之前的, 保证这个是唯一的.
	        if(SDKFileUtils.fileExist(dstMP4)){
	        	SDKFileUtils.deleteFile(dstMP4);
	        }
	  } 
	  @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
//        findViewById(R.id.id_video_play_btn).setEnabled(false);
	}
	  @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
		private void showHintDialog()
		{
			new AlertDialog.Builder(this)
			.setTitle("提示")
			.setMessage("视频过大,可能会需要一段时间,您确定要处理吗?")
	        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					startVideoEditorTask();
				}
			})
			.setNegativeButton("取消", null)
	        .show();
		}
		private void startVideoEditorTask()
		{
			if(isRunning)
				return;
			
			new SubAsyncTask().execute();
		}
		
		MediaPlayer audioPlayer=null;
		public void playAudio(String audioFile){
			
			if(MediaInfo.isSupport(audioFile) && audioPlayer ==null)
			{
				audioPlayer=new MediaPlayer();
				try {
					audioPlayer.setDataSource(audioFile);
					audioPlayer.prepare();
					audioPlayer.start();
					
					audioPlayer.setOnCompletionListener(new OnCompletionListener() {
						
						@Override
						public void onCompletion(MediaPlayer mp) {
							// TODO Auto-generated method stub
							audioPlayer.release();
							audioPlayer=null;
						}
					});
					
				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	    }
		
	  public class SubAsyncTask extends AsyncTask<Object, Object, Boolean>{
			  @Override
			protected void onPreExecute() {
			// TODO Auto-generated method stub
				  mProgressDialog = new ProgressDialog(OnlyTestCmdActivity.this);
		          mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		          mProgressDialog.setMessage("正在处理中...");
		          mProgressDialog.setCancelable(false);
		          mProgressDialog.show();
		          isRunning=true;
		          super.onPreExecute();
			}
      	    @Override
      	    protected synchronized Boolean doInBackground(Object... params) {
      	    	// TODO Auto-generated method stub
      	    	//这里完全是我们测试VideoEditor中的方法时使用的,为了方便高效的测试, 我们在这里直接调用每个方法来做,请知悉.
      	    	
//      	    	mEditor.executeDeleteAudio("/sdcard/2x.mp4", "/sdcard/2x_noaudio.mp4");
//      	    	
//      	    	mEditor.executeVideoMergeAudio("/sdcard/2x_noaudio.mp4","/sdcard/encoderAA.aac","/sdcard/Add_aac3.mp4",0,5.38f);
      	    	
//      	    	mEditor.executeVideoSalceAndCrop("/sdcard/test_720p.mp4", "/sdcard/testNNN.mp4", 0, 0, 0, 0, 10, "lansoh264_enc");
      	    	
//      	    	if(mMediaInfo!=null){
      	    	//这里是要做音频编码格式的检测,以方便快速的提取,直接拷贝,不用解码后再编码.
//      	    		if(mMediaInfo.aCodecName.equals("mp3")){
//      	    			mEditor.executeDeleteVideo("/sdcard/2x.mp4","/sdcard/2x_nov.mp3");
//      	    		}else if(mMediaInfo.aCodecName.equals("aac")){
//      	    			mEditor.executeDeleteVideo("/sdcard/2x.mp4","/sdcard/2x_nov.aac");
//      	    		}
//      	    	}
      	    	
//      	    	mEditor.executeVideoCutOut(videoPath,"/sdcard/2x_cut.mp4",5,5);
//	mEditor.executeGetAllFrames("/sdcard/2x.mp4","/sdcard/","getpng");
      	    	
//      	    	isRunning=true;
      	    	
      	    	if(isTestAudio){
//      	    		int ret=mEditor.executeAudioMix("/sdcard/lansongBox/niusanjin.mp3", "/sdcard/lansongBox/aac20s.aac", 0, 0, dstAAC);
      	    	}else{
      	    		
//      	    		int width=mInfo.vCodecWidth;
//      	    		int height=mInfo.vCodecHeight;
//      	    		if(mInfo.vRotateAngle==90 || mInfo.vRotateAngle==270){
//      	    			width=mInfo.vCodecHeight;
//      	    			height=mInfo.vCodecWidth;
//      	    		}
//      	    		int ret=mEditor.executeVideoFrameCrop(videoPath, width/2,height/2, 0, 0, dstMP4,mInfo.vCodecName,mInfo.vBitRate/3);
      	    		
//      	    		int ret=mEditor.executeAddWaterMark(videoPath,"/sdcard/ic_72x72.png",0,0,dstMP4,(int)(mInfo.vBitRate*0.8f));
      	    	
      	    		//Log.i(TAG,"视频压缩");
      	    		//mEditor.executeVideoCompress(videoPath, dstMP4, 0.7f);
      	    		
//      	    		int bitrate=(int)(mInfo.vBitRate*1.2f);
//      	    		if(bitrate>2000*1000)
//      	    			bitrate=2000*1000; //2M
//      	    		int ret=mEditor.executeVideoMirrorH(videoPath,mInfo.vCodecName,bitrate,dstMP4);
      	    		
//      	    		int ret=mEditor.videoReverse(videoPath, mInfo.vCodecName, bitrate, dstMP4);
      	    		
//      	    		int ret=mEditor.executeGetAllFrames(videoPath,mInfo.vCodecName, "/sdcard/","peirui");
      	    		 // mEditor.executePadingVideo(videoPath, mInfo.vCodecName, 720, 640, 0, 0, dstMP4, (int)(mInfo.vBitRate*1.2f));
//      	    		 mEditor.executeCropPaddingVideo(videoPath, mInfo.vCodecName, 320,320,0,0,720,640,0,0,dstMP4, (int)(mInfo.vBitRate*1.2f));
      	    		
//      	    		//测试如下方法. 
//      	    		   mEditor.executeVideoRotateVertically(videoPath, mInfo.vCodecName, (int)(mInfo.vBitRate*1.2f), dstMP4);
      	    		
//      	    			mEditor.executeVideoRotateHorizontally(videoPath, mInfo.vCodecName, (int)(mInfo.vBitRate*1.2f), dstMP4);
      	    		
//      	    		   mEditor.executeImage2Gif();
//      	    		mEditor.executeVideoRotate90Clockwise(videoPath, mInfo.vCodecName, (int)(mInfo.vBitRate*1.2f), dstMP4);
//      	    		mEditor.executeVideoRotate90CounterClockwise(videoPath, mInfo.vCodecName, (int)(mInfo.vBitRate*1.2f), dstMP4);
//      	    		mEditor.executeAVReverse(videoPath, mInfo.vCodecName, (int)(mInfo.vBitRate*1.2f), dstMP4);
      	    		
//      	    			mEditor.executeVideoAdjustSpeed(videoPath, mInfo.vCodecName, 0.5f,(int)(mInfo.vBitRate*1.2f), dstMP4);
//      	    			mEditor.executeVideoZeroAngle(videoPath, mInfo.vCodecName, (int)(mInfo.vBitRate*1.5f), dstMP4);
      	    		
//      	    				mEditor.executeRotateAngle(videoPath, mInfo.vCodecName, 270,(int)(mInfo.vBitRate*1.2f), dstMP4);
//      	    			mEditor.executeSetVideoMetaAngle(videoPath, 270,dstMP4);
//      	    			mEditor.executeGetOneFrame(videoPath,mInfo.vCodecName,mInfo.vDuration/2,"/sdcard/pp.png");
      	    		
//      	    			mEditor.testAddSubtitle2Video(videoPath, mInfo.vCodecName, "/sdcard/subtitle.ass", (int)(mInfo.vBitRate*1.2f), "/sdcard/san.mp4");
      	    			//mEditor.testVideoAddText(videoPath, mInfo.vCodecName, (int)(mInfo.vBitRate*1.5f), dstMP4);
      	    		  	
      	    		}
      	    	
//      	    	Log.i(TAG,"editor executeVideoFrameCrop return ret====================:"+ret);
      	    	
      	    	 //演示同时进行裁剪,叠加和压缩.
      	    	 
//      	    	 int cropW=480;
//      	    	 int cropH=480;
//      	    	 int max=Math.max(mInfo.vWidth,mInfo.vHeight);
//      	    	 
//      	    	 int cropMax=Math.max(cropW, cropH);
//      	    	 float dstBr=(float)mInfo.vBitRate;
//      	    	 
//      	    	 float ratio=(float)cropMax/(float)max;
//      	    	 
//      	    	 dstBr*=ratio;  //得到恒定码率的等比例值.
//      	    	 dstBr*=0.8f; //再压缩20%.
//      	    	 
//      	    	 dstBr/=2;
//      	    	 
//      	    	 mEditor.executeCropOverlay(videoPath, mInfo.vCodecName, "/sdcard/watermark.png", 20, 20, cropW, cropH, 0, 0, dstPath, (int)dstBr);
      	   	
      	    	 
//      	    	mEditor.executeConvertMp4toTs("/sdcard/2x.mp4","/sdcard/2x0.ts");
      	    	//因为静态码率
//      	    	mEditor.executeAddWaterMark(videoPath,"/sdcard/watermark.png",0,0,dstPath,(int)(mInfo.vBitRate));
      	    	
//      	    	mEditor.pictureFadeInOut("/sdcard/threeword.png",5,0,40,50,75,dstPath);
      	    //	mEditor.pictureFadeIn("/sdcard/testfade.png",3,0,60,"/sdcard/testfade2.mp4");
      	//  	mEditor.pictureFadeOut("/sdcard/testfade.png",3,0,60,"/sdcard/testfade3.mp4");
      	    	
      	    	
//      	    	 mEditor.waterMarkFadeIn("/sdcard/2x.mp4","/sdcard/watermark.png",2,5,0,30,0,0,dstPath);
      	    	
//      	    	mEditor.executeRotateAngle("/sdcard/2x.mp4", mMediaInfo.vCodecName, 90, "/sdcard/F1.mp4",1000000);
      	    	
      	    	//这里检测mp3的时长,
//      	    	mEditor.audioAdjustVolumeMix("/sdcard/hongdou.mp3", "/sdcard/kaimendaji.mp3", 3.0f, 0.5f, "/sdcard/hongdouxxx.mp3");
      	    	
      	    //	mVideoEditor.audioAdjustVolumeMix("/sdcard/save_encodec4.aac","/sdcard/kaimen20s.mp3",3.0f,0.4f,"/sdcard/jni_amix.aac");
      	    	//mVideoEditor.avReverse(null, null, null);
      	    	//String srcPath,int totalTime,int fadeinstart,int fadeinCnt,int fadeoutstart,int fadeoutCnt,
//     		   	String dstPath
      	    	//demoVideoGray();
      	    	//demodeleteMisuc();
      	    	 
//      	    	 mEditor.audioPcmCut("/sdcard/j1.pcm", 44100, 2, 2, 1000, 5000, "/sdcard/p2.pcm");
      	    	 
//      	    	mEditor.audioPcmMute("/sdcard/j1.pcm", 44100, 2, 2, 8000, 25000, "/sdcard/p3.pcm");
      	    	
//      	    	mEditor.audioPcmReplace("/sdcard/j1.pcm", "/sdcard/p2.pcm",44100, 2, 2, 8000,"/sdcard/p4.pcm");
      	    	
      	    	return null;
      	    }
    	@Override
    	protected void onPostExecute(Boolean result) { 
    		// TODO Auto-generated method stub
    		super.onPostExecute(result);
    		if( mProgressDialog!=null){
	       		 mProgressDialog.cancel();
	       		 mProgressDialog=null;
    		}
    		isRunning=false;
    		
    		if(isTestAudio){
    			if(SDKFileUtils.fileExist(dstAAC))
        			findViewById(R.id.id_video_play_btn).setEnabled(true);
    		}else{
    			if(SDKFileUtils.fileExist(dstMP4))
        			findViewById(R.id.id_video_play_btn).setEnabled(true);
    		}
    	}
    }
	  @Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		  if(isRunning){
			  Log.e(TAG,"VideoEditor is running...cannot back!!! ");
			  return ;
		  }
			  
		  
		super.onBackPressed();
	}
	  //-------------------------------------------------------------------------
	 
}

