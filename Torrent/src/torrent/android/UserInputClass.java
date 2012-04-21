package torrent.android;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;


/************* The UserInputClass module created a pop-up window on request to input the file details and transfer data*******************/
public class UserInputClass {
	TorrentActivity activity;
	String file;
	String shaone;
	String keyword;

    public UserInputClass(TorrentActivity arg) {
        Button btnstart = (Button)arg.findViewById(R.id.button);
        btnstart.setText("Search Now");
        btnstart.setVisibility(Button.VISIBLE);
        btnstart.setOnClickListener(start);
        activity = arg;
    }
    
/************ The following function is used to convert a primitive integer type to its corresponding byte array form *************/	 
	public static byte[] intToByteArray(int value) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }
	
/************ The following function is used to convert a primitive 4-byte array type to its corresponding integer form ************/
	 public static int byteArrayToInt(byte[] b, int offset) {
	        int value = 0;
	        for (int i = 0; i < 4; i++) {
	            int shift = (4 - 1 - i) * 8;
	            value += (b[i + offset] & 0x000000FF) << shift;
	        }
	        return value;
	    }
	 
/*****************
	  * addToByteArray: This method adds a particular byte sequence to a bigger byte array. 
	  * parameters: 
	  * 	byte[] from: the sequence to be inserted
	  * 	byte[] to: the sequence to which we add from.
	  * 	int offset: the position (in 'to') where we insert from
	  * 	 
*****************/
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
	 
/******************
	  * getFromByteArray: This method get a particular byte sequence from a bigger byte array. 
	  * parameters: 
	  * 	byte[] from: the sequence to be extracted from
	  * 	byte[] to: the sequence to extract.
	  * 	int offset: the position (in 'to') where we extract from
	  * 	int length: Length of the sequence to extract
*****************/
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
	 
/************ This function is used to send data to a peer ************/
	 public void sendMessage(String type, String message){
		 int intType = -1;
		 if(type.equals("GET")){
			 intType = 1;
		 }
		 	byte[] buffer = new byte[1000];
		 	buffer = addToByteArray(intToByteArray(intType),buffer,0);
		 	buffer = addToByteArray(intToByteArray(message.length()),buffer,4);
		 	buffer = addToByteArray(message.getBytes(),buffer,8);
		 	OutputStream outputStream;
			Socket socket = activity.client;
			try {
				outputStream = socket.getOutputStream();
				outputStream.write(buffer,0,8+message.length());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	 }
	 
/************** This function is a listener for a button*************/	 
private OnClickListener start = new OnClickListener() {

/************* This function creates a pop-up window for user input of file details********/
@Override
public void onClick(View v) {

LayoutInflater li = LayoutInflater.from(activity);
View view = li.inflate(R.layout.file, null);

/********** Three UserInput fields are created ,i.e., File name, SHA1 and keyword*********/
final EditText input1 = (EditText) view.findViewById(R.id.editText1);
final EditText input2 = (EditText) view.findViewById(R.id.editText2);
final EditText input3 = (EditText) view.findViewById(R.id.editText3);

AlertDialog pop = new AlertDialog.Builder(activity).create();
pop.setView(view);


pop.setButton("OK", new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int which) {
     
    	file = input1.getText().toString();
    	shaone = input2.getText().toString();
    	keyword = input3.getText().toString();
    	sendMessage("GET", file);
    	
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
