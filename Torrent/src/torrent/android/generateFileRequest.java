/*
 * genreateFileRequest class is used when a new file offset is to be requested from a peer.
 * 
 */

package torrent.android;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

public class generateFileRequest {
	TorrentActivity activity;
	
	public FileMetaData myFile= new FileMetaData();
	Random randomGenerator = new Random();
	/*
	 * This function will be called directly by other modules (viz. UserInputClass and WiFiDirectBroadcastReceiver)
	 * 	when a new file offset is to be requested.
	 * 
	 * First it will generate a valid random offset to be requested:
	 * 		A valid offset has to satisfy two criteria:
	 * 			a) it must be a file segment that is missing on this device
	 * 			b) it must be a file segment that has not been asked for before.
	 * 
	 * Then it will create a message and send it.
	 */
	public void newRequest(String fileName, TorrentActivity arg){
		activity = arg;
		FileMetaData temp = null;
		boolean found = false;
		int i;
		for(i=0; i<activity.requestList.size();i++){
			temp = activity.requestList.get(i);
			if(fileName.equals(temp.name)){
				found = true;
			
				break;
			}
		}
		if(found){
			int segment = findSegment(temp);
			if(segment == -1){
				return;
			}
			int numSegments =activity.determineNumSegments(temp.fileSize);
			temp.doNotAsk.set(segment);
			activity.requestList.set(i, temp);
				activity.receiver.UI.sendMessage("GF", createMessage(temp), segment);
			
			
			}
		return;
	}
	public String createMessage(FileMetaData fileMetaData) {
		
		String message = new String(fileMetaData.name +"/"+fileMetaData.SHA1);
		return message;
	}
	private int findSegment(FileMetaData fileMetaData) {
		int numSegments =activity.determineNumSegments(fileMetaData.fileSize);
		boolean segmentFound = false;
		int segment = -1;
		int count=0;
		if(fileMetaData.numSegments == numSegments)
			return -1;
		do{
			segment = randomGenerator.nextInt(numSegments);
			count++;
			segment = segment%numSegments;
			if(fileMetaData.bitmap.get(segment) || fileMetaData.doNotAsk.get(segment))
				segmentFound=false;
			else
				segmentFound = true;
				}while(!segmentFound);
		return segment;
	}
	
	
	
	
}
