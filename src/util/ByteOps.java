package util;

public class ByteOps {

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static int appendBytes(byte[] data, byte[] destArray, int offset) {
		for (int pos = 0; pos < data.length; pos++) {
			destArray[pos + offset] = data[pos];
		}
		return data.length;
	}

	public static byte[] subArray(byte[] data, int size, int offset) {
		byte[] out = new byte[size];
		for (int pos = 0; pos < size; pos++) {
			out[pos] = data[pos + offset];
		}
		return out;
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static void printHexBytes(String bytes) {
		String newStr = "";
		int pos = 0;
		while (pos < bytes.length()) {
			newStr = newStr + bytes.substring(pos, pos + 1);
			pos++;
			if (pos % 4 == 0) {
				newStr = newStr + " ";
			}
		}

		System.out.println(newStr);
	}

	public static byte[] invertEndian(byte[] oldValue) {
		byte[] out = new byte[oldValue.length];
		for (int pos = 0; pos < out.length; pos++) {
			out[pos] = oldValue[oldValue.length - pos - 1];
		}
		return out;
	}
	
	public static boolean memComp(byte[] lhs, byte[] rhs){
		if(lhs.length != rhs.length){
			return false;
		}
		
		for(int counter = 0; counter < lhs.length; counter++){
			if(lhs[counter] != rhs[counter]){
				return false;
			}
		}
		
		return true;
	}
}
