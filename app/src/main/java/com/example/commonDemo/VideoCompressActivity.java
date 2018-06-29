package com.example.commonDemo;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.lansoeditor.demo.R;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoCompress;
import com.lansosdk.videoeditor.onVideoCompressCompletedListener;
import com.lansosdk.videoeditor.onVideoCompressProgressListener;

public class VideoCompressActivity extends Activity{
    private  String videoPath = null;

    private String dstPath;
    private TextView tvBefore;
    private TextView tvAfter;
    private Button btnPreview;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.video_compress_layout);

        initView();
    }
    private void selectVideo(){
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, 66);
        tvBefore.setText("");
        tvBefore.setText("");
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 66 && resultCode == RESULT_OK && null != data) {
            Uri selectedVideo = data.getData();
            String[] filePathColumn = {MediaStore.Video.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedVideo,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            videoPath = cursor.getString(columnIndex);

            tvBefore.setText(""+ SDKFileUtils.getFileSize(videoPath));

            cursor.close();
        }
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
    }

    VideoCompress videoCompress;
    LSProgressDialog progressDialog;
    /**
     * 开始压缩
     */
    private void startCompress(){
        if(SDKFileUtils.getFileSize(videoPath)<=3.0f){  //3M
            DemoUtil.showHintDialog(VideoCompressActivity.this,"文件大小不超过3M, 没必要压缩");
        }else{
            progressDialog=new LSProgressDialog();
            progressDialog.show(VideoCompressActivity.this);
            videoCompress=new VideoCompress(getApplication(),videoPath);
            videoCompress.setOnVideoCompressCompletedListener(new onVideoCompressCompletedListener() {
                @Override
                public void onCompleted(String video) {
                    Log.i("compress","压缩完毕");
                    progressDialog.release();
                    dstPath=video;

                    float size= SDKFileUtils.getFileSize(dstPath);
                    tvAfter.setText(""+size);
                    btnPreview.setEnabled(true);
                }
            });
            videoCompress.setOnVideoCompressProgressListener(new onVideoCompressProgressListener() {
                @Override
                public void onProgress(int percent) {
                    progressDialog.setProgress(percent);
                }
            });
            videoCompress.start();
        }
    }
    private void startPreview(){
        if(SDKFileUtils.fileExist(dstPath)){
            Intent intent = new Intent(VideoCompressActivity.this, VideoPlayerActivity.class);
            intent.putExtra("videopath", dstPath);
            startActivity(intent);
        }
    }
    private void initView(){
        findViewById(R.id.id_videocompress_select).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectVideo();
            }
        });

        findViewById(R.id.id_videocompress_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCompress();
            }
        });

        btnPreview=(Button)findViewById(R.id.id_videocompress_preview);
        btnPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPreview();
            }
        });
        btnPreview.setEnabled(false);
        tvBefore=(TextView)findViewById(R.id.id_videocompress_tv_before);
        tvAfter=(TextView)findViewById(R.id.id_videocompress_tv_after);
    }
}
