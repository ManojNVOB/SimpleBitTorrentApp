import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Logger;


public class PeerThread extends Thread {	
	
    private static final Logger logger = PeerProcess.getMyLogger();
    private Socket socket;
    private boolean isClient = false;
    private boolean terminateThread = false;
    private Peer newPeer;
    InterestedMsg peerInterestUtil;
    private HaveMsg peerHaveUtil;
    private HandShakeMsgs handshake;
    private  Thread init;
	private ChokeMsg peerChokeUtil;
	
	public enum MsgTypes{
		
		CHOKE((byte)0), 
		UNCHOKE((byte)1), 
		INTERESTED((byte)2), 
		NOT_INTERESTED((byte)3), 
		HAVE((byte)4), 
		BITFIELD((byte)5), 
		REQUEST((byte)6), 
		PIECE((byte)7);
		byte value = Byte.MIN_VALUE;
		
		private MsgTypes(byte n){
			this.value = n;
		}
		
	}
	
    public Peer getPeer() {
        return newPeer;
    }

    public InterestedMsg getInterested() {
        return peerInterestUtil;
    }
    
    public HaveMsg getHaveMsg() {
        return peerHaveUtil;
    }

    
    public PeerThread(Socket s, boolean client, int pId) throws IOException {
        this.socket = s;
        this.isClient = client;
        try {
			newPeer = new Peer(socket);
		} catch (IOException e) {
			System.out.println("problem creating peer object");
			e.printStackTrace();
		}
        
        peerInterestUtil = new InterestedMsg(socket);
        peerHaveUtil = new HaveMsg(socket);
        peerChokeUtil  = new ChokeMsg(socket);
        
        newPeer.setPeerChokeUtil(peerChokeUtil);
        
        try {
			handshake = new HandShakeMsgs(socket, pId, client);
		} catch (IOException e) {
			System.out.println("error creating handshake object");
			e.printStackTrace();
		}
        
        if (isClient == false){
            int peerId = handshake.receiveHandshake();
            newPeer.setId(peerId);
            handshake.sendHandshake();
        }
        
        if (isClient == true) {
            newPeer.setId(pId);
            newPeer.setClientValue(true);
            handshake.sendHandshake();
            handshake.receiveHandshake();
        } 
       

        init = new Thread() {
            public void run() {
            
                try {
					newPeer.sendBitfieldMsg();
				} catch (IOException e) {
					System.out.println("error sending bit field message");
					e.printStackTrace();
				}
                try {
					newPeer.getBitfieldMsg();
				} catch (IOException e1) {
					System.out.println("error in getting bitfield message");
					e1.printStackTrace();
				}

                if (newPeer.isInterested() == true) {
                    
                    try {
						peerInterestUtil.sendInterestedMsg();
					} catch (IOException e) {
						System.out.println("error sending Interested message");
						e.printStackTrace();
					}
                } 
                
                if (newPeer.isInterested() == false){
                    peerInterestUtil.sendNotInterestedMsg();
                }

                if (isClient == true) {
                    logger.info("Peer " + PeerProcess.getCommonConfig().get("peerId")
                            + " makes a connection to Peer " + newPeer.getId());
                } else {
                    logger.info("Peer " + PeerProcess.getCommonConfig().get("peerId")
                            + " is connected from " + newPeer.getId());
                }
                newPeer.setInitToTrue();
            }

        };
        init.start();
    }
    

    public boolean isThreadTerminated() {
        return terminateThread;
    }

    public boolean isCurrentByteZero(byte b, int idx){
    	
    	if ((b & (1 << (7 - (idx % 8)))) == 0) {
    		return true;
    	}
    	return false;
    }
    
    public void terminateSocket(boolean client) {
        terminateThread = client;
        if(client == true){
	        if(socket.isClosed() == false)
	        {
	            try {
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
        }
    }
    
    public static MsgTypes getMsgType(byte[] msgStat) {
        for (MsgTypes actMsgType : MsgTypes.values()) {
            if (actMsgType.value == msgStat[4]) {
                return actMsgType;
            }
        }
        return null;
    }
       
    @Override
    public void run() {
    
    	try {
			newPeer.initialized();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        try {
            InputStream is = new BufferedInputStream(socket.getInputStream());
            while (!isThreadTerminated()) {
                byte[] msg = new byte[5];
                msg = byteArrayFormatter.readBytes(is, msg, 5);
                MsgTypes message = getMsgType(msg);
                
                if(message ==  MsgTypes.BITFIELD){
                	// do nothing
                }
                
                else if(message == MsgTypes.HAVE){

                    byte[] readPieceIndexBytes = new byte[4];
                    readPieceIndexBytes = byteArrayFormatter.readBytes(is, readPieceIndexBytes, 4);
                    int piece = byteArrayFormatter.byteArrayToInt(readPieceIndexBytes);
                    byte[] currentBitfield = Peer.getCurrBitField();

                    if (isCurrentByteZero(currentBitfield[piece / 8], piece)) {
                    	peerInterestUtil.sendInterestedMsg();
                    }
                    newPeer.updateBitFieldMsg(piece);
                    logger.info("Peer " + Peer.peerId+ " received the have message from " + newPeer.getId() +" for the piece "
                    		+ piece);
                }
                
                else if(message == MsgTypes.CHOKE){
                	
                    int idx = newPeer.getRequestedIndex();
                    byte[] currentBitfield = Peer.getCurrBitField();
                    
                    if (isCurrentByteZero(currentBitfield[idx / 8], idx)) {
                        Peer.resetRequestedPieceIndex(idx / 8, idx % 8);
                    }

                    Peer.chokedMap.put(newPeer.getId(), newPeer);
                    logger.info("Peer " + Peer.peerId + " is choked by " + newPeer.getId());
                }
                
                else if(message == MsgTypes.UNCHOKE){

                    Peer.unchokedMap.put(newPeer.getId(), newPeer);
                   
                    logger.info("Peer " + Peer.peerId + " is unchoked by " + newPeer.getId());
                    int nextIndex = newPeer.requestNewPiece();

                    if (nextIndex != -1) {
                        newPeer.sendRequestMsg(nextIndex);
                    }
                    
                    if( nextIndex == -1 && !(Arrays.equals(newPeer.getCurrBitField(), newPeer.getPeerBitFieldMsg())))
                    {
                    	peerInterestUtil.sendInterestedMsg();
                    }
                    
                    if(nextIndex == -1){
                        peerInterestUtil.sendNotInterestedMsg();
                    }
                }
                
                else if(message == MsgTypes.INTERESTED){

                    if(!Peer.isInterestedPeer(newPeer)){
                        Peer.addToInterested(newPeer);
                    }
                    logger.info("Peer " + Peer.peerId+ " received the interested message from "+ newPeer.getId());
                }
                
                else if(message == MsgTypes.NOT_INTERESTED){

                	Peer.removeFromInterested(newPeer);
                    newPeer.setChoked(true);
                    Peer.notInterested.put(newPeer.getId(), newPeer);
                    logger.info("Peer " + Peer.peerId+ " received the not interested message from "+ newPeer.getId());
                }
                
                else if(message == MsgTypes.PIECE){

                	byte[] pieceByteArray = new byte[4];
                    pieceByteArray[0] = msg[0];
                	pieceByteArray[1] = msg[1];
                	pieceByteArray[2] = msg[2];
                	pieceByteArray[3] = msg[3];
                	
                	int pieceSz = byteArrayFormatter.byteArrayToInt(pieceByteArray) - 1;
                    int payloadSz = pieceSz - 4;
                    
                
                    byte[] pieceIndexBytes = new byte[4];
                    pieceIndexBytes = byteArrayFormatter.readBytes(is, pieceIndexBytes, 4);
                    
                    
                    
                    byte[] piece = new byte[payloadSz];
                    piece = byteArrayFormatter.readBytes(is, piece, payloadSz);
                    Long downloadTime = - Peer.requestTime.get(newPeer.getId());

                    Peer.downloadTime.put(newPeer.getId(), downloadTime);
                    newPeer.setDownloadRate(downloadTime);
                    int pieceIdx = byteArrayFormatter.byteArrayToInt(pieceIndexBytes);
                    Peer.setCurrBitFieldIndex(pieceIdx / 8, pieceIdx % 8);
                    
                    // broadcast "HAVE" message to other running peers
                    for (PeerThread p : Connection.neighbours) {
                        p.getHaveMsg().sendHaveMsg(pieceIdx);
                    }
                    
                 
                    for (int i = 0; i < payloadSz; i++) {
                        Peer.fileData[pieceIdx * Integer.parseInt(PeerProcess.getCommonConfig().get("PieceSize")) + i] = piece[i];
                    }
                    
                    logger.info("Peer " + Peer.peerId + " has downloaded the piece " + pieceIdx + " from " + newPeer.getId());

                    int newPieceIndex = newPeer.requestNewPiece();
                    
                    if(newPieceIndex == -1){
                        
                        peerInterestUtil.sendNotInterestedMsg();
                    }
                    
                    else if (newPieceIndex != -1 && Peer.unchokedMap.containsKey(newPeer.getId())) {
                    
                        newPeer.sendRequestMsg(newPieceIndex);
                    }
                    
                    else if(newPieceIndex == -1 && !(Arrays.equals(newPeer.getCurrBitField(), newPeer.getPeerBitFieldMsg())))
                    {
                    
                    	peerInterestUtil.sendInterestedMsg();
                    }

                    else if(newPieceIndex == -1 && Arrays.equals(Peer.getCurrBitField(), Peer.getFinalBitField())){
                    
                        for (PeerThread peerThread : Connection.neighbours) {
                            peerThread.getInterested().sendNotInterestedMsg();
                    
                        }
                    
                        File file = new File("peer_"+Peer.peerId + "/"+PeerProcess.getCommonConfig().get("FileName"));
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        fileOutputStream.write(Peer.fileData);
                    
                    }
                }
                else if(message == MsgTypes.REQUEST){
                    
                    byte[] ind = new byte[4];
                    is.read(ind);
                    int pIndex = byteArrayFormatter.byteArrayToInt(ind);
                    if (!newPeer.isChoked() | newPeer.isOptimisticallyUnchoked()) {
                        newPeer.sendPieceMsg(pIndex);
                    }

                }
                
            }

        } catch (IOException e) {
            if(!isThreadTerminated()) {
                e.printStackTrace();
            }
        }finally {
            init.interrupt();
        }

    }
}

