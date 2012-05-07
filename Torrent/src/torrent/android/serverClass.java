/*
 * READ MODULE:
 * This class transforms into the read module after connecting to its peer.
 * It parses the incoming messages and pushes it into WiFiDirectBroadcastReceiver(Event Dispatcher) class's buffer
 * 		for it to process. 
 * The only exception is GET RESPONSE message (type 7). This message is handled by this module itself.
 * 
 */


package torrent.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.BitSet;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class serverClass extends AsyncTask <TorrentActivity, FileMetaData, String>{

    private Context context;
    private TorrentActivity activity;
    
    /**
	 * http://snippets.dzone.com/posts/show/94
	 * The following function is used to convert a primitive 4-byte array type to its corresponding integer form.
	 */
	 public static int byteArrayToInt(byte[] b, int offset) {
	        int value = 0;
	        for (int i = 0; i < 4; i++) {
	            int shift = (4 - 1 - i) * 8;
	            value += (b[i + offset] & 0x000000FF) << shift;
	        }
	        return value;
	    }
	 /**
	  * getFromByteArray: This method get a particular byte sequence from a bigger byte array. 
	  * parameters: 
	  * 	byte[] from: the sequence to be extracted from
	  * 	byte[] to: the sequence to extract.
	  * 	int offset: the position (in 'to') where we extract from
	  * 	int length: Length of the sequence to extract
	  */
	 public byte[] getFromByteArray(byte[]from, byte[]to, int offset, int length){
		 if((offset + length)> from.length){
			 System.out.println("Offset exceeds array size");
			 return to;
		 }
		 for(int i=offset;i<length;i++){
			 to[i-offset]=from[offset];
		 }
		 return to;
	 }
    @Override
    protected String doInBackground(TorrentActivity... arg0) {
        try {
        	activity = arg0[0];
            /*
             * Create a server socket and wait for peer connections. This
             * call blocks until a connection is accepted from a client
             */
            ServerSocket serverSocket = new ServerSocket(15001);
            Socket client = serverSocket.accept();
            activity.server = serverSocket;
            activity.client = client;
            context = activity.myContext;
			Intent i = new Intent();
			i.setAction("torrent.android.custom.intent.TEST");
			context.sendBroadcast(i);
            /*If this code is reached, peer has connected 
             */
            InputStream inputstream =  client.getInputStream();
			byte[] buf = new byte[1000];
            int j=1;
            
            //Read Module Starts.
        	
            while(j>0){
			inputstream.read(buf,0,12);
			//Paring Message Header
			int intType = byteArrayToInt(buf,0); 
			int offset = byteArrayToInt(buf,4);
			int length = byteArrayToInt(buf,8);
			//Parsing Message
			inputstream.read(buf,12,length);
			String hello = new String (buf,12,length);
			activity.receiver.buffer = hello;
			activity.receiver.offsetRequested = offset;
			
			
			
			if(intType ==2)//Search Using Filename
				i.setAction("torrent.android.custom.intent.SF");
			
			else if(intType == 3)//Search USing SHA1
				i.setAction("torrent.android.custom.intent.SS");
			
			else if(intType == 4)//Search Using Keywords
				i.setAction("torrent.android.custom.intent.SK");
			
			else if(intType == 5)////Search Response
				i.setAction("torrent.android.custom.intent.SR");
			
			else if(intType == 6)//GET Request
				i.setAction("torrent.android.custom.intent.GF");
			
			else if(intType == 7){//GET Response
				int replacePosition =-1;
				int divider = hello.indexOf("/");
				if(divider == -1)
					continue;
				char[] tempFilename = new char[divider];
				hello.getChars(0, divider, tempFilename, 0);
				String filename = String.valueOf(tempFilename);
				//Find the filename in requestList.
				for(int t=0;t<activity.requestList.size();t++){
					FileMetaData temp1 = activity.requestList.get(t);
					if(temp1.name.equals(filename)){
						replacePosition=t;
						
						break;
					}
				}
				//////////////////
				if(replacePosition == -1)
					continue;
				FileMetaData fileMetaData = activity.requestList.get(replacePosition);
				
				if(offset == -1)
					continue;
				int j5=0;
				//Put the received data into file's temporary variable.
				for(int j4=divider+1; j4<length;j4++, j5++)
					fileMetaData.data[offset*activity.segmentSize + j5] = buf[12 + j4];
				
				fileMetaData.bitmap.set(offset);
				fileMetaData.numSegments++;
				activity.requestList.set(replacePosition,fileMetaData);
				int done = fileMetaData.numSegments;
				int totalSegments = activity.determineNumSegments(fileMetaData.fileSize);
				publishProgress(fileMetaData);//Update file's download bar.
			
				if(done==totalSegments){//If file is done write it into permanent storage area
					final File f = new File(Environment.getExternalStorageDirectory() + "/Download/" + fileMetaData.name);
	                File dirs = new File(f.getParent());
	                
	                if (!dirs.exists())
	                    dirs.mkdirs();
	                f.createNewFile();
	                OutputStream out = new FileOutputStream(f);
	                try {
			            out.write(fileMetaData.data);
			            out.close();
			           
			        } catch (IOException e) {
			            return null;
			        }
			        activity.receiver.buffer  = fileMetaData.name;
			        i.setAction("torrent.android.custom.intent.NEXT");
				}
				else{//If file is not completely received continue.
					activity.receiver.fileNameBuffer = new String(fileMetaData.name);
					i.setAction("torrent.android.custom.intent.GR");
				}
			}
			
			
            context.sendBroadcast(i);
        	}
        	
            inputstream.close();
            client.close();
            return "DONE";
        } catch (IOException e) {
           Intent i = new Intent();
           i.setAction("torrent.android.custom.intent.CANCEL");
           context.sendBroadcast(i);
            return null;
        }
    }
    
    protected void onProgressUpdate(FileMetaData... progress) {
        ProgressBar p = (ProgressBar) progress[0].fileProgressBar;
        int totalSegments = activity.determineNumSegments(progress[0].fileSize);
        p.setProgress(progress[0].numSegments*100/totalSegments);
    }
   
 
    protected void onPostExecute(String result) {
    /*This part of the code will never be reached*/

			return;
		 
     }

}