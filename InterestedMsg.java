import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class InterestedMsg {
	
	private Socket socket;
    private OutputStream outStream;
    private InputStream inStream;
    
    public InterestedMsg(Socket socket) throws IOException {
        this.socket = socket;
        outStream = new BufferedOutputStream(this.socket.getOutputStream());
        inStream = new BufferedInputStream(this.socket.getInputStream());  
    }
	
    public synchronized void sendInterestedMsg() throws IOException {
        
        byte[] actualMessage = MessageFormatter
                .getMsg(PeerThread.MsgTypes.INTERESTED);
        
        outStream.write(actualMessage);
        outStream.flush();       
    }
    
    public synchronized void sendNotInterestedMsg() {
        byte[] actualMessage = MessageFormatter
                .getMsg(PeerThread.MsgTypes.NOT_INTERESTED);
        try {
            outStream.write(actualMessage);
            outStream.flush();

        } catch (IOException e) {
            
            e.printStackTrace();
        }
    }
}
