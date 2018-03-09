package com.example.commonDemo;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.lansoeditor.demo.R;
import com.lansosdk.videoeditor.AVDecoder;
import com.lansosdk.videoeditor.AudioEncodeDecode;
import com.lansosdk.videoeditor.CopyDefaultVideoAsyncTask;
import com.lansosdk.videoeditor.FileWriteUtils;
import com.lansosdk.videoeditor.LanSoEditor;
import com.lansosdk.videoeditor.LoadLanSongSdk;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKDir;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.PermissionChecker;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity{

	private DemoInfo[] mTestCmdArray={ 
			
			 new DemoInfo(R.string.demo_id_mediainfo,R.string.demo_id_mediainfo,true,false),
			 new DemoInfo(R.string.demo_id_segmentrecord,R.string.demo_id_segmentrecord,true,false),
			 new DemoInfo(R.string.demo_id_avsplit,R.string.demo_more_avsplit,true,true),//是否视频输出, 是否音频输出
			 new DemoInfo(R.string.demo_id_avmerge,R.string.demo_more_avmerge,true,false),
			 new DemoInfo(R.string.demo_id_cutaudio,R.string.demo_more_cutaudio,false,true),
			 new DemoInfo(R.string.demo_id_cutvideo,R.string.demo_more_cutvideo,true,false),
			 new DemoInfo(R.string.demo_id_concatvideo,R.string.demo_more_concatvideo,true,false),
			 new DemoInfo(R.string.demo_id_videocrop,R.string.demo_more_videocrop,true,false),
			new DemoInfo(R.string.demo_id_videocompress,R.string.demo_more_videocompress,true,false),
			 new DemoInfo(R.string.demo_id_videoscale_soft,R.string.demo_more_videoscale_soft,true,false),
			 new DemoInfo(R.string.demo_id_videoscale_hard,R.string.demo_more_videoscale_hard,false,false),
			 new DemoInfo(R.string.demo_id_videowatermark,R.string.demo_more_videowatermark,true,false),
			 new DemoInfo(R.string.demo_id_videocropwatermark,R.string.demo_more_videocropwatermark,true,false),
			 new DemoInfo(R.string.demo_id_videogetframes,R.string.demo_more_videogetframes,false,false),
			 new DemoInfo(R.string.demo_id_videogetoneframe,R.string.demo_more_videogetoneframe,false,false),
			 new DemoInfo(R.string.demo_id_videoclockwise90,R.string.demo_more_videoclockwise90,true,false),
			 new DemoInfo(R.string.demo_id_videocounterClockwise90,R.string.demo_more_videocounterClockwise90,true,false),
			 new DemoInfo(R.string.demo_id_videoaddanglemeta,R.string.demo_more_videoaddanglemeta,true,false),
			 new DemoInfo(R.string.demo_id_ontpicturevideo,R.string.demo_more_ontpicturevideo,true,false),
			 new DemoInfo(R.string.demo_id_morepicturevideo,R.string.demo_more_morepicturevideo,true,false),
			 new DemoInfo(R.string.demo_id_audiodelaymix,R.string.demo_more_audiodelaymix,false,true),
			 new DemoInfo(R.string.demo_id_audiovolumemix,R.string.demo_more_audiovolumemix,false,true),
			 new DemoInfo(R.string.demo_id_videopad,R.string.demo_more_videopad,true,false),
			 new DemoInfo(R.string.demo_id_videoadjustspeed,R.string.demo_more_videoadjustspeed,true,false),
			 new DemoInfo(R.string.demo_id_videomirrorh,R.string.demo_more_videomirrorh,true,false),
			 new DemoInfo(R.string.demo_id_videomirrorv,R.string.demo_more_videomirrorv,true,false),
			 new DemoInfo(R.string.demo_id_videorotateh,R.string.demo_more_videorotateh,true,false),
			 new DemoInfo(R.string.demo_id_videorotatev,R.string.demo_more_videorotatev,true,false),
			 new DemoInfo(R.string.demo_id_videoreverse,R.string.demo_more_videoreverse,true,false),
			 new DemoInfo(R.string.demo_id_avreverse,R.string.demo_more_avreverse,true,false),
			 new DemoInfo(R.string.direct_play_video,R.string.direct_play_video,false,false),
			 new DemoInfo(R.string.demo_id_expend_cmd,R.string.demo_more_avsplit,false,false),
			 
			 new DemoInfo(R.string.demo_id_connet_us,R.string.demo_more_avsplit,false,false),			 
	};
	private ListView  mListView=null;
	private TextView tvVideoPath;
	private boolean isPermissionOk=false;
	 private static final String TAG="MainActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		 setContentView(R.layout.demo_layout);
		 
		 //加载so库,并初始化.
		 LoadLanSongSdk.loadLibraries();
		 LanSoEditor.initSo(getApplicationContext(),null);
		 
		 
			//因为从android6.0系统有各种权限的限制, 这里开始先检查是否有读写的权限,PermissionsManager采用github上开源库,不属于我们sdk的一部分.
		 //下载地址是:https://github.com/anthonycr/Grant,您也可以使用别的方式来检查app所需权限.
		 PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this, new PermissionsResultAction() {
	            @Override
	            public void onGranted() {
	            	isPermissionOk=true;
	                Toast.makeText(MainActivity.this, R.string.message_granted, Toast.LENGTH_SHORT).show();
	            }
	            @Override
	            public void onDenied(String permission) {
	            	isPermissionOk=false;
	                String message = String.format(Locale.getDefault(), getString(R.string.message_denied), permission);
	                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
	            }
	        });
		 
		  tvVideoPath=(TextView)findViewById(R.id.id_main_tvvideo);
		
		  findViewById(R.id.id_main_select_video).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					startSelectVideoActivity();
				}
			});
	        findViewById(R.id.id_main_use_default_videobtn).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					new com.lansosdk.videoeditor.CopyDefaultVideoAsyncTask(MainActivity.this, tvVideoPath, "ping20s.mp4").execute();
				}
			});
		 mListView=(ListView)findViewById(R.id.id_demo_list);
		 mListView.setAdapter(new SoftApAdapter(MainActivity.this));
		 
		 mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				if(position==0){
					if(checkPath()){
						startMediaInfoActivity();
					}
				}else if(position==1){  //分段录制
					startSegmentRecord();
				}else if(position==mTestCmdArray.length-2){  //最后两个, 扩展功能
					startCustomFunctionActivity();
				}else if(position==mTestCmdArray.length-1){  //最后一个, 联系我们
					
					startBusynessActivity();
				}else if(position==mTestCmdArray.length-3){  //直接视频播放
					startVideoPlayer();
				}else {
					if(checkPath()){
						startActivity(position);	
					}
				}
			}
		});
		 if(isPermissionOk==false && selfPermissionGranted(getApplicationContext(), "android.permission.WRITE_EXTERNAL_STORAGE")==false){
			 showHintDialog("当前没有读写权限,请关闭后,重新打开在弹出框中选中[允许]");
	        	isPermissionOk=false;
	        }else{
	        	isPermissionOk=true;
	        }
		 showHintDialog();
		 
		 new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				test();
			}
		}, 500);
	}
	private void test()
	{
		//VideoEditor  et=new VideoEditor();
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		SDKFileUtils.deleteDir(new File(SDKDir.TMP_DIR)); //删除dir
	}
	private void startActivity(int position)
	{
			DemoInfo demo = mTestCmdArray[position];
			if(demo.mHintId==R.string.demo_id_videoscale_hard)
			{
				startScaleActivity();
				
			}else{
				Intent intent=new Intent(MainActivity.this,AVEditorDemoActivity.class);
				
				intent.putExtra("videopath1",tvVideoPath.getText().toString());
				intent.putExtra("outvideo", demo.isOutVideo);
				intent.putExtra("outaudio", demo.isOutAudio);
				intent.putExtra("demoID", demo.mHintId);
				intent.putExtra("textID", demo.mTextId);
				startActivity(intent);
			}
	}
	private void startMediaInfoActivity()
	{
		Intent intent=new Intent(MainActivity.this,MediaInfoActivity.class);
		intent.putExtra("videopath", tvVideoPath.getText().toString());
		startActivity(intent);
	}
	private void startBusynessActivity()
	{
		Intent intent=new Intent(MainActivity.this,BusynessActivity.class);
		startActivity(intent);
	}
	private void startCustomFunctionActivity()
	{
		Intent intent=new Intent(MainActivity.this,CustomFunctionActivity.class);
		startActivity(intent);
	}
	  //-----------------------
	  private void startScaleActivity()  //开启硬件缩放
	  {
		  Intent intent=new Intent(MainActivity.this,ScaleExecuteDemoActivity.class);
	    	intent.putExtra("videopath", tvVideoPath.getText().toString());
	    	startActivity(intent);
	  }
	  private void startSegmentRecord()
	  {
		    	Intent intent=new Intent(MainActivity.this,SegmentRecorderActivity.class);
		    	startActivity(intent);
	  }
	  //直接播放视频.
	  private void startVideoPlayer()
	  {
		    	Intent intent=new Intent(MainActivity.this,VideoPlayerActivity.class);
		    	intent.putExtra("videopath", tvVideoPath.getText().toString());
		    	startActivity(intent);
	  }
	//-----------------------------------------
	 private final static int SELECT_FILE_REQUEST_CODE=10;
	  
	 private void startSelectVideoActivity()
	    {
	    	Intent i = new Intent(this, FileExplorerActivity.class);
	    	i.putExtra("SELECT_MODE", "video");
		    startActivityForResult(i,SELECT_FILE_REQUEST_CODE);
	    }
	    @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    	super.onActivityResult(requestCode, resultCode, data);
	    	switch (resultCode) {
			case RESULT_OK:
					if(requestCode==SELECT_FILE_REQUEST_CODE){
						Bundle b = data.getExtras();   
			    		String string = b.getString("SELECT_VIDEO");   
						Log.i("sno","SELECT_VIDEO is:"+string);
						if(tvVideoPath!=null)
							tvVideoPath.setText(string);
					}
				break;

			default:
				break;
			}
	    }
	    private boolean checkPath(){
	    	if(isPermissionOk==false){
	    		Toast.makeText(MainActivity.this, "请在系统中打开权限.", Toast.LENGTH_SHORT).show();
	    		return false;
	    	}
	    	
	    	if(tvVideoPath.getText()!=null && tvVideoPath.getText().toString().isEmpty()){
	    		Toast.makeText(MainActivity.this, "请输入视频地址", Toast.LENGTH_SHORT).show();
	    		return false;
	    	}	
	    	else{
	    		String path=tvVideoPath.getText().toString();
	    		if((new File(path)).exists()==false){
	    			Toast.makeText(MainActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
	    			return false;
	    		}else{
	    			MediaInfo info=new MediaInfo(path,false);
	    			boolean ret=info.prepare();
	    			if(ret==false){
	    				showHintDialog(R.string.sdk_checkerror);
	    			}
	    			return ret;
	    		}
	    	}
	    }
	    //--------------------------------------------------------------
		  @SuppressLint("NewApi") 
		  public static boolean selfPermissionGranted(Context context,String permission) {
		        // For Android < Android M, self permissions are always granted.
		        boolean result = true;
		        int targetSdkVersion = 0;
		        try {
		            final PackageInfo info = context.getPackageManager().getPackageInfo(
		                    context.getPackageName(), 0);
		            targetSdkVersion = info.applicationInfo.targetSdkVersion;
		        } catch (PackageManager.NameNotFoundException e) {
		            e.printStackTrace();
		        }

		        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

		            if (targetSdkVersion >= Build.VERSION_CODES.M) {
		                // targetSdkVersion >= Android M, we can
		                // use Context#checkSelfPermission
		                result = context.checkSelfPermission(permission)
		                        == PackageManager.PERMISSION_GRANTED;
		            } else {
		                // targetSdkVersion < Android M, we have to use PermissionChecker
		                result = PermissionChecker.checkSelfPermission(context, permission)
		                        == PermissionChecker.PERMISSION_GRANTED;
		            }
		        }
		        return result;
		    }	
	    private void showHintDialog()
	   	{
	      	 	
	    	String timeHint=getResources().getString(R.string.sdk_limit);
		   	timeHint=String.format(timeHint, VideoEditor.getSDKVersion());
		   		
		   		
	   		new AlertDialog.Builder(this)
	   		.setTitle("提示")
	   		.setMessage(timeHint)
	           .setPositiveButton("确定", new DialogInterface.OnClickListener() {
	   			
	   			@Override
	   			public void onClick(DialogInterface dialog, int which) {
	   		    	Calendar c = Calendar.getInstance();
	   		   		int year=c.get(Calendar.YEAR);
	   		   		int month=c.get(Calendar.MONTH)+1;
	   		   				
	   				int lyear=VideoEditor.getLimitYear();
	   		   		int lmonth=VideoEditor.getLimitMonth();

	   		   		Log.i(TAG,"current year is:"+year+" month is:"+month +" limit year:"+lyear+" limit month:"+lmonth);
	   		   		
	   		   		String timeHint=getResources().getString(R.string.sdk_limit2);
	   		   		timeHint=String.format(timeHint, lyear,lmonth);
	   		   		
	   				showHintDialog(timeHint);
	   			}
	   		})
	           .show();
	   	}
	    private void showHintDialog(String hint){
	    	new AlertDialog.Builder(this)
			.setTitle("提示")
			.setMessage(hint)
	        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
				}
			})
	        .show();
	    }
	    private void showHintDialog(int hintId){
	    	new AlertDialog.Builder(this)
			.setTitle("提示")
			.setMessage(hintId)
	        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			})
	        .show();
	    }
	//------------------------------------------
		private class SoftApAdapter extends BaseAdapter
		{
		    
		    private Activity mActivity;
		    
		    public SoftApAdapter(Activity activity)
		    {
		        mActivity = activity;
		    }
		    
		    @Override
		    public int getCount()
		    {
		        return mTestCmdArray.length;
		    }
		    
		    @Override
		    public Object getItem(int position)
		    {
		        return mTestCmdArray[position];
		    }
		    
		    @Override
		    public long getItemId(int position)
		    {
		        return 0;
		    }
		    
		    @Override
		    public View getView(int position, View convertView, ViewGroup parent)
		    {
			        if (convertView == null)
			        {
			            LayoutInflater inflater = mActivity.getLayoutInflater();
			            convertView = inflater.inflate(R.layout.test_cmd_item, parent, false);
			        }
			        TextView tvNumber = (TextView)convertView.findViewById(R.id.id_test_cmditem_cnt);
			        
			        TextView tvName = (TextView)convertView.findViewById(R.id.id_test_cmditem_tv);
			        
			        DemoInfo cmdInfo = mTestCmdArray[position];
			        
			        String str="NO.";
					 str+=String.valueOf(position+1);
					 
					 tvNumber.setText(str);
					 
					 tvName.setText(getResources().getString(cmdInfo.mHintId));
					 
			        return convertView;
		    }
		}

}
