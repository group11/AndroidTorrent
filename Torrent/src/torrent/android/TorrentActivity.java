package torrent.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Object;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import torrent.android.WiFiDirectBroadcastReceiver;

public class TorrentActivity extends Activity{
	public final int segmentSize = 512;
	 public List<FileMetaData> requestList= new ArrayList<FileMetaData>();
	 public List<FileMetaData> myFiles = new ArrayList<FileMetaData>();
	 public final int MAXREQ =3;
	final Lock listlock=new ReentrantLock();
	final Lock filelock=new ReentrantLock();
	final Condition listCV= listlock.newCondition();
	final Condition fileCV= filelock.newCondition();
	final Lock socketLock=new ReentrantLock();
	final Condition socketCV= socketLock.newCondition();
	final Lock eventQLock = new ReentrantLock();
	final Condition eventQCV = eventQLock.newCondition();
	public List<String> eventQ = new ArrayList<String>();
	public Context myContext;
	private boolean isWifiP2pEnabled = true;
	public WifiP2pManager manager;  
	public String myMacAddress;
	private final IntentFilter intentFilter = new IntentFilter();
	public Channel channel;
	public WiFiDirectBroadcastReceiver receiver = null;

	public IntentFilter eventIntent = new IntentFilter();
	private AsyncTask<TorrentActivity, Void, Integer> dd;
	private AsyncTask<TorrentActivity, Void, String> ss;
	public ServerSocket server = null;
	public Socket client = null;
	public boolean isConnected = false;
	public TextView tv16;
	public Button DiscoverPeersBtn;
	public ProgressBar[] progressBarArr = new ProgressBar[3];
	public TextView[] fileTextView = new TextView[3];
	public Button[] downloadButton = new Button[3];
	public int requestCount = 0;
	public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
	        this.isWifiP2pEnabled = isWifiP2pEnabled;
	    }
	 public boolean mExternalStorageAvailable = false;
	    public boolean mExternalStorageWriteable = false;
	    
	    
	    public int determineNumSegments(long fileSize){
	    	int numSegments =0;
	    	if(((int)fileSize%segmentSize)==0){
	    		numSegments = (int)fileSize/segmentSize;
	    	}
	    	else
	    		numSegments = (int)fileSize/segmentSize +1;
	    	
	    	return numSegments;
	    }
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        myContext = getApplicationContext();
        
/******************Creating the TABS for this application********************/
        TabHost tabHost =(TabHost)findViewById(R.id.tabhost);
        tabHost.setup();
        
        TabSpec spec1=tabHost.newTabSpec("Status");
        spec1.setContent(R.id.tab1);
        spec1.setIndicator("Status");
        
        TabSpec spec2=tabHost.newTabSpec("Details");
        spec2.setContent(R.id.tab2);
        spec2.setIndicator("Details");
        
        TabSpec spec3=tabHost.newTabSpec("Misc.");
        spec3.setContent(R.id.tab3);
        spec3.setIndicator("Misc.");
        
        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        tabHost.addTab(spec3);
       
        
        tv16 = (TextView)findViewById(R.id.TextView16);
 /*****************************Add different Intents to intentFilter************************************/
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction("torrent.android.custom.intent.TEST");//To notify when device is ready for searching
        intentFilter.addAction("torrent.android.custom.intent.PRINT");//To print something on the screen
        intentFilter.addAction("torrent.android.custom.intent.SF");//Search Request: FileName
        intentFilter.addAction("torrent.android.custom.intent.SS");//Search Request: SHA1
        intentFilter.addAction("torrent.android.custom.intent.SK");//Search Request: Keywords
        intentFilter.addAction("torrent.android.custom.intent.SR");//Search Response
        intentFilter.addAction("torrent.android.custom.intent.GF");//Get File Offset
        intentFilter.addAction("torrent.android.custom.intent.GR");// Get Response
        intentFilter.addAction("torrent.android.custom.intent.CANCEL");// Cancel Connections 
        intentFilter.addAction("torrent.android.custom.intent.NEXT");//Check for next file to download
        /***********************************************/
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        progressBarArr[0] = (ProgressBar) findViewById(R.id.progressBar1);
        progressBarArr[1] = (ProgressBar) findViewById(R.id.progressBar2);
        progressBarArr[2] = (ProgressBar) findViewById(R.id.progressBar3);
        downloadButton[0]= (Button) findViewById(R.id.button1);
        downloadButton[1]= (Button) findViewById(R.id.button02);
        downloadButton[2]= (Button) findViewById(R.id.button3);
        fileTextView[0] = (TextView)findViewById(R.id.fileTextView1);
        fileTextView[1] = (TextView)findViewById(R.id.fileTextView2);
        fileTextView[2] = (TextView)findViewById(R.id.fileTextView3);
        
        /****Check for External Storage Availability***************/
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
            
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }   
        /***************************************************/
        File root = android.os.Environment.getExternalStorageDirectory();
        File mainFolder = new File(root.getAbsolutePath()+"/EE579/");
        String[] myFileList = mainFolder.list();
       
        File currentFile[] = mainFolder.listFiles();
       /************************Fill myFiles List with the available files***********/
        for(int t=0;t<currentFile.length;t++){
        	FileMetaData temp = new FileMetaData();
        	temp.name = currentFile[t].getName();
        	temp.fileSize = currentFile[t].length();
        	int numSegments = determineNumSegments(temp.fileSize);
        	temp.bitmap=new BitSet(numSegments);
        	temp.bitmap.set(0, numSegments-1);
        	temp.numSegments = numSegments;
        	/********************To Calculate SHA1************/
		        	MessageDigest md = null;
		        	FileInputStream file = null;
					try {
						file = new FileInputStream(currentFile[t].getAbsolutePath());
					} catch (FileNotFoundException e) {
						
						e.printStackTrace();
					}
		    		byte [] buffer = new byte[(int) currentFile[t].length()];
		    		try {
						file.read(buffer);
					} catch (IOException e) {
						
						e.printStackTrace();
					}
		    		try {
						md = MessageDigest.getInstance("SHA-1");
					} catch (NoSuchAlgorithmException e) {
						
						e.printStackTrace();
					}
		    		md.update(buffer);
		    		byte [] value = new byte[40];
		    		value = md.digest();
		    temp.SHA1 = AeSimpleSHA1.convertToHex(value);
    		/*****************************/
    		myFiles.add(temp);
    		TextView tvf = (TextView)findViewById(R.id.TextView002);
    		tvf.append(temp.name + "\n");
        }
        /******************************************************************/
        Toast.makeText(this,"Start " ,Toast.LENGTH_SHORT).show();
        
        DiscoverPeersBtn = (Button)findViewById(R.id.handle);
        DiscoverPeersBtn.setVisibility(Button.INVISIBLE);
        
    }
/*
 *Discover Peers Button
 *Upon Clicking this button device will initiate peer discovery.  
 */
	private OnClickListener start = new OnClickListener() {


		@Override
		public void onClick(View v) {
			 manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

		            @Override
		            public void onSuccess() {
		               TextView tv1 = (TextView)findViewById(R.id.TextView01);
		               tv1.append("Discovery Initiated \n");
		            }
		            @Override
		            public void onFailure(int reasonCode) {
		            	TextView tv1 = (TextView)findViewById(R.id.TextView01);
			               tv1.append("Discovery Failed : \n" + reasonCode);
		                
		            }
		        });
		}
	};
    public void onResume() {
        super.onResume();
       
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this, listlock, listCV);
        registerReceiver(receiver, intentFilter);
        DiscoverPeersBtn.setVisibility(Button.VISIBLE);
        if(!isConnected){
        	DiscoverPeersBtn.setOnClickListener(start);
        }
        else
        {
        	manager.requestConnectionInfo(channel, receiver);
        }
    }
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        Toast.makeText(this,"Pause",Toast.LENGTH_SHORT).show();
    }
    
  
}