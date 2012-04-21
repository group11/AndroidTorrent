package torrent.android;

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
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.*;

/******************** In the Discovery module the group owner connects to a peer from the peer list  ******************/
public class Discovery extends AsyncTask<TorrentActivity, Void, String>{
		String host;
		Discovery dd = this;
		private TorrentActivity activity;
		Context context;
		
/************ The following function is used to convert a primitive 4-byte array type to its corresponding integer form.***********/
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
		 context = activity.myContext;
	
		activity.listlock.lock();
		
/********* The following IF statement waits till atleast one peer is found**********/		
		if(activity.receiver.peerslist1.size()==0){
		try {
			activity.listCV.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		
/******** The following function assigns a peer as DEVICE***********/		
		WifiP2pDevice device = null;
		int i;
		for(i=0;i<activity.receiver.peerslist1.size();i++){
			device = activity.receiver.peerslist1.get(i);
		}
		activity.listlock.unlock();
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		     
			 
/************ The following code makes a socket connection to a peer*********/ 			 
		    host = activity.receiver.peerIp;
		    	Socket socket = new Socket();
		    	activity.client = socket;
		    	String hello = null;
		    	int port=15001;
		    	try {
					socket.bind(null);
					socket.connect((new InetSocketAddress(host, port)), 500);
					context = activity.myContext;
					Intent i1 = new Intent();
					i1.setAction("torrent.android.custom.intent.TEST");
					context.sendBroadcast(i1);
					InputStream in = socket.getInputStream();
					  
					byte[] buf = new byte[1000];
					i=1;
					while(i>0){	
					in.read(buf,0,8);
						int intType = byteArrayToInt(buf,0); 
						int length = byteArrayToInt(buf,4);
						in.read(buf,8,length);
						hello = new String (buf,8,length);
						activity.receiver.buffer = hello; 
						i1.setAction("torrent.android.custom.intent.PRINT");
						context.sendBroadcast(i1);
					}
					in.close();
					socket.close();
					 
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        
		return hello;
	}
	
	 protected void onPostExecute(String result) {
		 TextView tv = (TextView) activity.findViewById(R.id.textView3);
			tv.setText(result);
		 return;
     }

}
