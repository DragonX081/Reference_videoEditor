package com.lansosdk.videoeditor;

import android.content.Context;

import com.lansosdk.box.ScaleExecute;
import com.lansosdk.box.onScaleCompletedListener;
import com.lansosdk.box.onScaleProgressListener;

public class VideoCompress {

    private Context context;
    private String srcVideo;
    private String dstVideo;
    private MediaInfo mediaInfo;
    private boolean isExecuting = false;
    private onVideoCompressCompletedListener compressCompletedListener;
    private onVideoCompressProgressListener progressListener;

    public  VideoCompress(Context ctx,String video){
        context=ctx;
        srcVideo=video;
    }
    public void setOnVideoCompressCompletedListener(onVideoCompressCompletedListener listener){
        compressCompletedListener=listener;
    }
    public void setOnVideoCompressProgressListener(onVideoCompressProgressListener listener){
        progressListener=listener;
    }

    public boolean start() {
        if (isExecuting)
            return false;

        mediaInfo=new MediaInfo(srcVideo,false);
        if(mediaInfo.prepare()){
            isExecuting = true;

            dstVideo= SDKFileUtils.createMp4FileInBox();
            ScaleExecute vScale = new ScaleExecute(context, srcVideo);  //videoPath是路径
            vScale.setOutputPath(dstVideo);

            int scaleWidth=mediaInfo.getWidth();
            int scaleHeight=mediaInfo.getHeight();
            if(mediaInfo.vWidth* mediaInfo.vHeight==1280*720){

                if(mediaInfo.getWidth()>mediaInfo.getHeight()){
                    scaleWidth=960;
                    scaleHeight=544;
                }else {
                    scaleWidth=544;
                    scaleHeight=960;
                }
            }else if(mediaInfo.vWidth * mediaInfo.vHeight==1920*1080){
                if(mediaInfo.getWidth()>mediaInfo.getHeight()){
                    scaleWidth=960;
                    scaleHeight=544;
                }else {
                    scaleWidth=544;
                    scaleHeight=960;
                }
            }else if(mediaInfo.vWidth * mediaInfo.vHeight>1920*1080){
                scaleWidth=mediaInfo.getWidth()/2;
                scaleHeight=mediaInfo.getHeight()/2;
            }else if(mediaInfo.vWidth * mediaInfo.vHeight>1280*720){
                scaleWidth=mediaInfo.getWidth()/2;
                scaleHeight=mediaInfo.getHeight()/2;
            }else {
                scaleWidth=(int)(mediaInfo.getWidth()*0.7f);
                scaleHeight=(int)(mediaInfo.getHeight()*0.7f);
            }

            vScale.setScaleSize(scaleWidth, scaleHeight, getSuggestBitRate(scaleHeight * scaleWidth));

            //设置缩放进度监听.currentTimeUS当前处理的视频帧时间戳.
            vScale.setScaleProgessListener(new onScaleProgressListener() {

                @Override
                public void onProgress(ScaleExecute v, long currentTimeUS) {
                    if(progressListener!=null){
                        float time = (float) currentTimeUS / (float)(mediaInfo.vDuration*1000000);
                        int b = Math.round(time * 100);
                        progressListener.onProgress(b);
                    }
                }
            });

            //设置缩放进度完成后的监听.
            vScale.setScaleCompletedListener(new onScaleCompletedListener() {

                @Override
                public void onCompleted(ScaleExecute v) {
                    isExecuting = false;

                    if(compressCompletedListener!=null){

                        if (SDKFileUtils.fileExist(dstVideo) && mediaInfo.isHaveAudio()) {
                            String  retPath= SDKFileUtils.createMp4FileInBox();
                            int ret = LanSongMergeAV.mergeAVDirectly(srcVideo, dstVideo, retPath);
                            if (ret != 0) {
                                retPath = dstVideo;
                            }else{
                                SDKFileUtils.deleteFile(dstVideo);
                            }
                            compressCompletedListener.onCompleted(retPath);
                        }else{
                            compressCompletedListener.onCompleted(dstVideo);
                        }
                    }
                }
            });
            return vScale.start();
        }else {
            return false;
        }
    }
    public static int getSuggestBitRate(int wxh) {
        if (wxh <= 480 * 480) {
            return 1000 * 1024;
        } else if (wxh <= 640 * 480) {
            return 1500 * 1024;
        } else if (wxh <= 800 * 480) {
            return 1800 * 1024;
        } else if (wxh <= 960 * 544) {
            return 2000 * 1024;
        } else if (wxh <= 1280 * 720) {
            return 2500 * 1024;
        } else if (wxh <= 1920 * 1088) {
            return 3000 * 1024;
        } else {
            return 3500 * 1024;
        }
    }
}
