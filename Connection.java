import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

public class Connection {
	
	private final ScheduledExecutorService scheduler;
	int acceptConnections;
	Logger logger;
	public static Set<PeerThread> neighbours = new HashSet<PeerThread>();
	
	  
	 public Connection(ScheduledExecutorService s) {
			this.scheduler = s;
			this.logger = PeerProcess.getMyLogger();
		}
	 
		
    public void connectExistingPeers(int serverId) throws NumberFormatException, UnknownHostException, IOException {
        
    	Map<Integer,String> peerConf = PeerProcess.getPeerConfig();
    	
        for (Integer clientId : peerConf.keySet()) {
            
        	if (clientId < serverId) {
                
        		String line = peerConf.get(clientId);
                String[] parts = line.split(" ");
                String clientAddress = parts[1];
                String portNum = parts[2];
                
                Socket socket = new Socket(clientAddress, Integer.parseInt(portNum));
                PeerThread peerThread = new PeerThread(socket, true, clientId);
                peerThread.start();
                neighbours.add(peerThread);

            }

        }
    }
	 
	public void connectNewPeers(int serverId, int port) {

        final int portNum = port;
		Map<Integer, String> peerConf = PeerProcess.getPeerConfig();
        
		// count all the peers with greater id
        for (Integer pId : peerConf.keySet()) {
            
        	if (pId > serverId) {
                acceptConnections++;
            }
        	
        }
        
        Thread thread = new Thread() {
            public void run() {
                ServerSocket serverSocket=null;
                Socket clientSocket=null;
				try {
					serverSocket = new ServerSocket(portNum);
				} catch (IOException e) {
					System.out.println("error creating socket");
					e.printStackTrace();
				} 
                    while (acceptConnections > 0) { 
                    	
						try {
							clientSocket = serverSocket.accept();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println("error in accepting peer connection");
							e.printStackTrace();
						}
						
                        if (clientSocket != null) {
                            PeerThread neighbourThread = null;
							try {
								neighbourThread = new PeerThread(clientSocket, false, -1);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                            neighbourThread.start();
                            neighbours.add(neighbourThread);
                            acceptConnections--;
                        }
                        
                    }

            }
        };
        thread.start();
    }

    
    public void schedulePeerTermination() {
        final Runnable exitPeerCheck = new Runnable() {
            @Override
            public void run() {
                byte[] finalBitField = Peer.getFinalBitField();
                byte[] currBitField = Peer.getCurrBitField();

                if (Arrays.equals(currBitField, finalBitField) == true) {
                    if (neighbours.size() > 0) {
                        boolean shutDown = true;
                        for (PeerThread p : neighbours) {
                            byte[] pBitFieldMsg = p.getPeer().getPeerBitFieldMsg();
                            if (Arrays.equals(pBitFieldMsg, finalBitField) == false) {
                                shutDown = false;
                                break;
                            }
                        }
                        if (shutDown) {
                            for (PeerThread p : neighbours) {
                                p.terminateSocket(true);
                                p.interrupt();
                            }

                            scheduler.shutdownNow();

/*                            try {
                                boolean status = scheduler.awaitTermination(5, SECONDS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }finally {*/
                                logger.info("Peer " + Peer.peerId + " has downloaded the complete file ");
                                logger.info("Exiting Peer " + Peer.peerId);
 /*                           }*/
                        }
                    }

                }
            }
        };
        scheduler.scheduleAtFixedRate(exitPeerCheck, 3, 3, SECONDS);

    }
    
}
