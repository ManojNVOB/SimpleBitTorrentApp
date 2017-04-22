import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;


public class DetermineNeighbors {
	
	List<Peer> unchokedPeers, chokedPeers; 
	private final ScheduledExecutorService scheduler;
	Logger logger;
	Peer previousOptUnchokPeer;
	 
	public DetermineNeighbors(ScheduledExecutorService scheduler) {
			
			this.scheduler = scheduler;
			this.logger = PeerProcess.getMyLogger();
	}

    public void optimisticallyUnchokedNeighbour(int m) {
    	
    	final int rateInSeconds = m; 
        final Runnable findPreferedPeer = new Runnable() {

            @Override
            public void run() {
                if (chokedPeers.size()!= 0) {
                    int randPeerIndex = ThreadLocalRandom.current().nextInt(0, chokedPeers.size());
                    Peer candidatePeer = chokedPeers.remove(randPeerIndex);
                    if (candidatePeer != null && candidatePeer != previousOptUnchokPeer) {
                        candidatePeer.setOptimisticallyUnchoked(true);
                      
						candidatePeer.getPeerChokeUtil().sendUnChokeMsg();
						
                        if (previousOptUnchokPeer != null) {
                            previousOptUnchokPeer.setOptimisticallyUnchoked(false);
                            if (previousOptUnchokPeer.isChoked()) {
                                try {
									previousOptUnchokPeer.getPeerChokeUtil().sendChokeMsg();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
                            }
                        }
                        previousOptUnchokPeer = candidatePeer;
                        logger.info("Peer " + Peer.peerId + " has the optimistically unchoked neighbor " + "Peer " + candidatePeer.getId());
                    }
                }
                else if(previousOptUnchokPeer != null)
                {
                    previousOptUnchokPeer.setOptimisticallyUnchoked(false);
                    if (previousOptUnchokPeer.isChoked()) {
                        
                        try {
							previousOptUnchokPeer.getPeerChokeUtil().sendChokeMsg();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    }
                    
                    previousOptUnchokPeer = null;
                }

            }

        };
        scheduler.scheduleAtFixedRate(findPreferedPeer, rateInSeconds, rateInSeconds, SECONDS);
    }
    
    public void preferredNeighbours(final int peersCount, final int rateInSeconds) {
    	Runnable neighboursSelector = null;
    	try{
           neighboursSelector = new Runnable() {
        	
            public void run() {                	
                List<Peer> interestedList = Peer.interested;
                unchokedPeers = Collections.synchronizedList(new ArrayList<Peer>());
                chokedPeers = Collections.synchronizedList(new ArrayList<Peer>());
                int count = peersCount;
                StringBuilder unchokedPeersStr = new StringBuilder();
                
                Collections.sort(interestedList, new Comparator<Peer>(){
                	@Override
                	public int compare(Peer o1, Peer o2) {

                		return (int)(o1.getDownloadRate() - o2.getDownloadRate());
                	}
                });
                
                if (interestedList != null) {
                    for(Peer currPeer: interestedList){
                        if(currPeer.isInitialized()){
                            if(count > 0){                                  
                                unchokedPeers.add(currPeer);
                                if (currPeer.isChoked()) {
                                    currPeer.setChoked(false);
                                    if (!currPeer.isOptimisticallyUnchoked()) {                                            
                                       
									currPeer.getPeerChokeUtil().sendUnChokeMsg();
										                                        
                                    }
                                }
                                unchokedPeersStr.append(currPeer.getId() + ",");
                            } else {
                                
                                chokedPeers.add(currPeer);
                                if (!currPeer.isChoked()) {
                                    currPeer.setChoked(true);
                                    if (!currPeer.isOptimisticallyUnchoked()) {
                                
                                        
											try {
												currPeer.getPeerChokeUtil().sendChokeMsg();
											} catch (IOException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
										
                                    }
                                }
                            }
                        }
                        count--;
                    }
                    
                    String temp = unchokedPeersStr.toString();
                    String resStr = temp.substring(0,temp.lastIndexOf(","));
                    if (!resStr.isEmpty()) {
                        logger.info("Peer " + Peer.peerId + " has the preferred neighbors " + resStr);
                    }                       
                }
            }
        };
    	}
    	catch(Exception e){
    		System.out.println("do nothing");
    	}
        scheduler.scheduleAtFixedRate(neighboursSelector, rateInSeconds, rateInSeconds, SECONDS);

    }


}
