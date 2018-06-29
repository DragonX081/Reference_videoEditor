package com.example.commonDemo;

import android.app.Activity;
import android.app.ProgressDialog;
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
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;
import com.lansosdk.videoeditor.onVideoEditorEncodeChangedListener;
import com.lansosdk.videoeditor.onVideoEditorProgressListener;

import java.io.IOException;

/**
 * 
 * 杭州蓝松科技, 专业的视频开发团队.
 * 
 * 基本版本中视频编辑演示.
 * 此代码不属于sdk的一部分， 仅作为演示使用。
 * 
 */
public class AVEditorDemoActivity extends Activity implements OnClickListener{


	private final static String TAG="AVSplitDemoActivity";
	private final static  boolean VERBOSE = false;   
	
	String srcVideo=null;
	
	VideoEditor mEditor = null;;
	ProgressDialog  mProgressDialog;

	private String dstVideo=null;
	private String dstAudio=null;
	private boolean isRunning=false; 
	private int demoID=0;
	private int textID=0;
	private boolean isOutVideo;
	private boolean isOutAudio;
	
	
	  @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
			 setContentView(R.layout.test_cmd_layout);
				
			TextView tvText=(TextView)findViewById(R.id.id_test_cmd_demo_hint);
			 
	        findViewById(R.id.id_test_cmd_btn).setOnClickListener(this);
	        findViewById(R.id.id_test_cmdvideo_play_btn).setOnClickListener(this);
	        findViewById(R.id.id_test_cmdaudio_play_btn).setOnClickListener(this);
	        
	        srcVideo=getIntent().getStringExtra("videopath1");
	        demoID=getIntent().getIntExtra("demoID",0);
	        isOutVideo=getIntent().getBooleanExtra("outvideo", false);
	        isOutAudio=getIntent().getBooleanExtra("outaudio", false);
	        textID=getIntent().getIntExtra("textID",0);
	        if(demoID!=0){
	        	setTitle(demoID);
	        }
	        if(textID!=0){
	        	tvText.setText(textID);
	        }
	        
	        if(isOutVideo==false){
	        	findViewById(R.id.id_test_cmdvideo_play_btn).setVisibility(View.GONE);
	        }
	        if(isOutAudio==false){
	        	findViewById(R.id.id_test_cmdaudio_play_btn).setVisibility(View.GONE);
	        }
	      /**
	       * 第一步,创建VideoEditor对象, 并设置进度监听,当然您也可以不设置监听.
	       */
	        mEditor=new VideoEditor();
	        mEditor.setOnProgessListener(new onVideoEditorProgressListener() {
				
				@Override
				public void onProgress(VideoEditor v, int percent) {
					// TODO Auto-generated method stub
					if(mProgressDialog!=null){
						mProgressDialog.setMessage("正在处理中..."+String.valueOf(percent)+"%");
					}
				}
			});
		  mEditor.setOnEncodeChangedListener(new onVideoEditorEncodeChangedListener() {
			  @Override
			  public void onChanged(VideoEditor v, boolean isSoftencoder) {
				  Toast.makeText(getApplicationContext(),"切换为软编码...",Toast.LENGTH_SHORT).show();
			  }
		  });
	        dstVideo= SDKFileUtils.newMp4PathInBox();
	        dstAudio= SDKFileUtils.newMp4PathInBox();
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
	  @Override
		protected void onDestroy() {
			super.onDestroy();
				SDKFileUtils.deleteFile(dstAudio);
				dstAudio=null;
				SDKFileUtils.deleteFile(dstVideo);
				dstVideo=null;
			if(audioPlayer!=null){
				audioPlayer.stop();
				audioPlayer.release();
				audioPlayer=null;
			}
		}
	  @Override
	  public void onClick(View v) {
		// TODO Auto-generated method stub
		  switch (v.getId()) {
				case R.id.id_test_cmd_btn:
					if(isRunning==false){
						new SubAsyncTask().execute();  //开始VideoEditor方法的处理==============>
					}
					break;
				case R.id.id_test_cmdvideo_play_btn:
					playDstVideo();
					break;
				case R.id.id_test_cmdaudio_play_btn:
						playDstAudio();
					break;
				default:
					break;
		}
	}
	 
	  
	  /**
	   * 第二步:创建一个AsyncTask,并在backgroud中执行VideoEditor的方法.(当然您也可以创建一个Thread,在Thread中执行)
	   */
	  public class SubAsyncTask extends AsyncTask<Object, Object, Boolean>{
			  @Override
			protected void onPreExecute() {
			// TODO Auto-generated method stub
				  showProgressDialog();
		          isRunning=true;
		          super.onPreExecute();
			}
      	    @Override
      	    protected synchronized Boolean doInBackground(Object... params) {
      	    	// TODO Auto-generated method stub
      	    	/**
      	    	 * 真正执行的代码,因演示的方法过多, 用每个方法的ID的形式来区分, 您实际使用中, 可直接填入具体方法的代码.
      	    	 */
      	    	startRunDemoFunction();
      	    	return null;
      	    }
	    	@Override
	    	protected void onPostExecute(Boolean result) { 
	    		// TODO Auto-generated method stub
	    		super.onPostExecute(result);
	    		
	    		calcelProgressDialog();
	    		
	    		isRunning=false;
	    		
	    		if(SDKFileUtils.fileExist(dstVideo))
	        		findViewById(R.id.id_test_cmdvideo_play_btn).setEnabled(true);
	    	}
    }
	  private int startRunDemoFunction()
	  {
		  int ret=-1;
		  switch (demoID) {
				case R.string.demo_id_avsplit: //音视频分离.
					DemoFunctions.demoAVSplite(mEditor, srcVideo, dstVideo, dstAudio);
					break;
				case R.string.demo_id_avmerge:  //合成音频/替换音频
					DemoFunctions.demoAVMerge(AVEditorDemoActivity.this,mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_cutaudio:  //剪切音频
					DemoFunctions.demoAudioCut(AVEditorDemoActivity.this,mEditor, dstAudio);
					break;
				case R.string.demo_id_cutvideo: //剪切视频
					DemoFunctions.demoVideoCut(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_concatvideo:  //视频拼接
					DemoFunctions.demoVideoConcat(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videocompress:
					//long  beforeDraw=System.currentTimeMillis();
					   
					DemoFunctions.demoVideoCompress(mEditor, srcVideo, dstVideo);
				    //Log.i(TAG,"draw 耗时:"+ (System.currentTimeMillis() -beforeDraw));
					break;
				case R.string.demo_id_videocrop:
					DemoFunctions.demoFrameCrop(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videoscale_soft:
					DemoFunctions.demoVideoScale(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videowatermark:
					 DemoFunctions.demoAddPicture(AVEditorDemoActivity.this, mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videocropwatermark:
					DemoFunctions.demoVideoCropOverlay(AVEditorDemoActivity.this, mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videogetframes:
					DemoFunctions.demoGetAllFrames(mEditor, srcVideo);
					break;
				case R.string.demo_id_videogetoneframe:
					DemoFunctions.demoGetOneFrame(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videoclockwise90:
					DemoFunctions.demoVideoRotate90Clockwise(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videocounterClockwise90:
					DemoFunctions.demoVideoRotate90CounterClockwise(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videoaddanglemeta:
					DemoFunctions.demoSetVideoMetaAngle(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_audiodelaymix:
					DemoFunctions.demoAudioDelayMix(AVEditorDemoActivity.this, mEditor, dstAudio);
					break;
				case R.string.demo_id_audiovolumemix:
					DemoFunctions.demoAudioVolumeMix(AVEditorDemoActivity.this, mEditor, dstAudio);
					break;
				case R.string.demo_id_videopad:
					DemoFunctions.demoPaddingVideo(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videoadjustspeed:
					DemoFunctions.demoVideoAdjustSpeed(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videomirrorh:
					DemoFunctions.demoVideoMirrorH(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videomirrorv:
					DemoFunctions.demoVideoMirrorV(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videorotateh:
					DemoFunctions.demoVideoRotateHorizontally(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videorotatev:
					DemoFunctions.demoVideoRotateVertically(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_videoreverse:
					DemoFunctions.demoVideoReverse(mEditor, srcVideo, dstVideo);
					break;
				case R.string.demo_id_avreverse:
					DemoFunctions.demoAVReverse(mEditor, srcVideo, dstVideo);
					break;
		default:
			break;
		}
		  return ret;
	  } 
	  
	  private void playDstVideo()
	  {
		  if(SDKFileUtils.fileExist(dstVideo)){
		    	Intent intent=new Intent(AVEditorDemoActivity.this,VideoPlayerActivity.class);
		    	intent.putExtra("videopath", dstVideo);
		    	startActivity(intent);
			}else{
				Toast.makeText(AVEditorDemoActivity.this, R.string.file_not_exist,Toast.LENGTH_SHORT).show();
			}
	  }
	  MediaPlayer audioPlayer=null;
	  private void playDstAudio()
	  {
		  		Log.i(TAG,"play dst audio"+dstAudio);
				if(MediaInfo.isSupport(dstAudio) && audioPlayer ==null)
				{
					audioPlayer=new MediaPlayer();
					try {
						audioPlayer.setDataSource(dstAudio);
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
	  private void showProgressDialog()
	  {
		  mProgressDialog = new ProgressDialog(AVEditorDemoActivity.this);
          mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
          mProgressDialog.setMessage("正在处理中...");
          mProgressDialog.setCancelable(false);
          mProgressDialog.show();
	  }
	  private void calcelProgressDialog()
	  {
		  if( mProgressDialog!=null){
	       		 mProgressDialog.cancel();
	       		 mProgressDialog=null;
		  }
	  }
	  
	  
	  
	  
	  
}

