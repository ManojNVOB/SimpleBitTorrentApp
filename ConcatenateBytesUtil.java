import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class ConcatenateBytesUtil {
	
	public static byte[] concatenateByteArrays(byte[] array1, byte[] array2) {
		        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
			out.write(array1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			out.write(array2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return out.toByteArray();
    }

    

    public static byte[] concatenateByteArray(byte b, byte[] a) {
            	
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
			out.write(a);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        out.write(b);
        return out.toByteArray();
        
    }
    
    public static byte[] concatenateByteArrays(byte[] array1, int aLength, byte[] array2, int bLength) {
        
    	
    	byte[] a = new byte[aLength];
    	for(int i=0; i<aLength; i++){
    		a[i] = array1[i];
    	}
    	
    	byte[] b = new byte[bLength];
    	for(int i=0; i<bLength; i++){
    		b[i] = array2[i];
    	}
    	
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
			out.write(a);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			out.write(b);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return out.toByteArray();
    }
}
