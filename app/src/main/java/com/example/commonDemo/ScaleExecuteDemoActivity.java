package com.example.commonDemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.lansoeditor.demo.R;
import com.lansosdk.box.ScaleExecute;
import com.lansosdk.box.onScaleCompletedListener;
import com.lansosdk.box.onScaleProgressListener;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKDir;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;

/**
 * 对视频画面进行缩放, 用ffmpeg也可以完成, 但手机cpu的限制,ffmpeg用软件代码的形式来对一帧像素进行处理, 太慢了,在手机上完全无法使用. 
 * 基于此,我们推出了 用OpenGL来完成的硬缩放的方式,极大的提升了视频缩放的速度.
 * 
 *  演示: ScaleExecute类的使用. 这个是在后台进行缩放,在OpenGL线程中运行.
 * 流程是: 硬件解码---->OpenGL处缩放--->硬件编码.
 */
public class ScaleExecuteDemoActivity extends Activity{

	String videoPath=null;
	ProgressDialog  mProgressDialog;
	 int videoDuration;
	 boolean isRuned=false;
	 MediaInfo   mInfo;
	 TextView tvProgressHint;
	 TextView tvSelectFile;
	 /**
	 * 视频缩放中用到的临时文件路径.
	 */
	private String editTmpPath=null;
	/**
	 * 视频缩放的最终目标文件路径
	 */
	private String dstPath=null;
	
	 
	private static final String TAG="ScaleExecuteDemoActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		
		super.onCreate(savedInstanceState);
		 
		 setContentView(R.layout.video_scale_layout);
		 
		 tvProgressHint=(TextView)findViewById(R.id.id_video_scale_progress_hint);
		 tvSelectFile=(TextView)findViewById(R.id.id_video_scale_selectfilehint);
		 
		 videoPath=getIntent().getStringExtra("videopath");
		 
       findViewById(R.id.id_video_scale_startbtn).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(videoPath!=null)
				{
					 mInfo=new MediaInfo(videoPath,false);
					 
					 if(mInfo.prepare())
					 {
						 if(mInfo.vDuration>=60*1000){//大于60秒
								showHintDialog();
							}else{
								testScaleEdit();
							}
					 }
				}
			}
		});
       
       findViewById(R.id.id_video_scale_play).setEnabled(false);
       findViewById(R.id.id_video_scale_play).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(SDKFileUtils.fileExist(dstPath)){
					Intent intent=new Intent(ScaleExecuteDemoActivity.this,VideoPlayerActivity.class);
	    	    	intent.putExtra("videopath", dstPath);
	    	    	startActivity(intent);
				}else{
					 Toast.makeText(ScaleExecuteDemoActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
				}
			}
		});

       //在手机的默认路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
       editTmpPath=SDKFileUtils.newMp4PathInBox();
       dstPath=SDKFileUtils.newMp4PathInBox();
	}
	
	
	VideoEditor mVideoEditer;
	private void showHintDialog()
	{
		new AlertDialog.Builder(this)
		.setTitle("提示")
		.setMessage("视频过大,可能会需要一段时间,您确定要处理吗?")
        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				testScaleEdit();
			}
		})
		.setNegativeButton("取消", null)
        .show();
	}
	 //--------------------------------------------------
	private boolean isExecuting=false;
	private  long startTime=0;
	private void testScaleEdit()
	{
		if(isExecuting)
			return ;
		
		isExecuting=true;
		/**
		 * 创建ScaleExecute类.
		 */
		ScaleExecute  vScale=new ScaleExecute(ScaleExecuteDemoActivity.this,videoPath);  //videoPath是路径
		//设置缩放后输出文件路径.
		vScale.setOutputPath(editTmpPath);
		
		
		//计算等比例缩放的压缩码率.
		 int scaleWidth=mInfo.vCodecWidth/2;  //这里是把视频的宽度和高度压缩一半.
		 int scaleHeight=mInfo.vCodecHeight/2;
		 
	  	 if(mInfo.vRotateAngle==90 || mInfo.vRotateAngle==270){  //交互一下
	  		 int tmp=scaleWidth;
	  		 scaleWidth=scaleHeight;
	  		 scaleHeight=tmp;
	  	 }
		 
	  	 //这里暂定为码率为原来的70%,如果您想更小一些,则可以在这里修改即可,码率不是越小越好, 在分辨率不变的情况下, 码率过小可能出现马赛克,过大或许编码器出错.一般线性比例为好.
	  	 float scaleBitRate=(float)mInfo.vBitRate*0.7f;  
	  	 
		
	  	//当没有设置或设置为false时,,如果原来视频旋转了270或90度,则缩放后的视频也旋转270或90度. 当设置为true时, 把原来旋转的视频角度值去掉,进行缩放.默认是false或不设置.
	  	vScale.setModifyAngle(false);
	  	 
	  	 
		//设置缩放的宽度,高度和缩放后保存文件的码率.
		vScale.setScaleSize(scaleWidth,scaleHeight,(int)scaleBitRate);
		
		//设置缩放进度监听.currentTimeUS当前处理的视频帧时间戳.
		vScale.setScaleProgessListener(new onScaleProgressListener() {
			
			@Override
			public void onProgress(ScaleExecute v, long currentTimeUS) {
				// TODO Auto-generated method stub
				//currentTimeUS:处理当前帧的时间戳.可以用这个和总时长整除, 从而得到 进度百分比.
				tvProgressHint.setText(String.valueOf(currentTimeUS));
			}
		});
		
		//设置缩放进度完成后的监听.
		vScale.setScaleCompletedListener(new onScaleCompletedListener() {
			
			@Override
			public void onCompleted(ScaleExecute v) {
				// TODO Auto-generated method stub
				tvProgressHint.setText("Completed!!!");
				isExecuting=false;
				if(SDKFileUtils.fileExist(editTmpPath)){
					//增加音频信息
					boolean ret=VideoEditor.encoderAddAudio(videoPath, editTmpPath,SDKDir.TMP_DIR, dstPath);
					if(!ret){
						dstPath=editTmpPath;
					}
				}
				Log.i(TAG,"耗时时间是:"+(System.currentTimeMillis() -startTime));

				findViewById(R.id.id_video_scale_play).setEnabled(true);
			}
		});
		vScale.start();
		startTime=System.currentTimeMillis();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
			 SDKFileUtils.deleteFile(dstPath);
	    	   SDKFileUtils.deleteFile(editTmpPath);
	}
}	
