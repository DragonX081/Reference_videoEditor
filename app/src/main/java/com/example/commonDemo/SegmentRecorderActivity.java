package com.example.commonDemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera.Area;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.lansoeditor.demo.R;
import com.lansosdk.videoeditor.OpenSegmentsRecordListener;
import com.lansosdk.videoeditor.OpenSegmentsRecorder;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("deprecation")
public class SegmentRecorderActivity extends Activity implements Handler.Callback ,OnClickListener {
	private final static String TAG = "SegmentRecorderActivity";

	
	public static final float MAX_RECORD_TIME = 15 * 1000f;  //设置录制的最大时间.  15秒.
	
	
	public static final float MIN_RECORD_TIME = 2 * 1000f;   //录制的最小时间
	
	
	
	private PowerManager.WakeLock mWakeLock;
	private OpenSegmentsRecorder segmentRecorder;

	private Handler handler;

	private static final int MSG_SEGMENT_PROGRESS = 0;
	private static final int MSG_SEGMENT_PAUSE = 1;
	private static final int MSG_VIDEOCAMERA_READY = 2;
	private static final int MSG_STARTRECORD = 3;
	private static final int MSG_PAUSERECORD = 4;
	private static final int MSG_CHANGE_FLASH = 66;
	private static final int MSG_CHANGE_CAMERA = 8;
	private static final int MSG_AUTO_FOCUS = 9;
	private static final int MSG_FOCUS_FINISH = 10;

	public static final int REQUEST_VIDEOPROCESS = 5;

	private boolean mAllowTouchFocus = false;

	Button quitBtn;
	Button flashBtn;
	Button cancelBtn;
	Button nextBtn;
	Button recorderVideoBtn;
	Button switchCameraIcon;
	VideoProgressView progressView;
	VideoPreviewView cameraTextureView;
	VideoFocusView focusView;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.video_record_activity);
		 
		 
	
		initView();
		
		handler = new Handler(this);
		//第一步:初始化 断点录制. 
		//最后三个参数是:视频编码宽度,视频编码高度,视频编码码率,因为是竖屏拍照, 录制的视频宽度和高度对调了,故这里设置480和640等.
		segmentRecorder = new OpenSegmentsRecorder(this, cameraTextureView.getHolder(),480,480,1000*1000); //建议采用这个
//		segmentRecorder = new OpenSegmentsRecorder(this, cameraTextureView.getHolder(),480,640,1500*1000);
//		segmentRecorder = new OpenSegmentsRecorder(this, cameraTextureView.getHolder(),720,1280,2000*1000);
//		segmentRecorder = new OpenSegmentsRecorder(this, cameraTextureView.getHolder(),1088,1920,3000*1000);
		
		//第二步:设置断点回调的各种方法.
		segmentRecorder.setSegmentsRecordListener(new OpenSegmentsRecordListener() {

			@Override  //当前段开始录制  在每次开始前调用.
			public void segmentRecordStart() {
				// TODO Auto-generated method stub
				progressView.setCurrentState(VideoProgressView.State.START);
			}
			
			 // 当前段录制停止了, timeMS  当前在暂停时的录制总时间. 
			//  segmentIdx segmnet的总数也是当前文件的索引, 等于 getSegmentSize();
			@Override  
			public void segmentRecordPause(int timeMS, int segmentIdx) {
				// TODO Auto-generated method stub
				handler.obtainMessage(MSG_SEGMENT_PAUSE, segmentIdx, 0).sendToTarget();
				progressView.setCurrentState(VideoProgressView.State.PAUSE);
				progressView.putTimeList(timeMS);
			}
			
			@Override  // 当前录制的总进度.  包括之前已经存在的视频段  加上正在录制的视频段.
			public void segmentProgress(long totalTime) {
				// TODO Auto-generated method stub
				handler.obtainMessage(MSG_SEGMENT_PROGRESS, (int) totalTime, 0).sendToTarget();
			}
			
			@Override  //相机准备好, 返回的预览大小
			public void segmentCameraReady(int[] previewSize) {
				// TODO Auto-generated method stub
				cameraTextureView.setAspectRatio(previewSize[1], previewSize[0]);
				handler.obtainMessage(MSG_VIDEOCAMERA_READY).sendToTarget();
			}
		});
		
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
			switchCameraIcon.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
			mWakeLock.acquire();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		segmentRecorder.pauseRecord();
		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(segmentRecorder!=null){
			segmentRecorder.release();
			segmentRecorder=null;
		}
	}

	private void recordEnd() {
		String dstfile=segmentRecorder.stopRecord();
		if(dstfile!=null){  //文件存在.
			Intent intent=new Intent(SegmentRecorderActivity.this,VideoPlayerActivity.class);
	    	intent.putExtra("videopath", dstfile);
	    	startActivity(intent);
			//Toast.makeText(SegmentRecorderActivity.this, "视频合成完毕", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(SegmentRecorderActivity.this, "视频合成失败", Toast.LENGTH_LONG).show();
		}
	}
	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case MSG_SEGMENT_PROGRESS:
				int tm = msg.arg1;  //传递过来的是总时间
				
				if (tm < MIN_RECORD_TIME) 
				{
					nextBtn.setVisibility(View.INVISIBLE);
				} else if (tm >= MIN_RECORD_TIME && tm < MAX_RECORD_TIME) {
					nextBtn.setVisibility(View.VISIBLE);
				} else if (tm >= MAX_RECORD_TIME) {
					segmentRecorder.pauseRecord();
					recordEnd();
				}
				
				break;

			case MSG_SEGMENT_PAUSE:
				int se = msg.arg1;  //如果序号??
				if (se < 1) {
					cancelBtn.setVisibility(View.INVISIBLE);
				} else {
					cancelBtn.setVisibility(View.VISIBLE);
				}
				break;
			case MSG_STARTRECORD:
				recorderVideoBtn.setBackgroundResource(R.drawable.video_record_start_btn_pressed);
				pauseAudioPlayback();
				segmentRecorder.startRecord();
				break;
			case MSG_PAUSERECORD:
				recorderVideoBtn.setBackgroundResource(R.drawable.video_record_start_btn);
				segmentRecorder.pauseRecord();
				break;
			case MSG_VIDEOCAMERA_READY:
				resetVideoLayout();
				handler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 300);
				break;
			case MSG_CHANGE_CAMERA:
				segmentRecorder.changeCamera();
				handler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 300);
				break;
			case MSG_CHANGE_FLASH:
				segmentRecorder.changeFlash();
				break;
			case MSG_AUTO_FOCUS:
				doAutoFocus();
				handler.sendEmptyMessageDelayed(MSG_FOCUS_FINISH, 1000);
				break;
			case MSG_FOCUS_FINISH:
				if (focusView != null) {
					focusView.setHaveTouch(false, new Rect(0, 0, 0, 0));
					mAllowTouchFocus = true;
				}
				break;
		}
		return false;
	}
	
//----------------follow is ui code----------一下是UI界面代码--------------------------------------------------------------------------------------------------------	
	
	private void initView()
	{

		quitBtn =(Button)findViewById(R.id.quitBtn);
		flashBtn =(Button)findViewById(R.id.recorder_flashlight);
		
		cancelBtn=(Button)findViewById(R.id.recorder_cancel);
		
		nextBtn=(Button)findViewById(R.id.recorder_next);
		
		recorderVideoBtn=(Button)findViewById(R.id.recorder_video);
		
		switchCameraIcon=(Button)findViewById(R.id.recorder_frontcamera);
		
		progressView=(VideoProgressView)findViewById(R.id.recorder_progress);
		
		cameraTextureView=(VideoPreviewView)findViewById(R.id.recorder_surface);
		 
		focusView=(VideoFocusView)findViewById(R.id.video_focus_view);
		
		
		
		quitBtn.setOnClickListener(this);
		flashBtn.setOnClickListener(this);
		cancelBtn.setOnClickListener(this);
		nextBtn.setOnClickListener(this);
		switchCameraIcon.setOnClickListener(this);
		
		recorderVideoBtn.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						handler.sendEmptyMessage(MSG_STARTRECORD);
						break;
					case MotionEvent.ACTION_UP:
						handler.sendEmptyMessage(MSG_PAUSERECORD);
						break;
					}
				return true;
			}
		});
		
		focusView.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				return onSquareFocusViewTouch(v,event);
			}
		});
	}
	
	private void doAutoFocus() {
		if(segmentRecorder!=null){
			boolean con = segmentRecorder.supportFocus() && segmentRecorder.isPreviewing();
			if (con) {
				if (mAllowTouchFocus && focusView != null && focusView.getWidth() > 0) {
					mAllowTouchFocus = false;
					int w = focusView.getWidth();
					Rect rect = doTouchFocus(w / 2, w / 2);
					if (rect != null) {
						focusView.setHaveTouch(true, rect);
					}
				}
			}
		}
	}

	private Rect doTouchFocus(float x, float y) {
		int w = cameraTextureView.getWidth();
		int h = cameraTextureView.getHeight();
		int left = 0;
		int top = 0;
		if (x - VideoFocusView.FOCUS_IMG_WH / 2 <= 0) {
			left = 0;
		} else if (x + VideoFocusView.FOCUS_IMG_WH / 2 >= w) {
			left = w - VideoFocusView.FOCUS_IMG_WH;
		} else {
			left = (int) (x - VideoFocusView.FOCUS_IMG_WH / 2);
		}
		if (y - VideoFocusView.FOCUS_IMG_WH / 2 <= 0) {
			top = 0;
		} else if (y + VideoFocusView.FOCUS_IMG_WH / 2 >= w) {
			top = w - VideoFocusView.FOCUS_IMG_WH;
		} else {
			top = (int) (y - VideoFocusView.FOCUS_IMG_WH / 2);
		}
		Rect rect = new Rect(left, top, left + VideoFocusView.FOCUS_IMG_WH, top + VideoFocusView.FOCUS_IMG_WH);
		
		Rect targetFocusRect = new Rect(rect.left * 2000 / w - 1000, rect.top * 2000 / h - 1000, rect.right * 2000 / w - 1000, rect.bottom * 2000 / h - 1000);
		try {
			List<Area> focusList = new ArrayList<Area>();
			Area focusA = new Area(targetFocusRect, 1000);
			focusList.add(focusA);
			segmentRecorder.doFocus(focusList);
			return rect;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private void resetVideoLayout() {
		if (segmentRecorder.flashEnable()) {
			flashBtn.setVisibility(View.VISIBLE);
		} else {
			flashBtn.setVisibility(View.GONE);
		}
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int screenWidth =size.x;

		FrameLayout cameraPreviewArea = (FrameLayout) findViewById(R.id.cameraPreviewArea);
		int cameraPreviewAreaHeight = cameraPreviewArea.getHeight();
		int salt = screenWidth + (cameraPreviewAreaHeight - screenWidth) / 2;
		//
		View recorder_surface_mask1 = findViewById(R.id.recorder_surface_mask1);
		View recorder_surface_mask2 = findViewById(R.id.recorder_surface_mask2);
		//
		FrameLayout.LayoutParams layoutParam2 = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		layoutParam2.bottomMargin = salt;

		FrameLayout.LayoutParams layoutParam3 = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		layoutParam3.topMargin = salt;
		//
		recorder_surface_mask1.setLayoutParams(layoutParam2);
		recorder_surface_mask2.setLayoutParams(layoutParam3);
		//
		FrameLayout.LayoutParams layoutParam4 = (FrameLayout.LayoutParams) progressView.getLayoutParams();
		layoutParam4.topMargin = salt;
		progressView.setLayoutParams(layoutParam4);

		FrameLayout recorder_handl_area = (FrameLayout) findViewById(R.id.recorder_handl_area);
		FrameLayout.LayoutParams layoutParam5 = new FrameLayout.LayoutParams(screenWidth, screenWidth);
		layoutParam5.topMargin = (cameraPreviewAreaHeight - screenWidth) / 2;
		recorder_handl_area.setLayoutParams(layoutParam5);
	}

	// events --------------------------------------------------
	private boolean onSquareFocusViewTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				focusView.setDownY(event.getY());
				if(segmentRecorder!=null)
				{
					boolean con = segmentRecorder.supportFocus() && segmentRecorder.isPreviewing();
					if (con) {// 对焦
						if (mAllowTouchFocus) {
							mAllowTouchFocus = false;
							Rect rect = doTouchFocus(event.getX(), event.getY());
							if (rect != null) {
								focusView.setHaveTouch(true, rect);
							}
							handler.sendEmptyMessageDelayed(MSG_FOCUS_FINISH, 1000);
						}
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				float upY = event.getY();
				float dis = upY - focusView.getDownY();
				if (Math.abs(dis) >= 100) {
					if (segmentRecorder.cameraChangeEnable()) {
						handler.sendEmptyMessage(MSG_CHANGE_CAMERA);
					}
				}
				break;
		}
		return true;
	}


	private volatile boolean isDeleteState = false;
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
			case R.id.recorder_cancel:
				
				if(segmentRecorder.getSegmentSize()<=0){
					isDeleteState = false;
					progressView.setCurrentState(VideoProgressView.State.DELETE);
					cancelBtn.setBackgroundResource(R.drawable.video_record_backspace);
					break;
				}
				
				if (isDeleteState) {
					isDeleteState = false;
					segmentRecorder.deleteSegment();

					progressView.setCurrentState(VideoProgressView.State.DELETE);
					cancelBtn.setBackgroundResource(R.drawable.video_record_backspace);
					
				} else { 
					isDeleteState = true;  //在按一下, 删除.
					progressView.setCurrentState(VideoProgressView.State.BACKSPACE);
					cancelBtn.setBackgroundResource(R.drawable.video_record_delete);
				}
				break;
			case R.id.recorder_frontcamera:
				handler.sendEmptyMessage(MSG_CHANGE_CAMERA);
				break;
			case R.id.recorder_next:
				recordEnd();
				break;
			case R.id.recorder_flashlight:
				handler.sendEmptyMessage(MSG_CHANGE_FLASH);
				break;
			case R.id.quitBtn:
				onBackPressed();
				break;

		default:
			break;
		}
	}
	/**
	 * 在录音的时候, 停止其他的播放.
	 */
	private void pauseAudioPlayback() {
		Intent i = new Intent("com.android.music.musicservicecommand");
		i.putExtra("command", "pause");
		sendBroadcast(i);
	}
}
