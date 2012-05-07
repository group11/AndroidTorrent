This file contains the brief description of all the files present in our project.

Discovery.java - This class is responsible for the discovery module to commect to the group owner(one responsible for initiating the connection). Once a peer is peer is found it assigns a peer as a device. This class is also responsible for the socket connection between the groupowner and the peers.
Read Module is Implemented
Parsing Message Header
Parsing Message
Copying data into file's temporary variable.


TorrentActivity.java - The torrent Activity module is responsible for enabling the Wifi Direct and discovering peers. All the intents of Wifi Direct are initialized in this module. There are also the onResume , onPause functions which have been implemented.
Creating the TABS for this application
Add different Intents to intentFilter
Creating the TABS for this application for GUI
Check for External Storage Availability 
Fill myFiles List with the available files
To Calculate SHA1.
 

UserInputClass.java  - This class performs functions of two modules:

    1)Write Module.
  			sendMessage and sendFile
  	2)User Input Module
  			Search Using Filename.
Check if user has asked for the same file/keyword before.
There are several other functions like send file name, offset.
Implementing the searh button.
 This module is Invoked once the device is connected to peer and ready to search. 


FileMetaData.java - Metadata file contents
the contents are Filename, Number of segments in the file, Bitmap of the device segments, Temporary data segments for this file.



WifiDirectBroadcastReceiver.java - It has two major parts: Network Part and File Transfer part.
  Network Part:
  			It handles all the WiFiDirect Intents broadcasted by the Android System.
  File Transfer Part:
  			it handles all the messages and Intents broadcasted by the Read Modules.
The Wifi Direct Broadcast Receiver module is a intent listner for the WIFI Direct receiver. These are few of the functions.
Check to see if Wi-Fi is enabled and notify appropriate activity
Call WifiP2pManager.requestPeers() to get a list of current peers when peers change
Respond to new connection or disconnections
Respond to this device's wifi state changing
Function request.peers() manages a connection to a peer
Response to all the search requests when received.


SeverClass.java - 
This class transforms into the read module after connecting to its peer.
  It parses the incoming messages and pushes it into WiFiDirectBroadcastReceiver(Event Dispatcher) class's buffer
  		for it to process. 
  The only exception is GET RESPONSE message (type 7). This message is handled by this module itself.
In this module the socket connection is accepted and data is written into the buffer.Create a server socket and wait for client connections. This call blocks until a connection is accepted from a client.


AeSimpleSHA1.java - This module returns a string representation of a given SHA1 value.


DownloadButton.java - This is the download Button class:
  This button is created in two cases:
  		1) If no file is being downloaded, then it is created when a new request is received from the USER
  		2) If a file is being Downloaded and device receives a new request, it is created when all the other files ahead of this one in the queue have completed downloading.


generateFileRequest.java -  GenreateFileRequest class is used when a new file offset is to be requested from a peer.
This function will be called directly by other modules (viz. UserInputClass and WiFiDirectBroadcastReceiver)
	  	when a new file offset is to be requested.
	  
	  First it will generate a valid random offset to be requested:
	  		A valid offset has to satisfy two criteria:
	  			a) it must be a file segment that is missing on this device
	  			b) it must be a file segment that has not been asked for before.
Then it will create a message and send it.



