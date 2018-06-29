/**
 * 杭州蓝松科技, 专业的视频开发团队
 * 
 * www.lansongtech.com
 * 
 * 此代码为开源给客户使用，请勿传递给第三方。 谢谢。
 */
package com.lansosdk.videoeditor;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("deprecation")
public class OSegmentsRecorder implements PreviewCallback, SurfaceHolder.Callback {

	private static final String TAG = "lansosdk";
	private OCameraManager cameraManager = null;
	
	private long currentSegmentStartTime=0;  
	
	private long currentSegDuration;   //当前正在录制段的时间.
	
	private long lastSegmentsTotalTime;  //这段之前的所有时间之和.  //总时间等于 lastSegmentsTotalTime+ currentSegDuration
	

	private volatile AtomicBoolean mIsRecording = new AtomicBoolean(false);

	private AudioRecord audioRecorder;

	private String currentSegVideoFile=null;  //ts格式
	private String finalVideoFile;  //最终的视频文件.
	

	private LinkedList<VideoSegment> recorderFiles = new LinkedList<VideoSegment>();


	private Semaphore semp = new Semaphore(1);

	private VideoEncodeThread encodeThread;
	
	
	private AudioSampleThread aSampleThread;

	private SurfaceHolder mSurfaceHolder = null;	

	private AVEncoder mEncoder=null;
	
	private int firstSegmentDegree=-1;  //第一段录制时的角度.
	
	
	private Activity mActivity;
	private OpenSegmentsRecordListener segmentRecordListener = null;
	private int[] previewSize;
	private int mEncWidth,mEncHeight;
	private int mEncBitrate;
	/**
	 * 此类不建议用在 分段录制的过程中, 切换前后摄像头的情况.
	 * 此类不建议用在 分段录制的过程中, 切换前后摄像头的情况.
	 * 此类不建议用在 分段录制的过程中, 切换前后摄像头的情况.
	 * 此类不建议用在 分段录制的过程中, 切换前后摄像头的情况.
	 * 
	 * @param activity
	 * @param holder
	 * @param encWidth  视频编码的宽度, 因是手机是竖屏,则这里应该是高度.
	 * @param encHeight 视频编码的高度, 因是手机是竖屏,则这里应该是宽度
	 * @param bitrate
	 */
	public OSegmentsRecorder(Activity activity, SurfaceHolder holder,int encWidth,int encHeight,int bitrate) {
		
		mActivity=activity;
		cameraManager = new OCameraManager(activity,encWidth,encHeight);
		mSurfaceHolder = holder;
		mSurfaceHolder.addCallback(this);
		mEncoder=new AVEncoder();
		
		this.mEncWidth=encWidth;
		this.mEncHeight=encHeight;  //需要视频的分辨率
		
		mEncBitrate=bitrate;   //希望编码的码率
	}

	public int[] getPreviewSize() {
		return cameraManager.getPreviewSize();
	}

	
	/**
	 * 每次开始录制, 都初始化一次编码器,重新开始录制.
	 */
	private void initRecorderParam() {
		
		//生成一个视频文件.
		if(cameraManager!=null && cameraManager.isPreviewing()){
			currentSegVideoFile = 	SDKFileUtils.createFileInBox("ts");
			
			if(firstSegmentDegree==-1){
				firstSegmentDegree=cameraManager.getPreviewDataDegress();
			}
			mEncoder.init(currentSegVideoFile,firstSegmentDegree,mEncWidth,mEncHeight,25,mEncBitrate,44100,64000);
			
			try {
				semp.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			mIsRecording.set(true);
			
			
			encodeThread = new VideoEncodeThread();  //视频编码		
			aSampleThread = new AudioSampleThread(); //音频采集		
			
			
			aSampleThread.start();
			encodeThread.start();
			
		}
		
	}
	/**
	 * 开始录制一段视频
	 * 
	 */
	public boolean startRecord() {
			if(currentSegVideoFile!=null){
				Log.e(TAG,"last encoder is releasing. cannot start.return ;");
				return false;
			}
			initRecorderParam();
			currentSegmentStartTime=0;
			currentSegDuration=0;
			
			if(segmentRecordListener!=null){
				segmentRecordListener.segmentRecordStart();
			}
			return true;
	}

	public void pauseRecord() {
		if (mIsRecording.get()) {
			try {
				mIsRecording.set(false);
				if(aSampleThread!=null){
					aSampleThread.join();
					aSampleThread=null;
				}
				
				lastSegmentsTotalTime+=currentSegDuration;
				if(segmentRecordListener!=null &&currentSegDuration!=0){
					segmentRecordListener.segmentRecordPause((int) lastSegmentsTotalTime,recorderFiles.size()+1);  //加一是说当前已经有一个,只是异步执行.
				}
				//开启异步线程去等待编码完成.
				startEndencoderThread();
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				semp.release();
			}
		}
		mIsRecording.set(false);
	}
	Thread endEncoderThread=null;
	private void startEndencoderThread()
	{
		endEncoderThread= new Thread(new Runnable() {
			@Override
			public void run() {
				
				if(currentSegVideoFile!=null){
					
					try {
						
						if(encodeThread!=null){
							encodeThread.join();
							encodeThread=null;
						}
						
						if(mEncoder!=null){
							mEncoder.release(); //这里mEncoder不能=null,因为后面还需要用到.
						}
						recorderFiles.add(new VideoSegment(currentSegDuration, currentSegVideoFile));
						currentSegVideoFile=null;
						currentSegDuration=0;
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						semp.release();
					}
				}
			}
		});
		endEncoderThread.start();
	}
	private void waitEncoderEnd()
	{
		if(currentSegVideoFile!=null && endEncoderThread!=null){
			try {
				endEncoderThread.join();
				endEncoderThread=null;
			} catch (Exception e) {
				//ignore
			}
		}
	}
  /**
   * 停止当前的录制,并创建一个新的mp4文件, 用来合成. 
   * 如果之前已经合成过文件, 则删除之前的文件,重新合成.
   * 
   * @return 返回合成后的文件路径字符串.
   */
	public String stopRecord() {
		
		waitEncoderEnd();
		
		if (mIsRecording.get()) {
			pauseRecord();
		}
		
		if(SDKFileUtils.fileExist(finalVideoFile)){  //如果已经创建,则删除之前的.
			SDKFileUtils.deleteFile(finalVideoFile);
		}
		String cancatFile = SDKFileUtils.createMp4FileInBox();
		VideoEditor editor=new VideoEditor();
		String[] tsArray=new String[recorderFiles.size()];  
		for (int i=0;i<recorderFiles.size();i++) {
			VideoSegment item=recorderFiles.get(i);
			tsArray[i]=item.getName();
		}
		editor.executeConvertTsToMp4(tsArray, cancatFile);
		
		
		if(SDKFileUtils.fileExist(cancatFile))
		{
			if(mEncoder.mediaRorateDegree!=0){
				finalVideoFile = SDKFileUtils.createMp4FileInBox();
				editor.executeSetVideoMetaAngle(cancatFile, mEncoder.mediaRorateDegree, finalVideoFile);
			}else{
				finalVideoFile=cancatFile;
			}
			return finalVideoFile;
		}
		else
			return null;
	}

	public void release() {
		
		if (mIsRecording.get()) {
			pauseRecord();
		}
		
		if(cameraManager!=null){
			cameraManager.release();
		}
		
		clearSegment();
		videoFrameQ.clear();
		audioFrameQ.clear();
	}

	public void stopPreview() {
		if(cameraManager!=null){
			cameraManager.stopPreview();
		}
	}

	public boolean changeCamera() {
		
		if(supportRecordingChange())
		{
			cameraManager.changeCamera();
			cameraManager.updateParameters();
			segmentRecordListener.segmentCameraReady(previewSize);

			cameraManager.setPreviewDisplay(mSurfaceHolder);
//			cameraManager.setPreviewTexture(videoSurface);  //原来增加的.
//			int[] previewSize = cameraManager.getPreviewSize();
//			cameraManager.setPreviewCallBackWithBuffer(previewSize[0], previewSize[1], this);
			cameraManager.setPreviewCallBack(this);
			cameraManager.startPreview();
			return true;
		}
		else{
			Log.e(TAG,"current android version is not support change camera when recording.");
			return false;
		}
	}
	/**
	 * 在录制的过程中,是否支持旋转操作.
	 * 
	 * 默认是如果手机低于android5.0,并且在录制的过程中,则不支持旋转,其他则支持.
	 * @return
	 */
	public boolean supportRecordingChange()
	{
		if(firstSegmentDegree!=-1  && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){  //在录制中并且5.0+的系统
				 return true;
		}else if(firstSegmentDegree==-1 ){  //没有录制,则可以旋转.
			return true; 
		}else{
			return false;
		}
	}
	/**
	 * 当前录制的视频段.
	 * @return
	 */
	public int getSegmentSize()
	{
		return recorderFiles.size();
	}
	/**
	 * 
	 */
	public void deleteSegment() {
		if (recorderFiles.size() > 0) {
				VideoSegment se = recorderFiles.pollLast();
				
				lastSegmentsTotalTime -= se.during;
				
				SDKFileUtils.deleteFile(se.name);
				segmentRecordListener.segmentProgress(lastSegmentsTotalTime);
		}
		
		if(recorderFiles.size()==0){
			firstSegmentDegree=-1;
		}
	}
	
	public void clearSegment() {
		if (recorderFiles.size() > 0) {
			
			for(VideoSegment item: recorderFiles){
				SDKFileUtils.deleteFile(item.getName());
			}
			
			recorderFiles.clear();
			lastSegmentsTotalTime=0;
			currentSegDuration=0;
			
			videoFrameQ.clear();
			audioFrameQ.clear();
			segmentRecordListener.segmentProgress(0);
		}
	}
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		try {
			
			//把NV21格式的图像数据转换为YUV420P
			if (mIsRecording.get()) {
				
				long now = System.currentTimeMillis();
				byte[] dataCopy = new byte[data.length];
				System.arraycopy(data, 0, dataCopy, 0, data.length);
				
				if(currentSegmentStartTime==0){
					currentSegmentStartTime=now;
					currentSegDuration=0;
				}else{
					currentSegDuration = now - currentSegmentStartTime;
				}
				
				videoFrameQ.add(new OFrame(currentSegDuration, dataCopy));
				
				segmentRecordListener.segmentProgress((currentSegDuration+lastSegmentsTotalTime));
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private LinkedBlockingQueue<OFrame> videoFrameQ = new LinkedBlockingQueue<OFrame>();

	
	private LinkedBlockingQueue<OFrame> audioFrameQ = new LinkedBlockingQueue<OFrame>();

	class AudioSampleThread extends Thread {

		int limitCnt=0;
		@Override
		public void run() {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
			int sampleAudioRateInHz = 44100;
			
			int minBufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
			
			
			int bufferSize = minBufferSize<= 6114 ? 6114 : minBufferSize;  //这里最小是6114
			
			audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleAudioRateInHz, 
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT, bufferSize
					);

			//等待音频初始化完毕.
			while (audioRecorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			ByteBuffer audioData = ByteBuffer.allocate(bufferSize);
			audioRecorder.startRecording();

				while (mIsRecording.get()) {
					
					audioData.position(0).limit(0);
					int bufferReadResult = audioRecorder.read(audioData.array(), 0, 2048); 
					
					audioData.limit(bufferReadResult);
					
					if (bufferReadResult > 0) {
						
						long ts = System.currentTimeMillis();
						
						byte[] dataCopy = new byte[bufferReadResult];
						
						System.arraycopy(audioData.array(), 0, dataCopy, 0, bufferReadResult);  
						
						audioFrameQ.add(new OFrame(ts, dataCopy));
					}
				}
				if (audioRecorder != null) {
					try {
						audioRecorder.stop();
						audioRecorder.release();
						audioRecorder = null;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
		}
	}


	class VideoEncodeThread extends Thread {

		@Override
		public void run() {
			while (mIsRecording.get()) 
			{
				//编码视频
				if (!videoFrameQ.isEmpty()) {
					OFrame v = videoFrameQ.poll();
					int degree=cameraManager.isUseBackCamera()?90:270;
					mEncoder.pushVideoData(v.data,previewSize[0],previewSize[1],degree,v.ts);
				}
				
				//编码音频.
				if (!audioFrameQ.isEmpty()) {
					OFrame v = audioFrameQ.poll();
					mEncoder.pushAudioData(v.data,v.ts);
				}
				
			}
			
			//最后的帧数
			while (!videoFrameQ.isEmpty()) {
				OFrame v = videoFrameQ.poll();
				int degree=cameraManager.isUseBackCamera()?90:270;
				mEncoder.pushVideoData(v.data,previewSize[0],previewSize[1],degree,v.ts);
			}
			mEncoder.release();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		cameraManager.openCamera();
		cameraManager.updateParameters();
		cameraManager.setPreviewDisplay(holder);
		previewSize = cameraManager.getPreviewSize();

		
		segmentRecordListener.segmentCameraReady(previewSize);

		cameraManager.setPreviewCallBack(this);
		cameraManager.startPreview();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if(cameraManager!=null){
			cameraManager.closeCamera();  //这里不要等于null
		}
		
		if(mEncoder!=null){
			mEncoder.release();  //这里	mBoxEnc不能等于null, 可能后面还会用到.
		}
	}

	public void setSegmentsRecordListener(OpenSegmentsRecordListener l) {
		this.segmentRecordListener = l;
	}

	class VideoSegment {
		long during;

		public VideoSegment(long during, String name) {
			super();
			this.during = during;
			this.name = name;
		}

		String name;

		public long getDuring() {
			return during;
		}

		public VideoSegment setDuring(long during) {
			this.during = during;
			return this;
		}

		public String getName() {
			return name;
		}

		public VideoSegment setName(String name) {
			this.name = name;
			return this;
		}
	}
	//--------------------------------------增加的类.
	public boolean flashEnable() {
		
		if(cameraManager!=null){
			return cameraManager.flashEnable();
		}else{
			return false;
		}
	}
	public boolean cameraChangeEnable(){
		if(cameraManager==null){
			return false;
		}
		
		return cameraManager.cameraChangeEnable();
	}
	public void doFocus(List<Camera.Area> focusList){
		if(cameraManager==null){
			return ;
		}
		
		cameraManager.doFocus(focusList);
	}
	public boolean supportFocus(){
		if(cameraManager==null){
			return false;
		}
		return cameraManager.supportFocus();
	}
	public boolean isFaceFront()
	{
		if(cameraManager!=null){
			return cameraManager.isFaceFront();
		}else{
			return false;
		}
	}
	public boolean isPreviewing() {
		if(cameraManager==null){
			return false;
		}
		
		return cameraManager.isPreviewing();
	}
	public boolean changeFlash() {
		
		if(cameraManager==null){
			return false;
		}
		
		return cameraManager.changeFlash();
	}
	public boolean isRecording()
	{
		return mIsRecording.get();
	}
}
