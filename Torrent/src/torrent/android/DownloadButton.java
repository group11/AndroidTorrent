/*
 * This is the download Button class:
 * This button is created in two cases:
 * 		1) If no file is being downloaded, then it is created when a new request is received from the USER
 * 		2) If a file is being Downloaded and device receives a new request, it is created when all the other files ahead of this one in the queue have completed downloading.
 *
 */
	package torrent.android;

	import java.util.Random;

	import android.view.View;
	import android.view.View.OnClickListener;
	import android.widget.Button;

	public class DownloadButton {
		TorrentActivity activity;
		Button btnstart;
		public FileMetaData myFile= new FileMetaData();
		Random randomGenerator = new Random();
		
		DownloadButton(FileMetaData fileMetaData, TorrentActivity arg, Button button){
			activity = arg;
			myFile.name  = fileMetaData.name;
			myFile.SHA1  = fileMetaData.SHA1;
			myFile.fileSize  = fileMetaData.fileSize;
			myFile.numSegments = fileMetaData.numSegments;
			myFile.bitmap  = fileMetaData.bitmap;
			myFile.fileProgressBar  = fileMetaData.fileProgressBar;
			myFile.keywords = fileMetaData.keywords;
			  btnstart = button;
			  if(activity.receiver.downloading)
			  {
				  activity.receiver.downloadButtonQ.add(btnstart);
			  }
			  else{
				  activity.receiver.downloading = true;
		        btnstart.setText("Download " + myFile.name);
		        btnstart.setVisibility(Button.VISIBLE); 
			  }
		        btnstart.setOnClickListener(start);
		}
		
		private OnClickListener start = new OnClickListener() {
			@Override
			public void onClick(View v) {
				//btnstart.setVisibility(Button.INVISIBLE);
				btnstart.setText("Speed UP..");
			activity.receiver.GFR.newRequest(myFile.name, activity);//Start a New Request Stream for this File.
			

		}
		};
	}



