package com.lansosdk.videoeditor;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class FileWriteUtils {

    
	FileOutputStream fos = null;
    BufferedOutputStream bos = null;
    
   
    
	private final String TAG="FileWriteUtils";
	private final boolean VERBOSE=false;
	private String mSavePath=null;
	
  //  这段代码不要删除, 一直放在这里.
    public boolean openWriteFile(String path)
    {
    	boolean ret=false;
    	
    	if(mSavePath==null)
    	{
    		mSavePath=path;
        	if(path!=null){
        	    try {
                    fos = new FileOutputStream(new File(path));
                    bos = new BufferedOutputStream(fos);
                    fos = null;     // closing bos will also close fos
                    ret=true;
                } catch (IOException ioe) {
                 //   throw new RuntimeException(ioe);
                	Log.e(TAG,"video encoder cannot open write file: "+ioe.toString());
                }
        	}
    	}
    	return ret;
    }
    public FileWriteUtils()
    {
    	
    }
    public FileWriteUtils(String path)
    {
    	openWriteFile(path);
    }
    public void closeWriteFile()
    {
    	 try {
             if (bos != null) {
                 bos.close();
             }
             if (fos != null) {
                 fos.close();
             }
         } catch (IOException ioe) {
             throw new RuntimeException(ioe);
         }
    }
    /*
     * byte[] dataCopy = new byte[bufferReadResult];可以用这样的写入.
     * System.arraycopy(audioData.array(), 0, dataCopy, 0, bufferReadResult); 
     * */
    
    public void writeFile(ByteBuffer buf)
    {
    	  byte[] data = new byte[buf.remaining()];
    	  
    	 // if(VERBOSE)
    		  Log.d(TAG,"writeFile to "+mSavePath+" size:"+data.length);
    	  if(bos!=null)
    	  {
    		  buf.get(data);
	          try {
				bos.write(data);
				
			   } catch (IOException e) {
				// TODO Auto-generated catch block
			 	//e.printStackTrace();
				  Log.e(TAG,e.toString()); 
			  }
    	  }else{
    		  Log.e(TAG,"videoencoder write file error bos is null");
    	  }
    }
    public void writeFile(IntBuffer buf)
    {
    	ByteBuffer bb = ByteBuffer.allocateDirect( buf.remaining()*4);
        bb.asIntBuffer().put(buf);  //看做是IntBuffer, 然后put进数据.
        writeFile(bb);
    }
    public void writeFile(byte[] data)
    {
    	  if(VERBOSE)
    		  Log.d(TAG,"writeFile to "+mSavePath+" size:"+data.length);
    	  if(bos!=null)
    	  {
	          try {
				bos.write(data);
			   } catch (IOException e) {
				// TODO Auto-generated catch block
			 	//e.printStackTrace();
				  Log.e(TAG,e.toString()); 
			  }
    	  }else{
    		  Log.e(TAG,"videoencoder write file error bos is null");
    	  }
    }
    public static void saveData(int[] data, String filePath)
    {
    	FileWriteUtils write=new FileWriteUtils(filePath);
    	write.writeFile(IntBuffer.wrap(data));
    	write.closeWriteFile();
    }
	
}
