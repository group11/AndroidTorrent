package torrent.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
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
import android.widget.TextView;
import android.widget.Toast;

/*************** The WifiDirectBroadcastReceiver module is a intent listner for the WIFI Direct receiver****************/

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements PeerListListener, ConnectionInfoListener{
	
	private WifiP2pManager manager;
    private Channel channel;
    private TorrentActivity activity;
	public boolean connectdone;
    public String buffer;
    public String peerIp;
    public List<WifiP2pDevice> peerslist1 = new ArrayList<WifiP2pDevice>();
	public Lock listlock;
	public Condition listCV;
	private AsyncTask<TorrentActivity, Void, String> ss;
	//private CustomPeerListListener peerlist;
    public Context c1;
    public Intent ii;
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

    @Override
	public void onConnectionInfoAvailable(WifiP2pInfo info){
    	if(info.isGroupOwner){
    		new serverClass().execute(activity);
    	}
		peerIp = info.groupOwnerAddress.getHostAddress();
		Toast.makeText(activity, "INfo Available "+ peerIp + "  " + info.isGroupOwner, Toast.LENGTH_SHORT).show();
		
/**************** The following is the Client Code*************/
		if(!info.isGroupOwner){
			new Discovery().execute(activity);
		}
		
	}
	@Override
	public void onReceive(Context context, Intent intent) {
		
		String action = intent.getAction();
		c1 = context;
		ii = intent;
		if(action.equals("torrent.android.custom.intent.TEST")){
			Toast.makeText(context, "GOT IT", Toast.LENGTH_SHORT).show();
			UserInputClass UI = new UserInputClass(activity);
			
		}
		else if(action.equals("torrent.android.custom.intent.PRINT")){
			 TextView tv = (TextView) activity.findViewById(R.id.textView3);
				tv.setText(buffer);
			
		}
		
/******************** Check to see if Wi-Fi is enabled and notify appropriate activity*********************/
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
        	 int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
             if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                 // Wifi Direct mode is enabled
                 activity.setIsWifiP2pEnabled(true);
                 Toast.makeText(activity,"Enabled",Toast.LENGTH_SHORT).show();
             } else {
                 activity.setIsWifiP2pEnabled(false);
                 Toast.makeText(activity,"Disabled",Toast.LENGTH_SHORT).show();

             }
  
/******************** Call WifiP2pManager.requestPeers() to get a list of current peers when peers change *********************/  
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
        	if (manager != null && connectdone==false) {
                manager.requestPeers(channel,this);
                
                }
        	
/******************** Respond to new connection or disconnections *********************/        	
        }  else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
        	NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        	if(networkInfo.isConnected()){
        		Toast.makeText(activity, "Connected", Toast.LENGTH_SHORT).show();
        		activity.isConnected=true;
        		activity.manager.requestConnectionInfo(activity.channel,this);
        		}
        	else{
        		activity.isConnected=false;
        		Toast.makeText(activity, "Not Connected", Toast.LENGTH_SHORT).show();
        	
        		}

/******************** Respond to this device's wifi state changing *********************/
				} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        	activity.myMacAddress = ((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)).deviceAddress;
        }
	}
	
/******************* The following function is called by request.peers() and manages a connection to a peer ****************/
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
		 		Toast.makeText(activity, "BR " + ddd.deviceAddress,Toast.LENGTH_LONG).show();
		 	
		 		WifiP2pConfig config = new WifiP2pConfig();
				config.deviceAddress = ddd.deviceAddress;
				
		 		activity.manager.connect(activity.channel, config, new ActionListener() {
					
				    @Override
				    public void onSuccess(){
				    	
				    }
				    @Override
				    public void onFailure(int reason) {
				       
				    }
				});
		 	}else{
		 		Toast.makeText(activity, "I Lose!!!",Toast.LENGTH_LONG).show();
		 	}
			return;
			 
		 }
	
}
