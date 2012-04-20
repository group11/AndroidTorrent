package torrent.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;

public class serverClass extends AsyncTask <TorrentActivity, Void, String>{

/***************** In the serverClass module the socket connection is accepted and the incoming data is written into a buffer*********/
    
	private Context context;
    private TorrentActivity activity;
    
 /********* The following function is used to convert a primitive 4-byte array type to its corresponding integer form.***********/
	 public static int byteArrayToInt(byte[] b, int offset) {
	        int value = 0;
	        for (int i = 0; i < 4; i++) {
	            int shift = (4 - 1 - i) * 8;
	            value += (b[i + offset] & 0x000000FF) << shift;
	        }
	        return value;
	    }
	
/***************
	  * getFromByteArray: This method get a particular byte sequence from a bigger byte array. 
	  * parameters: 
	  * 	byte[] from: the sequence to be extracted from
	  * 	byte[] to: the sequence to extract.
	  * 	int offset: the position (in 'to') where we extract from
	  * 	int length: Length of the sequence to extract
********************/
	
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
			
/*********Create a server socket and wait for client connections. This call blocks until a connection is accepted from a client***********/
        	publishProgress();
            ServerSocket serverSocket = new ServerSocket(15001);
            Socket client = serverSocket.accept();
            activity.server = serverSocket;
            activity.client = client;
            context = activity.myContext;
			Intent i = new Intent();
			i.setAction("torrent.android.custom.intent.TEST");
			context.sendBroadcast(i);
            
/*****************In the following code a client has connected and data is transferred*****************/
            InputStream inputstream =  client.getInputStream();
            //OutputStream out = client.getOutputStream();
            byte[] buf = new byte[1000];
            int j=1;
        	while(j>0){
			inputstream.read(buf,0,8);
			int intType = byteArrayToInt(buf,0); 
			int length = byteArrayToInt(buf,4);
			inputstream.read(buf,8,length);
			String hello = new String (buf,8,length);
			activity.receiver.buffer = hello;
            i.setAction("torrent.android.custom.intent.PRINT");
            context.sendBroadcast(i);
        	}
            inputstream.close();
            client.close();
             //serverSocket.close();
           // new writeThread().execute(activity);
           // new readThread().execute(activity);
			
            return "DONE";
        } catch (IOException e) {
           
            return null;
        }
    }
    protected void onProgressUpdate(Integer... progress) {
    	TextView text = (TextView) activity.findViewById(R.id.textView3);
		text.setText("Accepting");
    }
 
    protected void onPostExecute(String hello) {
    	
		/*	Context mContext =activity.myContext;
			Dialog dialog = new Dialog(mContext);
			dialog.setContentView(R.layout.custom_dialog);
			dialog.setTitle("Peer List Size");
*/
    	//new readThread().execute(activity);
    		//new writeThread().execute(activity);
			TextView text = (TextView) activity.findViewById(R.id.textView3);
			text.setText(hello);
			
			return;
		 
     }

}