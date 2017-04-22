import java.io.IOException;
import java.io.InputStream;


public class byteArrayFormatter {
	
	public static int byteArrayToInt(byte[] b) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }

    public static byte[] convertToByteArray(final int integer) {
        byte[] result = new byte[4];

        result[0] = (byte) ((integer & 0xFF000000) >> 24);
        result[1] = (byte) ((integer & 0x00FF0000) >> 16);
        result[2] = (byte) ((integer & 0x0000FF00) >> 8);
        result[3] = (byte) (integer & 0x000000FF);

        return result;
    }

    public static byte[] readBytes(InputStream in, byte[] byteArray, int numBytesToRead) throws IOException {
        int len = numBytesToRead;
        int idx = 0;
        while (len != 0) {
            int bytesRemainingToRead = in.available();
            int bytesToRead;
            if(numBytesToRead <= bytesRemainingToRead){
            	bytesToRead = numBytesToRead;
            } else{
            	bytesToRead = bytesRemainingToRead;
            }
            byte[] dataRead = new byte[bytesToRead];
            if (bytesToRead != 0) {
				in.read(dataRead);
	            byteArray = ConcatenateBytesUtil.concatenateByteArrays(byteArray, idx, dataRead, bytesToRead);
	            idx += bytesToRead;
	            len -= bytesToRead;
            }
        }
        return byteArray;
    }

}
