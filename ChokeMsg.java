import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class ChokeMsg {
	private Socket socket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    
    public ChokeMsg(Socket socket) throws IOException {
        this.socket = socket;
        outStream = new BufferedOutputStream(this.socket.getOutputStream());
        inStream = new BufferedInputStream(this.socket.getInputStream());  
    }
	
    public synchronized  void sendChokeMsg() throws IOException {
        
    	byte[] actualMessage = MessageFormatter.getMsg(PeerThread.MsgTypes.CHOKE);
        outStream.write(actualMessage);
        outStream.flush();
    }

        public synchronized void sendUnChokeMsg() {
        byte[] actualMessage = MessageFormatter.getMsg(PeerThread.MsgTypes.UNCHOKE);
        try {
			outStream.write(actualMessage);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        try {
			outStream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}
