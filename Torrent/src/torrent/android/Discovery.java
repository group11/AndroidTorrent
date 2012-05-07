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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;



//import android.net.wifi.p2p.WifiP2pManager;
//import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.view.*;
public class Discovery extends AsyncTask<TorrentActivity,FileMetaData ,String>{
		String host;
		Discovery dd = this;
		private TorrentActivity activity;
		Context context;
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
	
	@Override
	protected String doInBackground(TorrentActivity... arg0) {
		activity = arg0[0];
		 context = activity.myContext;//The Application's context
		 /*If no peers are detected/(connected to), this module waits for the same to happen.*/
		activity.listlock.lock();
		if(activity.receiver.peerslist1.size()==0){
		try {
			activity.listCV.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		}
		/////////////////////////////////////////
		
		WifiP2pDevice device = null;
		int i;
		for(i=0;i<activity.receiver.peerslist1.size();i++){
			device = activity.receiver.peerslist1.get(i);
		}
		activity.listlock.unlock();
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		
		try {
			Thread.sleep(10000);//Giving peer time to start the server socket.
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		     
		    host = activity.receiver.peerIp;
		    	Socket socket = new Socket();
		    	activity.client = socket;
		    	String hello = null;
		    	int port=15001;
		    	try {
					socket.bind(null);
					socket.connect((new InetSocketAddress(host, port)), 500);//Connecting to Peer
					context = activity.myContext;
					Intent i1 = new Intent();
					i1.setAction("torrent.android.custom.intent.TEST");
					context.sendBroadcast(i1);
					InputStream in = socket.getInputStream();
					byte[] buf = new byte[1000];
					i=1;
					while(i>0){	//Read Module Starts
					in.read(buf,0,12);
					//Parsing Message Header
						int intType = byteArrayToInt(buf,0); 
						int offset = byteArrayToInt(buf,4);
						int length = byteArrayToInt(buf,8);
						
						in.read(buf,12,length);
					//Parsing Message
						hello = new String (buf,12,length);
						activity.receiver.offsetRequested = offset;
						activity.receiver.buffer = hello;
						
						if(intType ==2)//Search Request using FileName
							i1.setAction("torrent.android.custom.intent.SF");
						
						else if(intType == 3)//SearchRequest using SHA1
							i1.setAction("torrent.android.custom.intent.SS");
						
						else if(intType == 4)//Search Request using Keywords
							i1.setAction("torrent.android.custom.intent.SK");
						
						else if(intType == 5)//Search Response
							i1.setAction("torrent.android.custom.intent.SR");
						
						else if(intType == 6)//GET FILE Request
							i1.setAction("torrent.android.custom.intent.GF");
						
						else if(intType == 7){//GET Response
							int replacePosition =-1;
								int divider = hello.indexOf("/");
								if(divider == -1)
									continue;
								char[] tempFilename = new char[divider];
								hello.getChars(0, divider, tempFilename, 0);
								String filename = String.valueOf(tempFilename);
								//Finding the file in the request list.
								for(int t=0;t<activity.requestList.size();t++){
									FileMetaData temp1 = activity.requestList.get(t);
									if(temp1.name.equals(filename)){
										replacePosition=t;
										
										break;
									}
								}
								if(replacePosition == -1)
									continue;
							FileMetaData fileMetaData = activity.requestList.get(replacePosition);
							
							if(offset == -1)
								continue;
							int j5=0;
							//Copying data into file's temporary variable.
							for(int j4=divider+1; j4<length;j4++, j5++)
								fileMetaData.data[offset*activity.segmentSize + j5] = buf[12 + j4];
							
							fileMetaData.bitmap.set(offset);
							fileMetaData.numSegments++;
							activity.requestList.set(replacePosition,fileMetaData);
							int done = fileMetaData.numSegments;
							int totalSegments = activity.determineNumSegments(fileMetaData.fileSize);
							publishProgress(fileMetaData );//Update file's download bar
							
							if(done== totalSegments){//If entire file is received, writing it into permanent storage area
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
						        i1.setAction("torrent.android.custom.intent.NEXT");//Broadcast an intent to check if any other file is Queued for Downloading.
							}
							else{//If file is not completely received continue. 
								activity.receiver.fileNameBuffer = new String(fileMetaData.name);
								i1.setAction("torrent.android.custom.intent.GR");
							}
						}
						context.sendBroadcast(i1);
					}
					in.close();
					socket.close();
					 
				} catch (IOException e) {
					
					Intent i1 = new Intent();
			           i1.setAction("torrent.android.custom.intent.CANCEL");
			           context.sendBroadcast(i1);
					return null;
				} 
		        
		return hello;
	}
	protected void onProgressUpdate(FileMetaData... progress) {//Updating the file's download bar upon reception of each segment
        ProgressBar p = (ProgressBar) progress[0].fileProgressBar;
        int totalSegments = activity.determineNumSegments(progress[0].fileSize);
        p.setProgress(progress[0].numSegments*100/totalSegments);
    }
	 protected void onPostExecute(String result) {
		
		 //Will never reach this part of the code.
		 return;
     }

}
