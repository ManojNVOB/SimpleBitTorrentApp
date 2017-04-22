import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


public class HandShakeMsgs {
	
	private Socket socket;
	private static Map<Integer, Boolean> handshakeMap = new HashMap<Integer, Boolean>();
	private OutputStream out ;
    private InputStream in;
	private int id;
	private boolean client = false;
	public static final byte[] handshakeHeader = "P2PFILESHARINGPROJ".getBytes();
	
	public HandShakeMsgs(Socket s, int id, boolean client) throws IOException {
		this.socket = s;
		this.id = id;
		this.client = client;
		out = new BufferedOutputStream(socket.getOutputStream());
        in = new BufferedInputStream(socket.getInputStream());
	}
	
	public synchronized void sendHandshake() throws IOException {
        synchronized (handshakeMap) {
        	
        	// 10 byte zeros added to the handshake header
        	byte[] zeroBytes = new byte[10];
        	for(int i=0; i<10; i++){
        		zeroBytes[i] = 0; 
        	}
           
            byte[] concatenateHeaderWithZeroBits = ConcatenateBytesUtil.concatenateByteArrays(handshakeHeader,
            		zeroBytes);
            byte[] finalHandshakeMsg = ConcatenateBytesUtil.concatenateByteArrays(concatenateHeaderWithZeroBits,
            		 PeerProcess.getCommonConfig().get("peerId").getBytes());
           
            out.write(finalHandshakeMsg);
            out.flush();
            handshakeMap.put(id, false);
           
        }
	}
	
    public synchronized int receiveHandshake() throws IOException {
        
        byte[] b = new byte[32];
        in.read(b);
        byte[] copyOfRange = Arrays.copyOfRange(b, 28, 32);
        String peer = new String(copyOfRange);
        Integer peerId = Integer.parseInt(peer);
        if (client) {
            if (handshakeMap.containsKey(peerId) && handshakeMap.get(peerId) == false) {
                handshakeMap.put(peerId, true);
            }
        }
        return peerId;
    }
    
    public static byte[] getHandShakeMsg(int toPeerId) {
    	

    	byte[] zeroBytes = new byte[10];
    	for(int i=0; i<10; i++){
    		zeroBytes[i] = 0; 
    	}
    	
    	byte[] concatenateHeaderWithZeroBits = ConcatenateBytesUtil.concatenateByteArrays(handshakeHeader,
    			zeroBytes);
    	byte[] finalHandshakeMsg = ConcatenateBytesUtil.concatenateByteArrays(concatenateHeaderWithZeroBits,
    			byteArrayFormatter.convertToByteArray(toPeerId));
    	return finalHandshakeMsg;
       
    }
}
