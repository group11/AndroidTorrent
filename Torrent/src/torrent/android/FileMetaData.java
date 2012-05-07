/*
 * This is what a filemeta data consists of
 * The variable names are self explanatory.
 */
package torrent.android;

import java.util.BitSet;

import android.widget.ProgressBar;

public class FileMetaData {
	String name;//File Name
	long fileSize;
	String SHA1;
	int numSegments;//Number of Segments of the file device has 
	String[] keywords;
	BitSet bitmap;//Bitmap of the segments device has
	BitSet doNotAsk;//Bitmap of already requested segments.
	ProgressBar fileProgressBar;//The progress bar associated with this file
	byte[] data;//Temporary data variable for this file
	FileMetaData(){
		name = null;
		fileSize = 0;
		SHA1 = null;
		numSegments = 0;
		keywords = null;
		bitmap = null;
		fileProgressBar = null;
		data = null;
		doNotAsk=null;
	}
}
