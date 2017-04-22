import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class HaveMsg {

	private Socket socket;
    private OutputStream outStream;
    
    public HaveMsg(Socket socket) throws IOException {
        this.socket = socket;
        outStream = new BufferedOutputStream(this.socket.getOutputStream());
    }
    
    public synchronized void sendHaveMsg(int pieceIndex) {
        byte[] actualMessage = MessageFormatter.getMsg(
        		byteArrayFormatter.convertToByteArray(pieceIndex),
                PeerThread.MsgTypes.HAVE);
        try {
            outStream.write(actualMessage);
            outStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	
}
