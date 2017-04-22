import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;


public class BitFields {
	
	
    private  static byte[] requestedBitField;
    public static byte[] getRequestedBitField() {
		return requestedBitField;
	}


	public static void setRequestedBitField(byte[] requestedBitField) {
		BitFields.requestedBitField = requestedBitField;
	}

	private  static byte[] finalBitField;
    public static byte[] getFinalBitField() {
		return finalBitField;
	}


	public static void setFinalBitField(byte[] finalBitField) {
		BitFields.finalBitField = finalBitField;
	}

	private static byte[] currBitField;
	public static byte[] getCurrBitField() {
		return currBitField;
	}


	public static void setCurrBitField(byte[] currBitField) {
		BitFields.currBitField = currBitField;
	}

	private static byte[] fileData;
	
	 public static byte[] getFileData() {
		return fileData;
	}


	public static void setFileData(byte[] fileData) {
		BitFields.fileData = fileData;
	}


	public static void setUpBitFields() throws IOException{
	        
	        String fileName = "peer_"+Peer.peerId+"/"+PeerProcess.getCommonConfig().get("FileName");
	        Integer fileSize = Integer.parseInt(PeerProcess.getCommonConfig().get("FileSize"));
	        File file = new File(fileName);
	        
	        long numPieces = 0;
	        int pieceSize = Integer.parseInt(PeerProcess.getCommonConfig().get("PieceSize"));
	        if(fileSize % pieceSize == 0){
	        	numPieces = fileSize / pieceSize;
	        }
	        else{
	        	numPieces = fileSize / pieceSize + 1;
	        }
	        
	        
	        int numBits = (int) numPieces % 8;
	        
	        double bitfieldSize = Math.ceil(numPieces / 8.0f);
	        
	        currBitField = new byte[(int) bitfieldSize];
	        finalBitField = new byte[(int) bitfieldSize];
	        requestedBitField = new byte[(int) bitfieldSize];
	        
	        fileData = new byte[Integer.parseInt(PeerProcess.getCommonConfig().get("FileSize"))];
	        if (file.exists()) 
	        {
	            if (file.length() != fileSize) {
	                System.exit(-1);
	            } else {	// Read the contents of the file
	                FileInputStream fileInputStream = new FileInputStream(file);
	                fileInputStream.read(fileData);
	                fileInputStream.close();
	            }
	            
	            Arrays.fill(currBitField, (byte) 255);
	            Arrays.fill(finalBitField, (byte) 255);
	            System.out.println("currBitfield "+Arrays.toString(currBitField)+"\n");
	            //System.out.println(finalBitField);
	            if (numBits != 0) {
	                
	                setLastByteToZero(currBitField); 
	                setLastByteToZero(finalBitField);
	                while (numBits != 0) {
	                    setLastNBytesToOne(currBitField, numBits);
	                	setLastNBytesToOne(finalBitField, numBits);
	                    numBits--;
	                }
	            }
	        }
	        else {
	        	Arrays.fill(finalBitField, (byte) 255);
	            if (numBits != 0) {
	                setLastByteToZero(finalBitField);
	                while (numBits != 0) {
	                	setLastNBytesToOne(finalBitField, numBits);
	                    numBits--;
	                }
	            }
	        }

	    }	

	    public static void setLastByteToZero(byte[] bitField){
	    	
	    	bitField[bitField.length - 1] = 0;
	    	
	    }
	    
	    public static void setLastNBytesToOne(byte[] bitField, int N){
	    	
	    	bitField[bitField.length - 1] |= (1 << (8 - N));    	
	    }
}
