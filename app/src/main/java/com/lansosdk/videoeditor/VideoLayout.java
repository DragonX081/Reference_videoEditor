package com.lansosdk.videoeditor;

import android.graphics.Path;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 视频布局
 * 把多个视频画面,并列放在一起, 合成一个画面, 当前默认是以最长的视频为准, 如果有短的视频,则短视频停止到最后一帧;
 * 
 * 支持软编码和硬件编码; 
 * 建议合成后的视频分辨率不要大于720*1280;
 * 
 * TODO:此方法可能临时使用,后期会合并到VideoEditor.java文件中.
 */
public class VideoLayout extends VideoEditor {

    private static final String TAG = "VideoEditCustom";

    /**
     * 是否使用软件解码器
     */
    public static boolean isUseSoftDecoder=true;
    /**
     * 是否使用硬件编码器, 默认是使用.
     * 注意这个是全局变量, 设置一次后, 一直有效;
     */
    public static boolean isUseSoftEncoder=true;
    public static String  version="20180419V1";
    /**
     * 两个视频合并;
     * <p>
     * 原理是:  设置一个输出视频画面的宽度和高度, 认为是一个区域, 然后把一个一个的输入视频完整的放到这个区域里; 区域的X,Y坐标是一个一个的像素点;
     * 比如设置宽度是1280, 宽度是720的一个区域; 则第一个视频A的宽高是640x360,开始坐标是0,0,则放到区域的左上角;
     * <p>
     * 如果想第二个视频B放到第一个视频的下面,则B的x坐标和A视频的x坐标一致,Y坐标是A视频的高度; 如果想第二个视频和第一个视频有一定的间隔,则Y就等于A视频的高度+几个像素;
     * 如果想第三个视频C放到第一个视频的坐标,则C的Y坐标和A视频的Y坐标一致, x坐标是A视频的宽度; 如果中间有间隔,则x等于A视频的宽度+几个像素;
     * <p>
     * 注意,代码里没有做 每个视频是否存在的判断, 没有做宽度和高度判断;
     *
     * @param outW
     * @param outH
     * @param v1          第一个视频的完整路径
     * @param v1X         把第一个视频放到区域的哪个坐标 XY
     * @param v1Y
     * @param v2          第二个视频的完整路径
     * @param v2X         把第二个视频放到区域的哪个坐标上; xy坐标;
     * @param v2Y
     * @param dstDuration 视频拼接后的最后长度, 用MediaInfo可以得到每个视频的长度, 可以设置为最短长度, 也可以设置几个视频的最大长度;
     * @param dstPath     拼接后的目标视频保存路径, 后缀是mp4格式, 内部采用h264 的硬件编码;
     * @return
     */
    public int executeOverlay2Video(int outW, int outH,
                                    String v1, int v1X, int v1Y,
                                    String v2, int v2X, int v2Y,
                                    float dstDuration, String dstPath) {
        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS,pad=%d:%d:%d:%d:White [tmp1]; [1:v] setpts=PTS-STARTPTS [upperright]; [tmp1][upperright] overlay=x=%d:y=%d", outW, outH, v1X, v1Y, v2X, v2Y);

        List<String> cmdList = new ArrayList<String>();

        if(!isUseSoftDecoder){
        	  cmdList.add("-vcodec");
              cmdList.add("lansoh264_dec");
        }
      

        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));


        cmdList.add("-vcodec");
        if(isUseSoftEncoder){
            cmdList.add("libx264");
        }else{
            cmdList.add("lansoh264_enc");
            cmdList.add("-pix_fmt");
            cmdList.add(getColorFormat());
        }

        cmdList.add("-b:v");
        cmdList.add(checkBitRate(3 * 1024 * 1024));

        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }

    public int executeOverlayScale2Video(int outW, int outH,
                                         VideoLayoutParam p1,
                                         VideoLayoutParam p2,
                                         String dstPath) {

        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS, scale=%dx%d,pad=%d:%d:%d:%d:White [tmp1]; [1:v] setpts=PTS-STARTPTS, scale=%dx%d [upperright]; [tmp1][upperright] overlay=x=%d:y=%d", p1.scaleW, p1.scaleH,outW, outH, p1.x, p1.y,  p2.scaleW, p2.scaleH, p2.x, p2.y);

        float duration = getMaxDuration(Arrays.asList(p1.video, p2.video));


        List<String> cmdList = new ArrayList<String>();

        if(!isUseSoftDecoder){
      	  cmdList.add("-vcodec");
      	  cmdList.add("lansoh264_dec");
        }
        cmdList.add("-i");
        cmdList.add(p1.video);

        cmdList.add("-i");
        cmdList.add(p2.video);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(duration));


        cmdList.add("-vcodec");
        if(isUseSoftEncoder){
            cmdList.add("libx264");
        }else{
            cmdList.add("lansoh264_enc");
            cmdList.add("-pix_fmt");
            cmdList.add(getColorFormat());
        }
        cmdList.add("-b:v");
        cmdList.add(checkBitRate(3 * 1024 * 1024));

        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }

    /**
     * 3个视频合并
     * 原理见 {@link #executeOverlay2Video(int, int, String, int, int, String, int, int, float, String)}
     * @return
     */
    public int executeOverlay3Video(int outW, int outH,
                                    String v1, int v1X, int v1Y,
                                    String v2, int v2X, int v2Y,
                                    String v3, int v3X, int v3Y,
                                    float dstDuration, String dstPath) {
        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS,pad=%d:%d:%d:%d:White [tmp1]; [1:v] setpts=PTS-STARTPTS [upperright];[2:v] setpts=PTS-STARTPTS [lowerleft]; [tmp1][upperright] overlay=x=%d:y=%d [tmp2]; [tmp2][lowerleft] overlay=x=%d:y=%d", outW, outH, v1X, v1Y, v2X, v2Y, v3X, v3Y);
        List<String> cmdList = new ArrayList<String>();

        if(!isUseSoftDecoder){
      	  cmdList.add("-vcodec");
      	  cmdList.add("lansoh264_dec");
        }	
        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-i");
        cmdList.add(v3);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));


        cmdList.add("-vcodec");
        if(isUseSoftEncoder){
            cmdList.add("libx264");
        }else{
            cmdList.add("lansoh264_enc");
            cmdList.add("-pix_fmt");
            cmdList.add(getColorFormat());
        }
        cmdList.add("-b:v");
        cmdList.add(checkBitRate(4 * 1024 * 1024));


        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }

    public int executeOverlayScale3Video(int outW, int outH,
                                         VideoLayoutParam p1,
                                         VideoLayoutParam p2,
                                         VideoLayoutParam p3,
                                         String dstPath) {
        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS, scale=%dx%d,pad=%d:%d:%d:%d:White [tmp1]; [1:v] setpts=PTS-STARTPTS, scale=%dx%d [upperright];[2:v] setpts=PTS-STARTPTS, scale=%dx%d [lowerleft]; [tmp1][upperright] overlay=x=%d:y=%d [tmp2]; [tmp2][lowerleft] overlay=x=%d:y=%d", p1.scaleW, p1.scaleH,outW, outH, p1.x, p1.y,  p2.scaleW, p2.scaleH, p3.scaleW, p3.scaleH, p2.x, p2.y, p3.x, p3.y);

        List<String> cmdList = new ArrayList<String>();

        float duration = getMaxDuration(Arrays.asList(p1.video, p2.video, p3.video));

        if(!isUseSoftDecoder){
        	  cmdList.add("-vcodec");
        	  cmdList.add("lansoh264_dec");
        }	

        cmdList.add("-i");
        cmdList.add(p1.video);

        cmdList.add("-i");
        cmdList.add(p2.video);

        cmdList.add("-i");
        cmdList.add(p3.video);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(duration));

        cmdList.add("-vcodec");
        if(isUseSoftEncoder){
            cmdList.add("libx264");
        }else{
            cmdList.add("lansoh264_enc");
            cmdList.add("-pix_fmt");
            cmdList.add(getColorFormat());
        }
        cmdList.add("-b:v");
        cmdList.add(checkBitRate(4 * 1024 * 1024));

        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }

    /**
     * 4个视频的合并
     * 原理见 {@link #executeOverlay2Video(int, int, String, int, int, String, int, int, float, String)}
     */
    public int executeOverlay4Video(int outW, int outH,
                                    String v1, int v1X, int v1Y,
                                    String v2, int v2X, int v2Y,
                                    String v3, int v3X, int v3Y,
                                    String v4, int v4X, int v4Y,
                                    float dstDuration, String dstPath) {
        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS,pad=%d:%d:%d:%d:White [tmp1]; [1:v] setpts=PTS-STARTPTS [upperright];[2:v] setpts=PTS-STARTPTS [lowerleft];[3:v] setpts=PTS-STARTPTS [lowerright]; [tmp1][upperright] overlay=x=%d:y=%d [tmp2]; [tmp2][lowerleft] overlay=x=%d:y=%d [tmp3]; [tmp3][lowerright] overlay=x=%d:y=%d", outW, outH, v1X, v1Y, v2X, v2Y, v3X, v3Y, v4X, v4Y);

        List<String> cmdList = new ArrayList<String>();

        if(!isUseSoftDecoder){
        	  cmdList.add("-vcodec");
        	  cmdList.add("lansoh264_dec");
          }	

        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-i");
        cmdList.add(v3);

        cmdList.add("-i");
        cmdList.add(v4);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));

        cmdList.add("-vcodec");
        if(isUseSoftEncoder){
            cmdList.add("libx264");
        }else{
            cmdList.add("lansoh264_enc");
            cmdList.add("-pix_fmt");
            cmdList.add(getColorFormat());
        }
        cmdList.add("-b:v");
        cmdList.add(checkBitRate(4 * 1024 * 1024));


        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }
    public int executeOverlayScale4Video(int outW, int outH,
                                    VideoLayoutParam p1,
                                    VideoLayoutParam p2,
                                    VideoLayoutParam p3,
                                    VideoLayoutParam p4,
                                         String dstPath)
    {

        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS, scale=%dx%d, pad=%d:%d:%d:%d:White [tmp1]; [1:v] setpts=PTS-STARTPTS, scale=%dx%d [upperright];[2:v] setpts=PTS-STARTPTS, scale=%dx%d [lowerleft];[3:v] setpts=PTS-STARTPTS, scale=%dx%d [lowerright]; [tmp1][upperright] overlay=x=%d:y=%d [tmp2]; [tmp2][lowerleft] overlay=x=%d:y=%d [tmp3]; [tmp3][lowerright] overlay=x=%d:y=%d", p1.scaleW,p1.scaleH,outW, outH,
                p1.x,p1.y,p2.scaleW,p2.scaleH,p3.scaleW,p3.scaleH,p4.scaleW,p4.scaleH,p2.x,p2.y,p3.x,p3.y,p4.x,p4.y);

        List<String> cmdList = new ArrayList<String>();

        float dstDuration=getMaxDuration(Arrays.asList(p1.video,p2.video,p3.video,p4.video));

        if(!isUseSoftDecoder){
        	  cmdList.add("-vcodec");
        	  cmdList.add("lansoh264_dec");
          }	

        cmdList.add("-i");
        cmdList.add(p1.video);

        cmdList.add("-i");
        cmdList.add(p2.video);

        cmdList.add("-i");
        cmdList.add(p3.video);

        cmdList.add("-i");
        cmdList.add(p4.video);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));


        cmdList.add("-vcodec");
        if(isUseSoftEncoder){
            cmdList.add("libx264");
        }else{
            cmdList.add("lansoh264_enc");
            cmdList.add("-pix_fmt");
            cmdList.add(getColorFormat());
        }
        cmdList.add("-b:v");
        cmdList.add(checkBitRate(4 * 1024 * 1024));


        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }

    /**
     * 5个视频拼接
     * 原理请见 {@link #executeOverlay2Video(int, int, String, int, int, String, int, int, float, String)}
     */
    public int executeOverlay5Video(int outW, int outH,
                                    String v1, int v1X, int v1Y,
                                    String v2, int v2X, int v2Y,
                                    String v3, int v3X, int v3Y,
                                    String v4, int v4X, int v4Y,
                                    String v5, int v5X, int v5Y,
                                    float dstDuration, String dstPath) {
        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS,pad=%d:%d:%d:%d:White [tmp1]; [1:v] setpts=PTS-STARTPTS [upperright];[2:v] setpts=PTS-STARTPTS [lowerleft];[3:v] setpts=PTS-STARTPTS [lowerright]; [4:v] setpts=PTS-STARTPTS [lastone]; [tmp1][upperright] overlay=x=%d:y=%d [tmp2]; [tmp2][lowerleft] overlay=x=%d:y=%d [tmp3]; [tmp3][lowerright] overlay=x=%d:y=%d [tmp4]; [tmp4][lastone] overlay=x=%d:y=%d", outW, outH, v1X, v1Y, v2X, v2Y, v3X, v3Y, v4X, v4Y, v5X, v5Y);

        List<String> cmdList = new ArrayList<String>();

        if(!isUseSoftDecoder){
        	  cmdList.add("-vcodec");
        	  cmdList.add("lansoh264_dec");
          }	

        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-i");
        cmdList.add(v3);

        cmdList.add("-i");
        cmdList.add(v4);

        cmdList.add("-i");
        cmdList.add(v5);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));


        cmdList.add("-vcodec");
        if(isUseSoftEncoder){
            cmdList.add("libx264");
        }else{
            cmdList.add("lansoh264_enc");
            cmdList.add("-pix_fmt");
            cmdList.add(getColorFormat());
        }
        cmdList.add("-b:v");
        cmdList.add(checkBitRate(4 * 1024 * 1024));


        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }

    public int executeOverlayScale5Video(int outW, int outH,
                                    VideoLayoutParam p1,
                                    VideoLayoutParam p2,
                                    VideoLayoutParam p3,
                                    VideoLayoutParam p4,
                                    VideoLayoutParam p5,
                                         String dstPath) {

        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS,scale=%dx%d,pad=%d:%d:%d:%d:White [tmp1]; [1:v] setpts=PTS-STARTPTS, scale=%dx%d [upperright];[2:v] setpts=PTS-STARTPTS, scale=%dx%d [lowerleft];[3:v] setpts=PTS-STARTPTS, scale=%dx%d [lowerright]; [4:v] setpts=PTS-STARTPTS, scale=%dx%d [lastone]; [tmp1][upperright] overlay=x=%d:y=%d [tmp2]; [tmp2][lowerleft] overlay=x=%d:y=%d [tmp3]; [tmp3][lowerright] overlay=x=%d:y=%d [tmp4]; [tmp4][lastone] overlay=x=%d:y=%d", p1.scaleW,p1.scaleH,outW, outH,p1.x,p1.y,p2.scaleW,p2.scaleH,p3.scaleW,p3.scaleH,p4.scaleW,p4.scaleH,p5.scaleW,p5.scaleH,p2.x,p2.y,p3.x,p3.y,p4.x,p4.y,p5.x,p5.y);
        List<String> cmdList = new ArrayList<String>();

        float dstDuration=getMaxDuration(Arrays.asList(p1.video,p2.video,p3.video,p4.video,p5.video));

        if(!isUseSoftDecoder){
        	  cmdList.add("-vcodec");
        	  cmdList.add("lansoh264_dec");
          }	

        cmdList.add("-i");
        cmdList.add(p1.video);

        cmdList.add("-i");
        cmdList.add(p2.video);

        cmdList.add("-i");
        cmdList.add(p3.video);

        cmdList.add("-i");
        cmdList.add(p4.video);

        cmdList.add("-i");
        cmdList.add(p5.video);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));


        cmdList.add("-vcodec");
        if(isUseSoftEncoder){
            cmdList.add("libx264");
        }else{
            cmdList.add("lansoh264_enc");
            cmdList.add("-pix_fmt");
            cmdList.add(getColorFormat());
        }
        cmdList.add("-b:v");
        cmdList.add(checkBitRate(4 * 1024 * 1024));


        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }
    /**
     * 6个视频拼接
     * 原理请见 {@link #executeOverlay2Video(int, int, String, int, int, String, int, int, float, String)}
     * @return
     */
    public int executeOverlay6Video(int outW, int outH,
                                    String v1, int v1X, int v1Y,
                                    String v2, int v2X, int v2Y,
                                    String v3, int v3X, int v3Y,
                                    String v4, int v4X, int v4Y,
                                    String v5, int v5X, int v5Y,
                                    String v6, int v6X, int v6Y,
                                    float dstDuration, String dstPath) {

        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS,pad=%d:%d:%d:%d:White [tmp1]; [1:v] setpts=PTS-STARTPTS [upperright];[2:v] setpts=PTS-STARTPTS [lowerleft];[3:v] setpts=PTS-STARTPTS [lowerright]; [4:v] setpts=PTS-STARTPTS [input4]; [5:v] setpts=PTS-STARTPTS [input5]; [tmp1][upperright] overlay=x=%d:y=%d [tmp2]; [tmp2][lowerleft] overlay=x=%d:y=%d [tmp3]; [tmp3][lowerright] overlay=x=%d:y=%d [tmp4]; [tmp4][input4] overlay=x=%d:y=%d [tmp5]; [tmp5][input5] overlay=x=%d:y=%d", outW, outH, v1X, v1Y, v2X, v2Y, v3X, v3Y, v4X, v4Y, v5X, v5Y, v6X, v6Y);

        List<String> cmdList = new ArrayList<String>();

        if(!isUseSoftDecoder){
        	  cmdList.add("-vcodec");
        	  cmdList.add("lansoh264_dec");
          }	

        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-i");
        cmdList.add(v3);

        cmdList.add("-i");
        cmdList.add(v4);

        cmdList.add("-i");
        cmdList.add(v5);

        cmdList.add("-i");
        cmdList.add(v6);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));


        cmdList.add("-vcodec");
        if(isUseSoftEncoder){
            cmdList.add("libx264");
        }else{
            cmdList.add("lansoh264_enc");
            cmdList.add("-pix_fmt");
            cmdList.add(getColorFormat());
        }
        cmdList.add("-b:v");
        cmdList.add(checkBitRate(4 * 1024 * 1024));


        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }

    public int executeOverlayScale6Video(int outW, int outH,
                                    VideoLayoutParam p1,
                                    VideoLayoutParam p2,
                                    VideoLayoutParam p3,
                                    VideoLayoutParam p4,
                                    VideoLayoutParam p5,
                                    VideoLayoutParam p6,
                                         String dstPath) {

        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS,scale=%dx%d,pad=%d:%d:%d:%d:White [tmp1]; [1:v] setpts=PTS-STARTPTS,scale=%dx%d [upperright];[2:v] setpts=PTS-STARTPTS,scale=%dx%d [lowerleft];[3:v] setpts=PTS-STARTPTS,scale=%dx%d [lowerright]; [4:v] setpts=PTS-STARTPTS,scale=%dx%d [input4]; [5:v] setpts=PTS-STARTPTS,scale=%dx%d [input5]; [tmp1][upperright] overlay=x=%d:y=%d [tmp2]; [tmp2][lowerleft] overlay=x=%d:y=%d [tmp3]; [tmp3][lowerright] overlay=x=%d:y=%d [tmp4]; [tmp4][input4] overlay=x=%d:y=%d [tmp5]; [tmp5][input5] overlay=x=%d:y=%d",
                p1.scaleW,p1.scaleH,outW, outH,p1.x,p1.y,p2.scaleW,p2.scaleH,p3.scaleW,p3.scaleH,p4.scaleW,p4.scaleH,p5.scaleW,p5.scaleH,p6.scaleW,p6.scaleH,p2.x,p2.y,p3.x,p3.y,p4.x,p4.y,p5.x,p5.y,p6.x,p6.y);

        List<String> cmdList = new ArrayList<String>();

        float dstDuration=getMaxDuration(Arrays.asList(p1.video,p2.video,p3.video, p4.video,p5.video,p6.video));

        if(!isUseSoftDecoder){
        	  cmdList.add("-vcodec");
        	  cmdList.add("lansoh264_dec");
          }	

        cmdList.add("-i");
        cmdList.add(p1.video);

        cmdList.add("-i");
        cmdList.add(p2.video);

        cmdList.add("-i");
        cmdList.add(p3.video);

        cmdList.add("-i");
        cmdList.add(p4.video);

        cmdList.add("-i");
        cmdList.add(p5.video);

        cmdList.add("-i");
        cmdList.add(p6.video);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));

        cmdList.add("-vcodec");
        if(isUseSoftEncoder){
            cmdList.add("libx264");
        }else{
            cmdList.add("lansoh264_enc");
            cmdList.add("-pix_fmt");
            cmdList.add(getColorFormat());
        }
        cmdList.add("-b:v");
        cmdList.add(checkBitRate(4 * 1024 * 1024));

        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }
    /**
     * 9个视频拼接
     * 原理见 {@link #executeOverlay2Video(int, int, String, int, int, String, int, int, float, String)}
     * @return
     */
    public int executeOverlay9Video(int outW, int outH,
                                    String v1, int v1X, int v1Y,
                                    String v2, int v2X, int v2Y,
                                    String v3, int v3X, int v3Y,
                                    String v4, int v4X, int v4Y,
                                    String v5, int v5X, int v5Y,
                                    String v6, int v6X, int v6Y,
                                    String v7, int v7X, int v7Y,
                                    String v8, int v8X, int v8Y,
                                    String v9, int v9X, int v9Y,
                                    float dstDuration, String dstPath) {

        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS,pad=%d:%d:%d:%d:White [in1]; [1:v] setpts=PTS-STARTPTS [in2];[2:v] setpts=PTS-STARTPTS [in3];[3:v] setpts=PTS-STARTPTS [in4]; [4:v] setpts=PTS-STARTPTS [in5]; [5:v] setpts=PTS-STARTPTS [in6]; [6:v] setpts=PTS-STARTPTS [in7];[7:v] setpts=PTS-STARTPTS [in8]; [8:v] setpts=PTS-STARTPTS [in9]; [in1][in2] overlay=x=%d:y=%d [tmp1]; [tmp1][in3] overlay=x=%d:y=%d [tmp2]; [tmp2][in4] overlay=x=%d:y=%d [tmp3];[tmp3][in5] overlay=x=%d:y=%d [tmp4]; [tmp4][in6] overlay=x=%d:y=%d [tmp5]; [tmp5][in7] overlay=x=%d:y=%d [tmp6]; [tmp6][in8] overlay=x=%d:y=%d [tmp7]; [tmp7][in9] overlay=x=%d:y=%d", outW, outH, v1X, v1Y, v2X, v2Y, v3X, v3Y, v4X, v4Y, v5X, v5Y, v6X, v6Y, v7X, v7Y, v8X, v8Y, v9X, v9Y);
        List<String> cmdList = new ArrayList<String>();

        if(!isUseSoftDecoder){
        	  cmdList.add("-vcodec");
        	  cmdList.add("lansoh264_dec");
         }	

        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-i");
        cmdList.add(v3);

        cmdList.add("-i");
        cmdList.add(v4);

        cmdList.add("-i");
        cmdList.add(v5);

        cmdList.add("-i");
        cmdList.add(v6);

        cmdList.add("-i");
        cmdList.add(v7);

        cmdList.add("-i");
        cmdList.add(v8);

        cmdList.add("-i");
        cmdList.add(v9);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));


        cmdList.add("-vcodec");
        if(isUseSoftEncoder){
            cmdList.add("libx264");
        }else{
            cmdList.add("lansoh264_enc");
            cmdList.add("-pix_fmt");
            cmdList.add(getColorFormat());
        }
        cmdList.add("-b:v");
        cmdList.add(checkBitRate(4 * 1024 * 1024));


        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        return executeVideoEditor(command);
    }
    public int executeOverlayScale9Video(int outW, int outH,
            VideoLayoutParam p1,
            VideoLayoutParam p2,
            VideoLayoutParam p3,
            VideoLayoutParam p4,
            VideoLayoutParam p5,
            VideoLayoutParam p6,
            VideoLayoutParam p7,
            VideoLayoutParam p8,
            VideoLayoutParam p9,
            String dstPath) {

    	String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS,scale=%dx%d,pad=%d:%d:%d:%d:White [in1]; [1:v] setpts=PTS-STARTPTS,scale=%dx%d [in2];[2:v] setpts=PTS-STARTPTS,scale=%dx%d [in3];[3:v] setpts=PTS-STARTPTS,scale=%dx%d [in4]; [4:v] setpts=PTS-STARTPTS,scale=%dx%d [in5]; [5:v] setpts=PTS-STARTPTS,scale=%dx%d [in6]; [6:v] setpts=PTS-STARTPTS,scale=%dx%d [in7];[7:v] setpts=PTS-STARTPTS,scale=%dx%d [in8]; [8:v] setpts=PTS-STARTPTS,scale=%dx%d [in9]; [in1][in2] overlay=x=%d:y=%d [tmp1]; [tmp1][in3] overlay=x=%d:y=%d [tmp2]; [tmp2][in4] overlay=x=%d:y=%d [tmp3];[tmp3][in5] overlay=x=%d:y=%d [tmp4]; [tmp4][in6] overlay=x=%d:y=%d [tmp5]; [tmp5][in7] overlay=x=%d:y=%d [tmp6]; [tmp6][in8] overlay=x=%d:y=%d [tmp7]; [tmp7][in9] overlay=x=%d:y=%d",
    			p1.scaleW,p1.scaleH,outW, outH,p1.x,p1.y,p2.scaleW,p2.scaleH,p3.scaleW,p3.scaleH,p4.scaleW,p4.scaleH,p5.scaleW,p5.scaleH,p6.scaleW,p6.scaleH,p7.scaleW,p7.scaleH,p8.scaleW,p8.scaleH,p9.scaleW,p9.scaleH,p2.x,p2.y,p3.x,p3.y,p4.x,p4.y,p5.x,p5.y,p6.x,p6.y,p7.x,p7.y,p8.x,p8.y,p9.x,p9.y);
    	
    	float duration=getMaxDuration(Arrays.asList(p1.video,p2.video,p3.video,p4.video,p5.video,p6.video,p7.video,p8.video,p9.video));
    	
		List<String> cmdList = new ArrayList<String>();
		
		if(!isUseSoftDecoder){
			cmdList.add("-vcodec");
			cmdList.add("lansoh264_dec");
		}	
		
		cmdList.add("-i");
		cmdList.add(p1.video);
		
		cmdList.add("-i");
		cmdList.add(p2.video);
		
		cmdList.add("-i");
		cmdList.add(p3.video);
		
		cmdList.add("-i");
		cmdList.add(p4.video);
		
		cmdList.add("-i");
		cmdList.add(p5.video);
		
		cmdList.add("-i");
		cmdList.add(p6.video);
		
		cmdList.add("-i");
		cmdList.add(p7.video);
		
		cmdList.add("-i");
		cmdList.add(p8.video);
		
		cmdList.add("-i");
		cmdList.add(p9.video);
		
		cmdList.add("-filter_complex");
		cmdList.add(filter);
		
		cmdList.add("-t");
		cmdList.add(String.valueOf(duration));
		
		cmdList.add("-vcodec");
		if(isUseSoftEncoder){
			cmdList.add("libx264");
		}else{
			cmdList.add("lansoh264_enc");
			cmdList.add("-pix_fmt");
			cmdList.add(getColorFormat());
		}
		cmdList.add("-b:v");
		cmdList.add(checkBitRate(4 * 1024 * 1024));
		
		
		cmdList.add("-y");
		cmdList.add(dstPath);
		String[] command = new String[cmdList.size()];
		for (int i = 0; i < cmdList.size(); i++) {
		command[i] = (String) cmdList.get(i);
		}
		return executeVideoEditor(command);
}
    /**
     * 合并音视频文件;
     * 内部无视频编码;
     * 2018年3月5日15:44:43增加,;
     *
     * @param audio  可以是m4a的音乐文件, 也可以是含有音频的mp4源视频.
     * @param video  含有视频轨道的文件, 可以是无声声音或有其他声音的视频; 如果视频中有声音,则声音会被替换
     * @param dstMp4 生成的目标文件, 后缀是mp4;
     * @return
     */
    public static int mergeAudioVideo(String audio, String video, String dstMp4) {
        MediaInfo info = new MediaInfo(audio, false);
        MediaInfo info2 = new MediaInfo(video, false);

        if (info.prepare() && info.isHaveAudio() && info2.prepare() && info2.isHaveVideo()) {
            //ffmpeg -i sync_test.mp4 -i syncf.mp4 -map 0:a -map 1:v -acodec copy -vcodec copy syncMap.mp4
            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-i");
            cmdList.add(audio);
            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-map");
            cmdList.add("0:a");
            cmdList.add("-map");
            cmdList.add("1:v");

            cmdList.add("-acodec");
            cmdList.add("copy");
            cmdList.add("-vcodec");
            cmdList.add("copy");

            cmdList.add("-y");
            cmdList.add(dstMp4);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            VideoEditor editor = new VideoEditor();
            return editor.executeVideoEditor(command);
        } else {
            Log.w(TAG, "old mp4 file prepare error!!,do not add audio");
            return -1;
        }
    }

    private float getMaxDuration(List<String> array) {
        float retDuration = 0;
        for (String str : array) {
            MediaInfo info = new MediaInfo(str, false);
            if (info.prepare() && info.isHaveVideo()) {
                if (retDuration <= info.vDuration) {
                    retDuration = info.vDuration;
                }
            }
        }
        return retDuration;
    }
}
