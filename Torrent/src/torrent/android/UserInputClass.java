/*
 * This class performs functions of two modules:
 *
 * 	1)Write Module.
 * 			sendMessage and sendFile
 * 	2)User Input Module
 * 			Search Using Filename.
 *	
 *This module is Invoked once the device is connected to peer and ready to search. 
 */
package torrent.android;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class UserInputClass {
	TorrentActivity activity;
	String file = null;
	String shaone = null;
	String keyword = null;

    public UserInputClass(TorrentActivity arg) {
        Button btnstart = (Button)arg.findViewById(R.id.button);
        btnstart.setText("Search Now");
        btnstart.setVisibility(Button.VISIBLE);
        btnstart.setOnClickListener(start);
        activity = arg;
    }
    
    /**
	 * http://snippets.dzone.com/posts/show/93
	 * The following function is used to convert a primitive integer type to its corresponding byte array form.
	 **/
	 
	public static byte[] intToByteArray(int value) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }
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
	  * addToByteArray: This method adds a particular byte sequence to a bigger byte array. 
	  * parameters: 
	  * 	byte[] from: the sequence to be inserted
	  * 	byte[] to: the sequence to which we add from.
	  * 	int offset: the position (in 'to') where we insert from
	  * 	 
	  */
	 public byte[] addToByteArray(byte[] from, byte[] to, int offset){
		 if((offset + from.length) > to.length){
			 System.out.println("Offset exceeds Array size");
			 return to;
		 }
		 for(int i=offset;i<(offset+from.length);i++){
			 to[i] = from[i-offset];
		 }
		 return to;
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
	 /*
	  * Check if user has asked for the same file/keyword before
	  * */
	 
	 boolean verifyRequest(String fileName){
		 int i=0;
		 for(i=0;i<activity.requestList.size();i++){
			 FileMetaData check = activity.requestList.get(i);
			 if(check.name.equals(fileName)){
				 return false;
			 }
		 }
		 
		 return true;
	 }
	 /**
	  * This function is used to send a Message
	  *@param
	  *		type: String, specifies the message type
	  *		message: String, the actual message to be sent
	  *		offset: int, used only for get request.
	  */
	 public void sendMessage(String type, String message, int offset){
		 int intType = -1;
		 if(type.equals("SF")){
			 boolean check = verifyRequest(message);
			 if(check == false)
				 return;
			 intType = 2;
		 }
		 else if(type.equals("SS"))
			 intType = 3;
		 else if(type.equals("SK"))
			 intType = 4;
		 else if(type.equals("SR"))
			 intType = 5;
		 else if(type.equals("GF")){
			 intType = 6;
			  }
		 else if(type.equals("GR"))
			 intType = 7;
			
		 
		 	byte[] buffer = new byte[12+message.length()];
		 	buffer = addToByteArray(intToByteArray(intType),buffer,0);
		 	buffer = addToByteArray(intToByteArray(offset),buffer,4);
		 	buffer = addToByteArray(intToByteArray(message.length()),buffer,8);
		 	buffer = addToByteArray(message.getBytes(),buffer,12);
		 	OutputStream outputStream;
			Socket socket = activity.client;
			if(socket == null){
				Intent i = new Intent();
				i.setAction("torrent.android.custom.intent.CANCEL");
				activity.myContext.sendBroadcast(i);
				return;
			}
			try {
				outputStream = socket.getOutputStream();
				outputStream.write(buffer,0,12+message.length());
			} catch (IOException e) {
				
				Intent i = new Intent();
				i.setAction("torrent.android.custom.intent.CANCEL");
				activity.myContext.sendBroadcast(i);
				return;
			}
	 }
	 /*
	  * This function is used to send an offset of a file
	  * @param
	  * 	filename: String, the filename of the offset to be sent.
	  * 	offset: int, the offset to be sent.
	  */
	 public void sendFile(String filename, int offset) throws FileNotFoundException{
		File root = android.os.Environment.getExternalStorageDirectory();
		File filePath = new File(root.getAbsolutePath() + "/EE579/" + filename);
		long remCount=filePath.length();
		int sendLength=0;
		byte[] buffer ;
		buffer = new byte[12];
	 	buffer = addToByteArray(intToByteArray(7),buffer,0);
	 	buffer = addToByteArray(intToByteArray(offset),buffer,4);
	 	DataOutputStream outputStream;
		Socket socket = activity.client;
		Context cc = activity.getApplicationContext();
		ContentResolver cr = cc.getContentResolver();
		InputStream f = new FileInputStream(filePath);
		
		
		try {
			if(socket == null){
				Intent i = new Intent();
				i.setAction("torrent.android.custom.intent.CANCEL");
				activity.myContext.sendBroadcast(i);
				return;
			}
			outputStream = new DataOutputStream(socket.getOutputStream());
			
			int len =0;
			int sendCount =0;
			byte[] buf = new byte[activity.segmentSize];
			int i=0;
			while(i<offset && (len = f.read(buf, 0, activity.segmentSize))!=-1)
				i++;
			
			if(len == -1){	
			}
			if((len = f.read(buf,0,activity.segmentSize)) == -1){
			}
				
		 	buffer = addToByteArray(intToByteArray(len + filename.length()+1),buffer,8);
		 	
		 	outputStream.write(buffer,0,12);
		 	filename = filename + "/";
				outputStream.write(filename.getBytes());
				outputStream.write(buf,0,len);
				outputStream.flush();
			f.close();
			
			TextView tv1 = (TextView) activity.findViewById(R.id.TextView01);
			tv1.append("Sent Segment "+offset + "for file \n"+ filename);
		} catch (IOException e) {
			Intent i = new Intent();
			i.setAction("torrent.android.custom.intent.CANCEL");
			activity.myContext.sendBroadcast(i);
			return;
		}
	 	
	 	
	 	
	 }
	 
	/*
	 * This is search now button:
	 * 	 upon clicking this is will ask the user to input a filename or part of it to search in peer device.
	 */
private OnClickListener start = new OnClickListener() {


@Override
public void onClick(View v) {

LayoutInflater li = LayoutInflater.from(activity);
View view = li.inflate(R.layout.file, null);

final EditText input1 = (EditText) view.findViewById(R.id.editText1);

AlertDialog pop = new AlertDialog.Builder(activity).create();
pop.setView(view);


pop.setButton("OK", new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int which) {
     
    	file = input1.getText().toString();
 
    	if(file.length()>1){
    		
    		sendMessage("SF", file,-1);
    	}
    	else if(shaone.length()>1)
    	sendMessage("SS",shaone,-1);
    	else if(keyword.length()>1)
    		sendMessage("SK",keyword,-1);
    	
    }});

pop.setButton2("Cancel", new DialogInterface.OnClickListener() {  
    public void onClick(DialogInterface dialog, int which) {
       dialog.cancel();
        return;  
    }});   

pop.show();

	}

};

}
