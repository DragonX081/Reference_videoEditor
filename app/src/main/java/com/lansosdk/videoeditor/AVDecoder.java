package com.lansosdk.videoeditor;

import android.util.Log;

import java.nio.IntBuffer;

/**
 *  注意: 此类采用的ffmpeg中的软解码来做的, 
 *  如果您感觉软解有点慢, 我们提供了异步线程解码的形式, 可以加速解码处理,请联系我们.
 *
 */
public class AVDecoder {

//	private static void saveBitmap(Bitmap stitchBmp) {
//		//Bitmap stitchBmp = Bitmap.createBitmap(480, 360, Bitmap.Config.ARGB_8888);
//// stitchBmp.copyPixelsFromBuffer(mGLRgbBuffer);
//	 		String str="png"+"YYY2.png";
//		  File f = new File("/sdcard/", str);
//		  if (f.exists()) {
//		   f.delete();
//		  }
//		  try {
//			   FileOutputStream out = new FileOutputStream(f);
//			   stitchBmp.compress(Bitmap.CompressFormat.PNG, 90, out);
//			   out.flush();
//			   out.close();
//		   Log.i("TAG", "已经保存到"+f.getPath());
//		  } catch (FileNotFoundException e) {
//		   // TODO Auto-generated catch block
//		   e.printStackTrace();
//		  } catch (IOException e) {
//		   // TODO Auto-generated catch block
//		   e.printStackTrace();
//		  }
//	}
	
	/**
	 * 
	 * @param filepath
	 * @return
	 */
		public static native long  decoderInit(String filepath);
		/**
		 * 解码一帧, 发送上去.  seekUS大于等于0, 说明要seek, 
		 * 注意:如果您设置了seek大于等于0, 因为视频编码原理是基于IDR刷新帧的, seek时会选择在你设置时间的最近前一个IDR刷新帧的位置,请注意!
		 *  
		 *  
		 * 这里只seek一次开始解码, 解码后直接把数据发送上去. 用decoderIsEnd来判断当前是否已经解码好.
		 * 
		 * 建议:如果您的需求每次都解码同一个视频,视频总帧数在20帧以下,并每帧的字节不是很大, 建议一次解码后, 用list保存起来,不用每次都解码同一个视频.
		 * 
		 * @param handle  当前文件的句柄,
		 * 
		 * @param seekUs  是否要seek, 大于等于0说明要seek, 
		 * 
		 * @param out  输出. 数组由外部创建, 创建时的大小应等于 视频的宽度*高度*4;
		 * 
		 *  注意: 此类采用的ffmpeg中的软解码来做的, 
		 *  如果您感觉软解有点慢, 我们提供了异步线程解码的形式, 可以加速解码处理,请联系我们.
		 * @return  返回的是当前帧的时间戳.单位是US  微秒
		 */
		public static native long decoderFrame(long handle,long seekUs,int[] out);
		
		/**
		 * 释放当前解码器.
		 * 
		 * @param handle
		 * @return
		 */
		public static native int decoderRelease(long handle);
		/**
		 * 解码是否到文件尾.
		 * @param handle
		 * @return
		 */
		public static native boolean decoderIsEnd(long handle);
		
		/**
		 * 临时为了获取一个bitmap图片,临时测试.
		 * @param src
		 */
		public static void testGetFirstOnekey(String src)
		{
			  	long decoderHandler=0;
			  	IntBuffer  mGLRgbBuffer;
			  	MediaInfo info=new MediaInfo(src);
			  	if(info.prepare())
			    {
			    	   decoderHandler= AVDecoder.decoderInit(src);
			    	   if(decoderHandler!=0)
			    	   {
			    		   mGLRgbBuffer = IntBuffer.allocate(info.vWidth * info.vHeight);
			    			long  beforeDraw=System.currentTimeMillis();
			    			mGLRgbBuffer.position(0);
		    				AVDecoder.decoderFrame(decoderHandler, -1, mGLRgbBuffer.array());
		    				Log.i("TIME","draw comsume time is :"+ (System.currentTimeMillis() - beforeDraw));
		    				AVDecoder.decoderRelease(decoderHandler);
		    				
		    				//转换为bitmap
//		    				Bitmap stitchBmp = Bitmap.createBitmap(info.vWidth , info.vHeight, Bitmap.Config.ARGB_8888);
//		    				 stitchBmp.copyPixelsFromBuffer(mGLRgbBuffer);
//		    				 saveBitmap(stitchBmp); //您可以修改下, 然后返回bitmap
		    				//这里得到的图像在mGLRgbBuffer中, 可以用来返回一张图片.
		    				decoderHandler=0;
			    	   }
			 }else{
				 Log.e("TAG","get first one key error!");
			 }
		}
		/**
		 * 代码测试.
		 *
		 *测试证明, 是可以多个线程同时解码的.
		 *private void testDecoder()
		{
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					testDecoder2("/sdcard/480x480.mp4","/sdcard/480x480.yuv");
				}
			}).start();
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					testDecoder2("/sdcard/ping20s.mp4","/sdcard/ping20s.yuv");
				}
			}).start();
		}
		private void testDecoder2(String src,String dst)
		{
			   long decoderHandler;
			   IntBuffer  mGLRgbBuffer;
			   String gifPath=src;
			   MediaInfo  gifInfo=new MediaInfo(gifPath);
			       if(gifInfo.prepare())
			       {
			    	   decoderHandler=AVDecoder.decoderInit(gifPath);
			    	   FileWriteUtls  write=new FileWriteUtls(dst);
			    	   mGLRgbBuffer = IntBuffer.allocate(gifInfo.vWidth * gifInfo.vHeight);
			    	   while(AVDecoder.decoderIsEnd(decoderHandler)==false)
			    	   {
			    			mGLRgbBuffer.position(0);
		    				AVDecoder.decoderFrame(decoderHandler, -1, mGLRgbBuffer.array());
		    				mGLRgbBuffer.position(0);
		    				write.writeFile(mGLRgbBuffer);
			    	   }
			    	   write.closeWriteFile();
			    	   Log.i(TAG,"write closeEEEE!");
			       }
			}
		 */
}
