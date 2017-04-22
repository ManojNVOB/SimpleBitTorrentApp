import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Peer {
	
	private Socket socket;
    private OutputStream outStream;
    private InputStream inStream;
    private byte[] peerBitFieldMsg;
    private boolean choked = true;
    private int id;
    private boolean client = false;
    private boolean optUnchoke = false;
    private long downloadRate = 0;
    public static int peerId = 0;
    private int requestedIndex;
    private Boolean initialized = false;
    private  static byte[] requestedBitField = BitFields.getRequestedBitField();
    private  static byte[] finalBitField = BitFields.getFinalBitField();
    private static byte[] currBitField = BitFields.getCurrBitField();
    private ChokeMsg peerChokeUtil;
    
    
    
    public static byte[] fileData = BitFields.getFileData();
    
    public static List<Peer> interested = Collections.synchronizedList(new ArrayList<Peer>());

    public static Map<Integer, Peer> notInterested = Collections.synchronizedMap(new HashMap<Integer, Peer>());

    // Map for peers who choked the current peer
    public static Map<Integer, Peer> chokedMap = Collections.synchronizedMap(new HashMap<Integer, Peer>());
    
    // Map for peers who unchoked the current peer
    public static Map<Integer, Peer> unchokedMap = Collections.synchronizedMap(new HashMap<Integer, Peer>());

    public static Map<Integer, Long> requestTime = Collections.synchronizedMap(new HashMap<Integer, Long>());

    public static Map<Integer, Long> downloadTime = Collections.synchronizedMap(new ConcurrentHashMap<Integer, Long>());
    

    public Peer(Socket socket) throws IOException {
        this.socket = socket;
        outStream = new BufferedOutputStream(this.socket.getOutputStream());
        inStream = new BufferedInputStream(this.socket.getInputStream());  
    }
    

    public synchronized long getDownloadRate() {
        return downloadRate;
    }

    public synchronized void setDownloadRate(final long d) {
        downloadRate = -d;
    }
    
    public void setOptimisticallyUnchoked(boolean status){
        optUnchoke = status;
    }

    public void setClientValue(final boolean clientValue) {
        client = clientValue;
    }

    public boolean isClient() {
        return client;
    }
    
    public boolean isOptimisticallyUnchoked(){
        return  optUnchoke;
    }

    public synchronized  void setRequestedIndex(final int idx) {
        requestedIndex = idx;
    }

    public synchronized  int getRequestedIndex() {
        return requestedIndex;
    }
    
    public void setId(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
    
    public synchronized void setChoked(boolean n) {
        choked = n;
    }

    public synchronized boolean isChoked() {
        return choked;
    }
    

    public static synchronized boolean isInterestedPeer(Peer peer){
        for(Peer p : Peer.interested){
            if(p.getId() == peer.getId()){
                return true;
            }
        }
        return false;
    }

    public static synchronized void removeFromInterested(Peer peer){
        Peer.interested.remove(peer);
    }

    public static synchronized void addToInterested(Peer peer){
        Peer.interested.add(peer);
    }

    
    public static byte[] getFinalBitField() {
        return finalBitField;
    }

    

    public static synchronized byte[] getCurrBitField() {
        return currBitField;
    }
    
    public static synchronized void setCurrBitFieldIndex(int index, int i) {
        
        currBitField[index] |= (1 << (7 - i));
    }

    public boolean isInitialized(){
        if(initialized)
        	return true;
        return false;
    }
    public synchronized void initialized() throws InterruptedException {
        while (!initialized) {
        	wait(1000);
        }
    }

    public synchronized  void setInitToTrue(){
        initialized = true;
        notify();
    }

   
    public static synchronized byte[] getRequestedBitField() {
        return requestedBitField;
    }

   

    public synchronized void setRequestedPieceIndex(int index, int indexFromRight) {
        
        requestedBitField[index] |= (1 << indexFromRight);
        
    }

    public static synchronized  void resetRequestedPieceIndex(int index, int indexFromLeft) {
        
        requestedBitField[index] &= ~(1 << (7 - indexFromLeft));
        
    }

    public static void setLastByteToZero(byte[] bitField){
    	
    	bitField[bitField.length - 1] = 0;
    	
    }
    
    public static void setLastNBytesToOne(byte[] bitField, int N){
    	
    	bitField[bitField.length - 1] |= (1 << (8 - N));    	
    }


    public synchronized byte[]  getPeerBitFieldMsg(){
        return peerBitFieldMsg;
    }
    

    public static void setUpBitFields() throws IOException{
        
        requestedBitField = BitFields.getRequestedBitField();
        finalBitField = BitFields.getFinalBitField();
        currBitField = BitFields.getCurrBitField();
        fileData = BitFields.getFileData();
        

    }

    public synchronized void sendBitfieldMsg() throws IOException {
        
        byte[] currentBitfield = getCurrBitField();
        byte[] msg = MessageFormatter.getMsg(currentBitfield,
                    PeerThread.MsgTypes.BITFIELD);
        outStream.write(msg);
        outStream.flush();
        
    }
    
    public static byte[] readMsg(InputStream in, PeerThread.MsgTypes bitfield) throws IOException {
        byte[] lengthByte = new byte[4];
        int read = Integer.MIN_VALUE;
        byte[] data = null;
        read = in.read(lengthByte);
        
        int dataLength = byteArrayFormatter.byteArrayToInt(lengthByte);
        byte[] msgType = new byte[1];
        in.read(msgType);
    
            int actualDataLength = dataLength - 1;
            data = new byte[actualDataLength];
            data = byteArrayFormatter.readBytes(in, data, actualDataLength);
  
            if (msgType[0] != bitfield.value){
            	System.out.println("bit field "+ bitfield.value);
                System.out.println("msgType "+ msgType[0]);
                System.out.println("message type of sent message is not correct");
            }

        return data;
    }

    public synchronized  void updateBitFieldMsg(int idx) {
        peerBitFieldMsg[idx / 8] |= (1 << (7 - (idx % 8)));
    }

    public synchronized void getBitfieldMsg() throws IOException {
        peerBitFieldMsg = readMsg(inStream,
        		PeerThread.MsgTypes.BITFIELD);
    }

    
    public synchronized boolean isInterested() {
        
        int idx = 0;
        byte[] currentBitField = getCurrBitField();
        byte[] resultBitField = new byte[currentBitField.length];
        for (byte byt : currentBitField) {
            resultBitField[idx] = (byte) (byt ^ peerBitFieldMsg[idx]);
            idx++;
        }
        idx = 0;

        for (byte b : currentBitField) {

            resultBitField[idx] = (byte) (resultBitField[idx] & ~b);
            if (resultBitField[idx] != 0) {
                return true;
            }
        }
        return false;
    }


    public synchronized void sendRequestMsg(int idx) throws IOException {
      
        byte[] byteArray = byteArrayFormatter.convertToByteArray(idx);
        byte[] msg = MessageFormatter.getMsg(byteArray, PeerThread.MsgTypes.REQUEST);
        outStream.write(msg);
        outStream.flush();
        Peer.requestTime.put(id, System.nanoTime());  
  
    }

    public synchronized void sendPieceMsg(int idx) throws IOException {
        
        int pieceSz = Integer.parseInt(PeerProcess.getCommonConfig().get("PieceSize"));
        int startIdx = pieceSz * idx;
        int endIdx = startIdx + pieceSz - 1;
        endIdx = endIdx >= fileData.length ? endIdx = fileData.length - 1 : endIdx;
        
        byte[] content = new byte[endIdx - startIdx + 5]; 
        byte[] byteArray = byteArrayFormatter.convertToByteArray(idx);
        for (int i = 0; i < 4; i++) {
            content[i] = byteArray[i];
        }

        int i = startIdx; 
        while ( i <= endIdx) {
            content[i - startIdx + 4] = fileData[i];
            i++;
        }

        byte[] msg = MessageFormatter.getMsg(content,PeerThread.MsgTypes.PIECE);
        outStream.write(msg);
        outStream.flush();
    }

    public synchronized int requestNewPiece() {
        
        byte[] requestedBitfield = getRequestedBitField();
        byte[] notRequested = new byte[peerBitFieldMsg.length]; 
        byte[] bitfield = new byte[peerBitFieldMsg.length];
        byte[] currentBitfield = getCurrBitField();

        int i=0;
        while (i < requestedBitfield.length) {
            bitfield[i] = (byte) (requestedBitfield[i] & currentBitfield[i]);
            i++;
        }

        i=0;
        while (i < bitfield.length) {
            notRequested[i] = (byte) ((bitfield[i] ^ peerBitFieldMsg[i]) & ~bitfield[i]);
            i++;
        }

        ArrayList<Integer> bytes = new ArrayList<>();
        i=0;
        while (i < notRequested.length) {
            byte temp = notRequested[i];
            if(temp != 0){
                bytes.add(i);
            }
            i++;
        }

        if (bytes.isEmpty()) {
            return -1;
        }
        int randomValue = bytes.get(ThreadLocalRandom.current().nextInt(0, bytes.size()));
        int idx = ThreadLocalRandom.current().nextInt(0, 8);
        byte b = notRequested[randomValue];
        while(b == 0 || (b & (1 << idx)) == 0){
            randomValue = ThreadLocalRandom.current().nextInt(0, notRequested.length);
            idx = ThreadLocalRandom.current().nextInt(0, 8);
            b = notRequested[randomValue];
        }
        setRequestedPieceIndex(randomValue, idx);
        int index = randomValue*8 + 7 - idx;
        setRequestedIndex(index);
        return index;

    }
    
    public void close() throws IOException {
        socket.close();
	}
	
	@Override
	public void finalize() throws IOException {
	    this.close();
	}


	public ChokeMsg getPeerChokeUtil() {
		return peerChokeUtil;
	}


	public void setPeerChokeUtil(ChokeMsg peerChokeUtil) {
		this.peerChokeUtil = peerChokeUtil;
	}
}
