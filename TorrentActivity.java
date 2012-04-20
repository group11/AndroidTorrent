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
import android.widget.Toast;
import java.lang.Object;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import torrent.android.WiFiDirectBroadcastReceiver;

/******************* The TorrenActivity module enables the WIFI Direct and discovers peers**************/
public class TorrentActivity extends Activity{
	 
	final Lock listlock=new ReentrantLock();
	final Condition listCV= listlock.newCondition();
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
	//public Event eventReceiver = null;
	public IntentFilter eventIntent = new IntentFilter();
	private AsyncTask<TorrentActivity, Void, Integer> dd;
	private AsyncTask<TorrentActivity, Void, String> ss;
	public ServerSocket server = null;
	public Socket client = null;
	public boolean isConnected = false;
	
/************** WIFI Direct is enabled *****************/
	public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
	        this.isWifiP2pEnabled = isWifiP2pEnabled;
	    }
	
/******** The following in the onCreate function for when the app is construted************/	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        myContext = getApplicationContext();
		
/******** Intents of WIFI Direct are initialized************/
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction("torrent.android.custom.intent.TEST");
        intentFilter.addAction("torrent.android.custom.intent.PRINT");
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
    //    new EventDispatcher().execute(this);
        Toast.makeText(this,"Start",Toast.LENGTH_SHORT).show();
        
    }

/******** The following in the onResume function for when the app is started or resumed************/
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this, listlock, listCV);
        registerReceiver(receiver, intentFilter);
     

/************* In the following function the peers are discovered *************/
	   if(!isConnected){
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(TorrentActivity.this, "Discovery Initiated",
                        Toast.LENGTH_SHORT).show();
                
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(TorrentActivity.this, "Discovery Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();
            }
        });
       
        }
        else
        {
        	manager.requestConnectionInfo(channel, receiver);
        }
    }

/******** The following in the onPause function for when the app is paused************/
    public void onPause() {
        super.onPause();
		
/******** WIFI Direct receiver is unregistered************/
        unregisterReceiver(receiver);
      
        
        Toast.makeText(this,"Pause",Toast.LENGTH_SHORT).show();
    }
    
  
}