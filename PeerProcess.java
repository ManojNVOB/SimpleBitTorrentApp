import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PeerProcess {
    		
    private static Map<Integer, String> peerConfig;
	private static Map<String, String> commonConfig;
	private static Logger LOGGER;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
	
	 List<Peer> unchokedPeersList, chokedPeersList;  
	
	// Method to get peer configurations such as peer_id, address, listening port
	public static Map<Integer, String> getPeerConfig() {
		return peerConfig;
	}
	
	// Method to set peer configurations from PeerInfo.cfg
	public static void setPeerConfig() throws NumberFormatException, IOException{
		
		String fileName = "PeerInfo.cfg";
		FileInputStream inputStream = new FileInputStream(new File(fileName));
		InputStreamReader inputReader = new InputStreamReader(inputStream);
		BufferedReader br = new BufferedReader(inputReader);
		
		String currLine = br.readLine();
		peerConfig = new HashMap<Integer, String>();
		
		while (currLine != null) {
			String[] parts = currLine.split(" ");
			peerConfig.put(Integer.valueOf(parts[0]), currLine);
			currLine = br.readLine();
		}
		br.close();
	}

	
	
	// Method to get common configuration such as file name, size etc. 
	public static Map<String, String> getCommonConfig() {
		return commonConfig;
	}

	// Method to set common configuration from Common.cfg 
	public static void setCommonConfig() throws IOException {
		
		String fileName = "Common.cfg";
		FileInputStream inputStream = new FileInputStream(new File(fileName));
		InputStreamReader inputReader = new InputStreamReader(inputStream);
		BufferedReader br = new BufferedReader(inputReader);
		
		commonConfig = new HashMap<String, String>();		
		String line = br.readLine();
		
		while (line!= null) {
			String[] parts = line.split(" ");
			commonConfig.put(parts[0], parts[1]);
			line = br.readLine();
		}
		br.close();
	}
	
	// Method to set the logger 
	public  static void setLogger() throws  IOException {
        
		File peerDir = new File("peer_" +getCommonConfig().get("peerId"));
        if(!peerDir.isDirectory()){
        	peerDir.mkdir();
        }           
        FileHandler handle = new FileHandler( peerDir.getPath() + "/" + "log_peer_" + getCommonConfig().get("peerId") + ".log");
				 
		LOGGER = Logger.getLogger("logger");
		LOGGER.setUseParentHandlers(false);
		LOGGER.setLevel(Level.INFO);
		Handler[] logHandlers = LOGGER.getHandlers();
		
		if (logHandlers!=null && logHandlers.length > 0 && logHandlers[0] instanceof ConsoleHandler) {
			LOGGER.removeHandler(logHandlers[0]);
		}		

		LineFormatter fomatter = new LineFormatter();
		handle.setFormatter(fomatter);
		LOGGER.addHandler(handle);
		
	}
	
	// method to get the logger object
	public static Logger getMyLogger(){
		return LOGGER;
	}


    public static void main(String[] args) throws IOException,UnknownHostException,NumberFormatException {
     
        int peerId = Integer.parseInt(args[0]);
        
        // setting the properties from PeerInfo and Common cfg files
        setCommonConfig();
        setPeerConfig();
        commonConfig.put("peerId", String.valueOf(peerId));
        setLogger();
        
        // set up the id of curr peer and all bit fields for curr peer
        Peer.peerId = peerId;
        BitFields.setUpBitFields();
        Peer.setUpBitFields();
        
        // get the configuration of curr peer
        String peerConfString = getPeerConfig().get(peerId);
        String portNum = peerConfString.split(" ")[2];
        
        // create a new peer process for curr peer
        PeerProcess peer = new PeerProcess();
        
        Connection connectionSetup = new Connection(peer.scheduler);
        
        // Establishing a connection all existing peers
        connectionSetup.connectExistingPeers(peerId);
        
        // Accepts connection from all peers having an ID greater than the current peerID.
        connectionSetup.connectNewPeers(peerId, Integer.valueOf(portNum));
        
        Map<String, String> comProp = getCommonConfig();
        
        int m = Integer.parseInt(comProp.get("OptimisticUnchokingInterval"));
        int k = Integer.parseInt(comProp.get("NumberOfPreferredNeighbors"));
        int p = Integer.parseInt(comProp.get("UnchokingInterval"));
        
        DetermineNeighbors determineNeighbors = new DetermineNeighbors(peer.scheduler);
        determineNeighbors.preferredNeighbours(k, p);
        determineNeighbors.optimisticallyUnchokedNeighbour(m);
        
        connectionSetup.schedulePeerTermination();
    }

    
  
}
