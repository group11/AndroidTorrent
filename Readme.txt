

This file contains the brief description of all the files present in our project.

Discovery.java - This class is responsible for the read module to connect to the group owner(one responsible for initiating the connection). This class is also responsible for the socket connection with the groupowner.

TorrentActivity.java - The torrent Activity module is responsible for enabling the Wifi Direct and discovering peers. All the intents of Wifi Direct are initialized in this module. There are also the onResume , onPause functions which have been implemented.

UserInputClass.java  - THe UserInputClass module created a pop up window so that the peer can request the filename, the SHA1 value. This request is then put into the write queue.

WifiDirectBroadcastReceiver.java - The Wifi Direct Broadcast Receiver module is a intent listner for the WIFI Direct receiver. These are few of the functions.
Check to see if Wi-Fi is enabled and notify appropriate activity
Call WifiP2pManager.requestPeers() to get a list of current peers when peers change
Respond to new connection or disconnections
Respond to this device's wifi state changing
Function request.peers() manages a connection to a peer

SeverClass.java - In this module the socket connection is accepted and data is written into the buffer.Create a server socket and wait for client connections. This call blocks until a connection is accepted from a client. After a connection is accepted it becomes the read module.


