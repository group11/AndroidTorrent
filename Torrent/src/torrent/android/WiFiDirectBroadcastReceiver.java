/*
 * Event Dispatcher Module:
 * This is the heart of this application. All intents/events are processed here.
 * It has two major parts: Network Part and File Transfer part.
 * Network Part:
 * 			It handles all the WiFiDirect Intents broadcasted by the Android System.
 * File Transfer Part:
 * 			it handles all the messages and Intents broadcasted by the Read Modules.
 *
 */

package torrent.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements PeerListListener, ConnectionInfoListener{
	
	private WifiP2pManager manager;
	public boolean downloading = false;
	public List <Button> downloadButtonQ = new ArrayList<Button>();
	 public WifiP2pConfig config;
    private Channel channel;
    private TorrentActivity activity;
	public boolean connectdone;
    public String buffer;//Variable used to transfer data from read module to Event Dispatcher..   
    public String fileNameBuffer;
    public int offsetRequested;
    public String peerIp;
    public List<WifiP2pDevice> peerslist1 = new ArrayList<WifiP2pDevice>();
	public Lock listlock;
	public Condition listCV;
	private AsyncTask<TorrentActivity, FileMetaData, String> ss = null;
    public Context c1;
    public Intent ii;
    public UserInputClass UI;
    public generateFileRequest GFR ;
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, 
    		TorrentActivity activity, Lock listlock, Condition listCV) 
    		{
	
    	super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
        this.listlock=listlock;
        this.listCV=listCV;
        this.connectdone=false;
        this.buffer = "Not Yet";
	}
/*
 * This function is called when any information about the wifip2p connection is available.
 * When this function is called it can be safely assumed that this device is connected to its peer.
 * Depending upon whether this device is the group owner or not, it invokes one of the read modules (viz. Discovery and serverClass).
 * 
 */
    @Override
	public void onConnectionInfoAvailable(WifiP2pInfo info){
    	if(info.isGroupOwner){
    		ss =new serverClass().execute(activity);
    	}
		peerIp = info.groupOwnerAddress.getHostAddress();
		TextView tv2 = (TextView)activity.findViewById(R.id.TextView01);
        tv2.append("Info Available "+ peerIp + "  " +"Am I the Group Owner: "+ info.isGroupOwner+"\n");
		if(!info.isGroupOwner){
			ss = new Discovery().execute(activity);
		}
		
	}
    /*
     * This function is invoked each time a broadcasted intent is received.
     * It determines what kind of intent was broadcasted and takes action accordingly.
     */
	@Override
	public void onReceive(Context context, Intent intent) {
		
		String action = intent.getAction();
		c1 = context;
		ii = intent;
		if(action.equals("torrent.android.custom.intent.TEST")){
			/******Just a test Intent to notify the start of the read Modules.**********/
			TextView tv2 = (TextView)activity.findViewById(R.id.TextView01);
	        tv2.append("Ready to Search\n");
			UI = new UserInputClass(activity);
			GFR= new generateFileRequest();
			
		}
		
		else if(action.equals("torrent.android.custom.intent.PRINT")){
			/**This intent can be used by threads to display something/anything on the screen.**/
			 TextView tv = (TextView) activity.findViewById(R.id.textView3);
				tv.setText(buffer);
			
		}
		else if(action.equals("torrent.android.custom.intent.SF")){
			 /******************When Search Request using a filename is received*******************/
			TextView tv = (TextView) activity.findViewById(R.id.textView3);
			tv.setText("Filename = " + buffer);
			String bufferLower = buffer.toLowerCase();
			boolean found = false;
			FileMetaData temp = null;
			for (int i=0;i<activity.myFiles.size();i++){
				 temp= activity.myFiles.get(i);
				String tempLower = temp.name.toLowerCase();
				if(tempLower.contains(bufferLower)){
					
					found = true;
					break;
				}
			}
			if(found)
			{
				/************File found sending search response back************/
				TextView tvq = (TextView) activity.findViewById(R.id.TextView01);
				tvq.append("Peer's Request "+ temp.name +" Found \n");
				String message = new String(temp.name+"/"+temp.SHA1+"/"+Long.toString(temp.fileSize));
				UI.sendMessage("SR",message,-1);
				
			}
			
		}
		else if(action.equals("torrent.android.custom.intent.SS")){
			 /******************When Search Request using SHA1 is received************************/
			TextView tv = (TextView) activity.findViewById(R.id.textView3);
			tv.setText("SHA1 = " + buffer);
			
		}
		else if(action.equals("torrent.android.custom.intent.SK")){
			 /******************When Search Request using Keywords is received*******************/
			TextView tv = (TextView) activity.findViewById(R.id.textView3);
			tv.setText("Keyword = " + buffer);
			
		}
		else if(action.equals("torrent.android.custom.intent.SR")){
			 /******************When Search Response is received*******************/
			
			/**************************Parsing the message*************************************/
			/********Extracting Filename*******/
			int divider = buffer.indexOf("/");
			int start= divider;
			char[] tempFilename = new char[divider];
			buffer.getChars(0, divider, tempFilename, 0);
			String filename = String.valueOf(tempFilename);
			/********Extracting SHA1************/
			divider = buffer.indexOf("/", start+1);
			char[] tempSHA1 = new char[divider-start-1];
			buffer.getChars(start+1, divider, tempSHA1, 0);
			String SHA1 = String.valueOf(tempSHA1);
			start = divider;
			
			/*****Extracting File Size*********/
			char[] tempfileSize = new char[buffer.length()-start-1];
			buffer.getChars(start+1, buffer.length(), tempfileSize, 0);
			String fileSize = String.valueOf(tempfileSize);
			start = divider;
			Long FileSize = new Long(fileSize);
			/**********************************************************************************/
			TextView tvq = (TextView) activity.findViewById(R.id.TextView01);
			tvq.append("My Request "+ filename+" Found\n");
			FileMetaData toInsertRequest = new FileMetaData();
			toInsertRequest.name = new String(filename);
			toInsertRequest.SHA1 = new String(SHA1);
			toInsertRequest.fileSize = FileSize.longValue();
			toInsertRequest.bitmap = new BitSet(activity.determineNumSegments(toInsertRequest.fileSize));
			toInsertRequest.doNotAsk = new BitSet(activity.determineNumSegments(toInsertRequest.fileSize));
			toInsertRequest.bitmap.clear();
			toInsertRequest.doNotAsk.clear();
			toInsertRequest.data  = new byte[(int)toInsertRequest.fileSize];
			boolean alreadyListed = false;
			for(int t=0;t<activity.requestList.size();t++)
			{
				FileMetaData forTemp = activity.requestList.get(t);
				if(forTemp.name.equals(toInsertRequest.name) && forTemp.SHA1.equals(toInsertRequest.SHA1))
				{
					alreadyListed = true;
					break;
				}
			}
			if(!alreadyListed){//Check Whether if this is a duplicate request or not.
				activity.fileTextView[activity.requestCount].setText(filename);
				toInsertRequest.fileProgressBar = activity.progressBarArr[activity.requestCount];
				Button button = activity.downloadButton[activity.requestCount];
				activity.requestCount = (activity.requestCount+1)%activity.MAXREQ;
				toInsertRequest.fileProgressBar.setVisibility(ProgressBar.VISIBLE);
				toInsertRequest.fileProgressBar.setProgress(0);
				toInsertRequest.fileProgressBar.setBackgroundColor(1);
				DownloadButton DB = new DownloadButton(toInsertRequest, activity ,button);	
				activity.requestList.add(toInsertRequest);//Adding to Request List
			}
			else
				tvq.append("Duplicate Request");
		}
		
		else if(action.equals("torrent.android.custom.intent.GF")){
			 /******************When GET request is Received ************************/
			int offset = offsetRequested;
			int divider = buffer.indexOf("/");
			int start= divider;
			char[] tempFilename = new char[divider];
			buffer.getChars(0, divider, tempFilename, 0);
			String filename = String.valueOf(tempFilename);
			/************************Sending the file segment***************/
			try {
				UI.sendFile(filename, offset);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			 
			 /****************************************************/
		}
		else if(action.equals("torrent.android.custom.intent.GR")){
			/*****************Get response received******************/
			//Only broadcasted when entire file is NOT received.
			GFR.newRequest(fileNameBuffer, activity);
			TextView tv = (TextView) activity.findViewById(R.id.textView1);
			tv.setText("Segment "+ offsetRequested+ " received");
			
            
		}
		else if(action.equals("torrent.android.custom.intent.CANCEL")){
			/******This Intent is broadcasted to reset or cancel the connection with its peer*******/
			TextView tv = (TextView) activity.findViewById(R.id.TextView16);
			tv.setText("Disconnecting..");
			resetConnection();
		}
		else if(action.equals("torrent.android.custom.intent.NEXT")){
			/*****************Entire File is Received*************/
			/*Check if any other file is Queued for downloading*/
			TextView tvd = (TextView)activity.findViewById(R.id.TextView004);
			tvd.append("\n"+buffer);
			if(downloadButtonQ.size() == 0){//No file is Queued for Downloading
				downloading = false;
			}
			else{//There is a File Queued For downloading.
			Button next = downloadButtonQ.get(0);
			downloadButtonQ.remove(0);
			next.setVisibility(Button.VISIBLE);
				}
			}
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
        	 int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
             if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                 // Wifi Direct mode is enabled
                 activity.setIsWifiP2pEnabled(true);
                 //Toast.makeText(activity,"Enabled",Toast.LENGTH_SHORT).show();
             } else {
                 activity.setIsWifiP2pEnabled(false);
                 //Toast.makeText(activity,"Disabled",Toast.LENGTH_SHORT).show();

             }
        	
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
        	
        	TextView tv1 = (TextView)activity.findViewById(R.id.textView1);
    		tv1.setText(" ");
    		Button btnstart = (Button)activity.findViewById(R.id.button2);
	        btnstart.setVisibility(Button.INVISIBLE);
	        
        	if (manager != null && connectdone==false) {
                manager.requestPeers(channel,this);
                
                }
        	
        	
        }  else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
        	NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        	if(networkInfo.isConnected()){
        		TextView tv2 = (TextView)activity.findViewById(R.id.TextView01);
		        tv2.append("Connected\n");
        		
        		activity.isConnected=true;
        		activity.manager.requestConnectionInfo(activity.channel,this);
        		}
        	else{
        		activity.isConnected=false;
        		TextView tv2 = (TextView)activity.findViewById(R.id.TextView01);
		        tv2.append("Not Connected\n");
        		
        		
        	}
        	} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        	activity.myMacAddress = ((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)).deviceAddress;
        }
	}
	/*
	 * Reset Connection:
	 * this function is called when the device intents to cancel the ongoing connections and disconnects from its peers.
	 * It closes all the sockets and kills all the threads (except the main one).
	 */
	public void resetConnection(){
		TextView tv1 = (TextView)activity.findViewById(R.id.textView1);
		tv1.setText(" ");
		Button btnstart = (Button)activity.findViewById(R.id.button2);
        btnstart.setVisibility(Button.INVISIBLE);
		connectdone=false;
		activity.isConnected = false;
		if(ss != null){
			ss.cancel(true);
			ss = null;
			try {
				if(activity.client!=null){
				activity.client.close();
				activity.client=null;
				}
				if(activity.server!=null){
					activity.server.close();	
					activity.server= null;
				
				}
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			
		}
		
	manager.cancelConnect(activity.channel, new ActionListener(){
		
		@Override
		public void onFailure(int arg0) {
			
			
		}

		@Override
		public void onSuccess() {
			
			
		}
		
		
	});
	}
	/*
	 * This function is invoked when the device requests for peers and there are some peers available.
	 * Once peers are available, the device compares its MAC address with its peers MAC address.
	 * If it has a higher MAC address, then it will perform a WiFiP2P connect otherwise it will wait for its peer to do the same.
	 * This avoids multiple connects to be exchanged and ensures a proper handshake mechanism. 
	 */
	 @Override
	    public void onPeersAvailable(WifiP2pDeviceList peer)
		 {
		 int flag=0;
		 	if(peerslist1.size()==0)
		 		flag=1;
		    peerslist1.clear();
		 	peerslist1.addAll(peer.getDeviceList());
		 	WifiP2pDevice ddd = null;
		 
		 	if(peerslist1.size()!=0)
		 		ddd = peerslist1.get(0);
		 	if(ddd == null)
		 		return;
		 	activity.receiver.connectdone=true;
		 	if(activity.myMacAddress.compareTo(ddd.deviceAddress)>=0){
		 		
		 		TextView tv = (TextView)activity.findViewById(R.id.textView1);
		 		tv.append("Peer:\n" + ddd.deviceName);
		 		config = new WifiP2pConfig();
				config.deviceAddress = ddd.deviceAddress;
				Button btnstart = (Button)activity.findViewById(R.id.button2);
		        btnstart.setText("Connect");
		        btnstart.setVisibility(Button.VISIBLE);
		        btnstart.setOnClickListener(start);
		        TextView tv2 = (TextView)activity.findViewById(R.id.TextView01);
		        tv2.append("I am supposed to Connect\n");
		 	}else{
		 		TextView tv2 = (TextView)activity.findViewById(R.id.TextView01);
		        tv2.append("Peer will Connect\n");
		 		
		 	}
		 	
			return;
			 
		 }
	 /*
	  * This is the connect button.
	  * Upon clicking this button, the device initiates a connect request.
	  * This button is displayed only in the device with a higher MAC Address. 
	  * */
	 
	
	 private OnClickListener start = new OnClickListener() {


     	@Override
     	public void onClick(View v) {
		activity.manager.connect(activity.channel, config, new ActionListener() {
			
		    @Override
		    public void onSuccess(){
		    	
		    }
		    @Override
		    public void onFailure(int reason) {
		       return;
		    }
		});
     	}
     };
}
