
public class MessageFormatter {
	
	public static byte[] getMsg(String payload, PeerThread.MsgTypes msgType) {
        int l = payload.getBytes().length;
        byte[] msgL = byteArrayFormatter.convertToByteArray(l + 1); 
        byte[] concatenateMsgType = ConcatenateBytesUtil.concatenateByteArray(msgType.value, payload.getBytes());
        byte[] finalMsg = ConcatenateBytesUtil.concatenateByteArrays(msgL,concatenateMsgType);
        return finalMsg;
    }

    public static byte[] getMsg(PeerThread.MsgTypes msgType) {
        byte[] msgL = byteArrayFormatter.convertToByteArray(1);
        byte[] finalMsg = ConcatenateBytesUtil.concatenateByteArray(msgType.value, msgL);
        return finalMsg;
    }

    public static byte[] getMsg(byte[] payload, PeerThread.MsgTypes msgType) {
        byte[] msgL = byteArrayFormatter.convertToByteArray(payload.length + 1); 
        byte[] concatenateMsgType = ConcatenateBytesUtil.concatenateByteArray(msgType.value, msgL);
        byte[] finalMsg = ConcatenateBytesUtil.concatenateByteArrays(concatenateMsgType,payload);
        return finalMsg;
    }

}
