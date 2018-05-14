package com.lansosdk.videoeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

@SuppressWarnings("deprecation")
public class OCameraManager {
	private Camera camera = null;
	private boolean isPreviewOn = false;
	private int cameraFacingType = CameraInfo.CAMERA_FACING_BACK;
	private Activity activity;
	/**
	 * 默认屏幕旋转角度,让前后拍摄的视频角度一致
	 */
//	private int defaultScreenResolution = -1;

	private int previewSize[] = new int[2];
	private int rotateDegree=0;
	
	private int mDesireWidth,mDesireHeight;
	public OCameraManager(Activity activity,int desireW,int desireH) {
		super();
		this.activity = activity;
		mDesireWidth=desireW;
		mDesireHeight=desireH;
	}

	Thread startThread;

	public void openCamera() {
		startThread = new Thread(new Runnable() {
			@Override
			public void run() {
				int n = Camera.getNumberOfCameras();
				if (n > 1) {
					for (int i = 0; i < n; i++) {
						CameraInfo info = new CameraInfo();
						Camera.getCameraInfo(i, info);
						if (info.facing == cameraFacingType) {
							camera = Camera.open(i);
							break;
						}
					}
				} else {
					camera = Camera.open();
					cameraFacingType = CameraInfo.CAMERA_FACING_BACK;
				}
			}
		});
		startThread.start();
	}

	public boolean flashEnable() {
		return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
				&& cameraFacingType == CameraInfo.CAMERA_FACING_BACK;

	}

	private void startThreadJoin() {
		if (startThread != null) {
			try {
				startThread.join();
			} catch (Exception e) {
				//ignore
			}
		}
	}

	public void setPreviewDisplay(SurfaceHolder holder) {
		try {
			startThreadJoin();
			camera.setPreviewDisplay(holder);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setPreviewTexture(SurfaceTexture surfaceTexture) {
		try {
			startThreadJoin();
			camera.setPreviewTexture(surfaceTexture);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setPreviewCallBack(PreviewCallback callback) {
		camera.setPreviewCallback(callback);
	}

	boolean previewing = false;

	public boolean isPreviewing() {
		return previewing;
	}

	public boolean cameraChangeEnable() {
		return Camera.getNumberOfCameras() > 1;
	}

	public void setPreviewCallBackWithBuffer(int previewWidth, int previewHeight, PreviewCallback callback) {
		int buffersize = previewWidth * previewHeight * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
		byte[] previewBuffer = new byte[buffersize];
		startThreadJoin();
		camera.addCallbackBuffer(previewBuffer);
		camera.setPreviewCallbackWithBuffer(callback);
	}

	public void startPreview() {
		if (!isPreviewOn && camera != null) {
			isPreviewOn = true;
			startThreadJoin();
			camera.startPreview();
			previewing = true;
		}
	}

	public void stopPreview() {
		if (isPreviewOn && camera != null) {
			isPreviewOn = false;
			camera.stopPreview();
			previewing = false;
		}
	}

	public void closeCamera() {
		if (camera != null) {
			stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
		}
	}

	public void release() {
		if (camera != null) {
			stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
		}
	}
	public boolean supportFocus() {
		if(camera!=null){
			Parameters mParameters = camera.getParameters();
			Log.i("SEGment","mParameters.getMaxNumFocusAreas() :"+mParameters.getMaxNumFocusAreas() );
			return (mParameters.getMaxNumFocusAreas() > 0 ? true : false);
		}else{
			return false;
		}
	}

	public boolean isUseBackCamera() {
		if(camera==null){
			return false;
		}
		startThreadJoin();
		
		return cameraFacingType == CameraInfo.CAMERA_FACING_BACK;
	}

	public boolean isUseFrontCamera() {
		if(camera==null){
			return false;
		}
		startThreadJoin();
		return cameraFacingType == CameraInfo.CAMERA_FACING_FRONT;
	}

	public void useBackCamera() {
		
		startThreadJoin();
		cameraFacingType = CameraInfo.CAMERA_FACING_BACK;
	}

	public void useFrontCamera() {
		startThreadJoin();
		cameraFacingType = CameraInfo.CAMERA_FACING_FRONT;
	}
	public void changeCamera() {
		startThreadJoin();
		if (cameraFacingType == CameraInfo.CAMERA_FACING_BACK) {
			useFrontCamera();
		} else if (cameraFacingType == CameraInfo.CAMERA_FACING_FRONT) {
			useBackCamera();
		}
		closeCamera();
		openCamera();
	}
	
	public int getPreviewDataDegress()
	{
		if(rotateDegree==90){
			if (cameraFacingType == CameraInfo.CAMERA_FACING_BACK) {
				return 90;
			} else if (cameraFacingType == CameraInfo.CAMERA_FACING_FRONT) {
				return 270;
			}
		}
		return rotateDegree;
	}
	public boolean isFaceFront()
	{
		return cameraFacingType == CameraInfo.CAMERA_FACING_FRONT;
	}
	public void updateParameters() {
		startThreadJoin();
		
		Parameters camParams = camera.getParameters();
		Parameters parameters = camera.getParameters();
		List<Size> cs = parameters.getSupportedPreviewSizes();
		
		
		int desireWidth,desireHeight;
		desireWidth=mDesireWidth;
		desireHeight=mDesireHeight;
		
		if (cs != null && cs.size() > 0) {
			
			//先把分辨率从小到大排列.
			Collections.sort(cs, new ResolutionComparator());
			rotateDegree=determineDisplayOrientation(activity, cameraFacingType);
			
			
			
			if(rotateDegree==90 || rotateDegree==270){  //竖屏的话, 用户设置过来宽度和高度调换了
				desireWidth=mDesireHeight;
				desireHeight=mDesireWidth;
			}
			Size preSizeOld = null;
				
				//打印支持的分辨率!!!
				for (int i = 0; i < cs.size(); i++) {
					Size size = cs.get(i);
					Log.i("TAG","min==>max:support size:"+size.width+" x "+size.height);
				}	
				
				//找大于希望的分辨率.
				for (int i = 0; i < cs.size(); i++) {
					Size size = cs.get(i);
					if (size != null && size.width >= desireWidth && size.height >= desireHeight) {
						preSizeOld = size;
						break;
					}
				}
				
				
				if (preSizeOld==null) {
					int mediumResolution = cs.size() / 2;
					if (mediumResolution >= cs.size()) {
						mediumResolution = cs.size() - 1;
					}
					preSizeOld = cs.get(mediumResolution);
				}
				
				Size preSizeNew = null;
				
			//--------------------------另一种获取的方式:
				int w,h;
		        if(desireHeight>desireWidth){  //竖屏, 则调过来.
		        	w=desireHeight;
		        	h=desireWidth;
		        }else{
		        	w=desireWidth;
		        	h=desireHeight;
		        }
		    	findCameraSupportValue(w,h);
		    	preSizeNew= getOptimalPreviewSize(camParams,mCameraPictureSize,w);
//		    	 Log.i("TAG","preSizeNew:"+preSizeNew.width+"x"+preSizeNew.height);
		    	
		    
		    	if(preSizeNew !=null && preSizeOld!=null)
		    	{
		    		//如果两个都大于设置的宽度,则找最小的, 不然找最大的.
		    		if(preSizeNew.width>w && preSizeOld.width>w)
		    		{
		    			if(preSizeNew.width< preSizeOld.width){  
			    			previewSize[0] = preSizeNew.width;
							previewSize[1] = preSizeNew.height;
			    		}else{
			    			previewSize[0] = preSizeOld.width;
							previewSize[1] = preSizeOld.height;
			    		}
		    		}
		    		else{
		    			if(preSizeNew.width> preSizeOld.width){  
			    			previewSize[0] = preSizeNew.width;
							previewSize[1] = preSizeNew.height;
			    		}else{
			    			previewSize[0] = preSizeOld.width;
							previewSize[1] = preSizeOld.height;
			    		}
		    		}
		    	}else if(preSizeOld!=null) {
		    		previewSize[0] = preSizeOld.width;
					previewSize[1] = preSizeOld.height;
		    	}
				Log.i("TAG","last got preview size:"+previewSize[0]+" x "+previewSize[1]);
				camParams.setPreviewSize(previewSize[0], previewSize[1]);
			}
		
			camParams.setPreviewFrameRate(25);//gzj++
		
			rotateDegree=determineDisplayOrientation(activity, cameraFacingType);
			camera.setDisplayOrientation(rotateDegree);
			
			List<String> focusModes = camParams.getSupportedFocusModes();
			if (cameraFacingType == CameraInfo.CAMERA_FACING_BACK && focusModes != null) {//  fix
				Log.i("video", Build.MODEL);
				if (((Build.MODEL.startsWith("GT-I950")) || (Build.MODEL.endsWith("SCH-I959")) || (Build.MODEL
					.endsWith("MEIZU MX3"))) && focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
					camParams.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				} else if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
					camParams.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				} else {
					camParams.setFocusMode(Parameters.FOCUS_MODE_FIXED);
				}
			}
			
		camera.setParameters(camParams);
	}
	//----------------------------------------
    private CameraPictureSizeComparator mCameraPictureSizeComparator =
            new CameraPictureSizeComparator();
    public Size mCameraPictureSize=null;
	private void findCameraSupportValue(int desiredWidth,int desiredHeight) 
    {
    	
        Parameters cp = camera.getParameters();
        List<Size> cs = cp.getSupportedPictureSizes();
        if (cs != null && !cs.isEmpty()) 
        {
            Collections.sort(cs, mCameraPictureSizeComparator);
            for (Size size : cs)
            {
                if (size.width < desiredWidth && size.height < desiredWidth) {
                	if(mCameraPictureSize==null){
                		mCameraPictureSize=size;
                	}
                    break;
                }
                float ratio = (float) size.width /(float) size.height;
                float disireRatio=(float) desiredWidth/(float)desiredHeight;
                if(desiredWidth<desiredHeight){  //因为分辨率都是宽度大于高度的, 这里如果是竖屏,则把宽高调过来.
                	disireRatio=(float) desiredHeight/(float)desiredWidth;
                }
               // Log.i(TAG,"desiredWidth:"+desiredWidth+" desiredHeight:"+desiredHeight+" disireRatio"+disireRatio);
                if (ratio == disireRatio) 
                {
                    mCameraPictureSize = size;
                }
            }
        }
    }
	public void doFocus(List<Camera.Area> focusList) {
		Parameters param = camera.getParameters();
		param.setFocusAreas(focusList);
		param.setMeteringAreas(focusList);//这个只能这样设置, 不然的话, 聚焦不起作用.
		try {
			camera.setParameters(param);
		} catch (Exception e) {
			camera.autoFocus(null);
		}
	}

	public boolean changeFlash() {
		boolean flashOn = false;
		if (flashEnable()) {
			Parameters params = camera.getParameters();
			if (Parameters.FLASH_MODE_TORCH.equals(params.getFlashMode())) {
				params.setFlashMode(Parameters.FLASH_MODE_OFF);
				flashOn = false;
			} else {
				params.setFlashMode(Parameters.FLASH_MODE_TORCH);
				flashOn = true;
			}
			camera.setParameters(params);
		}
		return flashOn;
	}

	public void closeFlash() {
		if (flashEnable()) {
			Parameters params = camera.getParameters();
			if (Parameters.FLASH_MODE_TORCH.equals(params.getFlashMode())) {
				params.setFlashMode(Parameters.FLASH_MODE_OFF);
				camera.setParameters(params);
			}
		}
	}

	public Camera getCamera() {
		startThreadJoin();

		return camera;
	}

	public int[] getPreviewSize() {
		startThreadJoin();

		return previewSize;
	}
	public int getRotateDegree()
	{
		return rotateDegree;
	}

	private int determineDisplayOrientation(Activity activity, int defaultCameraId) {
		int displayOrientation = 0;
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
			CameraInfo cameraInfo = new CameraInfo();
			Camera.getCameraInfo(defaultCameraId, cameraInfo);

			int degrees = getRotationAngle(activity);
			
			
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
				displayOrientation = (cameraInfo.orientation + degrees) % 360;
				displayOrientation = (360 - displayOrientation) % 360;
			} else {
				displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
			}
		}
		return displayOrientation;
	}

	public static int getRotationAngle(Activity activity) {
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;

		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		return degrees;
	}

	class ResolutionComparator implements Comparator<Size> {
		@Override
		public int compare(Size size1, Size size2) {
			if (size1.height != size2.height) {
				return size1.height - size2.height;
			} else {
				return size1.width - size2.width;
			}
		}
	}
	private class CameraPictureSizeComparator implements Comparator<Size> {

	    // 拍照尺寸从大到小，优先获取较大尺寸!!!  注意这里是从大到小排列.
	    public int compare(Size size1, Size size2) {
	        return size2.width - size1.width;
	    }
	}
	 private Size getOptimalPreviewSize(Parameters parameters,
             Size pictureSize, int viewHeight) {

			if (parameters == null || pictureSize == null) {
			return null;
			}
			
			List<Size> sizes = parameters.getSupportedPreviewSizes();
			//从小到大排列.
			Collections.sort(sizes, new CameraPreviewSizeComparator());
			final double ASPECT_TOLERANCE = 0.05;
			double targetRatio = (double) pictureSize.width / pictureSize.height;
			if (sizes == null) {
			return null;
			}
			
			Size optimalSize = null;
			double minDiff = Double.MAX_VALUE;
			int targetHeight = pictureSize.height;
			// Try to find an size match aspect ratio and size
			for (Size size : sizes)
			{
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
			continue;
			}
			
			if (optimalSize != null && size.height > viewHeight) {
			break;
			}
			
			if (Math.abs(size.height - targetHeight) < minDiff) {
			optimalSize = size;
			minDiff = Math.abs(size.height - targetHeight);
			}
			}
			
			// Cannot find the one match the aspect ratio, ignore the requirement
			if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes)
			{
			if (Math.abs(size.height - targetHeight) < minDiff) {
			optimalSize = size;
			minDiff = Math.abs(size.height - targetHeight);
			}
			}
			}
			return optimalSize;
}
	 	private class CameraPreviewSizeComparator implements Comparator<Size> {
		
		// 预览尺寸从小到大，优先获取较小的尺寸
			public int compare(Size size1, Size size2) {
					return size1.width - size2.width;
			}
		}
}
