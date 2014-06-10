package message;

import java.net.InetAddress;
import java.net.UnknownHostException;

import data.Contact;

import util.ByteOps;

public class CommonStructures {

	private static final byte[] ipv4Preamble = ByteOps.hexStringToByteArray("00000000000000000000ffff");

	public static byte[] networkShort(int value) {
		String hexStr = Integer.toHexString(value);
		if (hexStr.length() > 4) {
			hexStr = hexStr.substring(hexStr.length() - 4, hexStr.length());
		}
		while (hexStr.length() < 4) {
			hexStr = "0" + hexStr;
		}
		return ByteOps.hexStringToByteArray(hexStr);
	}

	public static byte[] fixedSmallEndianInt(int value) {
		String hexStr = Integer.toHexString(value);
		while (hexStr.length() < 8) {
			hexStr = "0" + hexStr;
		}
		byte[] bigEndianValue = ByteOps.hexStringToByteArray(hexStr);
		return ByteOps.invertEndian(bigEndianValue);
	}

	public static int extractSmallEndianInt(byte[] value) {
		byte[] bigEndianValue = ByteOps.invertEndian(value);
		return Integer.valueOf(ByteOps.bytesToHex(bigEndianValue), 16);
	}

	public static byte[] fixedSmallEndianLong(long value) {
		String hexStr = Long.toHexString(value);
		while (hexStr.length() < 16) {
			hexStr = "0" + hexStr;
		}
		byte[] bigEndianValue = ByteOps.hexStringToByteArray(hexStr);
		return ByteOps.invertEndian(bigEndianValue);
	}

	public static byte[] varInt(long value) {
		String hexStr = Long.toHexString(value);
		String prefix = null;

		if (value < 253) {
			while (hexStr.length() < 2) {
				hexStr = "0" + hexStr;
			}
		} else if (value <= 65535) {
			while (hexStr.length() < 4) {
				hexStr = "0" + hexStr;
			}
			prefix = "fd";
		} else if (value <= Math.pow(2, 32) - 1.0) {
			while (hexStr.length() < 8) {
				hexStr = "0" + hexStr;
			}
			prefix = "fe";
		} else {
			while (hexStr.length() < 16) {
				hexStr = "0" + hexStr;
			}
			prefix = "ff";
		}

		byte[] valueBytes = ByteOps.invertEndian(ByteOps.hexStringToByteArray(hexStr));
		byte[] prefixBytes = null;
		if (prefix != null) {
			prefixBytes = ByteOps.hexStringToByteArray(prefix);
		}

		if (prefixBytes != null) {
			byte[] out = new byte[prefixBytes.length + valueBytes.length];
			ByteOps.appendBytes(prefixBytes, out, 0);
			ByteOps.appendBytes(valueBytes, out, prefixBytes.length);
			return out;
		} else {
			return valueBytes;
		}
	}

	public static long extractVarInt(byte[] data) {
		byte[] leadByte = ByteOps.subArray(data, 1, 0);
		String leadByteStr = ByteOps.bytesToHex(leadByte);

		int length = 0;
		int start = 1;

		if (leadByteStr.equalsIgnoreCase("fd")) {
			length = 2;
		} else if (leadByteStr.equalsIgnoreCase("fe")) {
			length = 4;
		} else if (leadByteStr.equalsIgnoreCase("ff")) {
			length = 8;
		} else {
			length = 1;
			start = 0;
		}

		byte[] intBytes = ByteOps.invertEndian(ByteOps.subArray(data, length, start));
		String intStr = ByteOps.bytesToHex(intBytes);
		return Long.parseLong(intStr, 16);
	}

	public static int getVarIntSize(byte[] data) {
		byte[] leadByte = ByteOps.subArray(data, 1, 0);
		String leadByteStr = ByteOps.bytesToHex(leadByte);

		if (leadByteStr.equalsIgnoreCase("fd")) {
			return 3;
		} else if (leadByteStr.equalsIgnoreCase("fe")) {
			return 5;
		} else if (leadByteStr.equalsIgnoreCase("ff")) {
			return 9;
		} else {
			return 1;
		}
	}

	public static byte[] varString(String value) {

		int size = 0;
		if (value != null) {
			size = value.length();
		}

		/*
		 * Build the size bytes and then append them in
		 */
		byte[] sizeBytes = CommonStructures.varInt(size);
		byte[] full = new byte[sizeBytes.length + size];
		ByteOps.appendBytes(sizeBytes, full, 0);

		if (size > 0) {
			byte[] charBytes = value.getBytes();
			ByteOps.appendBytes(charBytes, full, sizeBytes.length);
		}

		return full;
	}

	public static String extractVarString(byte[] data) {
		// XXX will this ever be larger than an int?
		int size = (int) CommonStructures.extractVarInt(data);

		if (size == 0) {
			return null;
		}

		int offset = CommonStructures.getVarIntSize(data);
		byte[] strData = ByteOps.subArray(data, size, offset);

		return new String(strData);
	}

	public static int getVarStrSize(byte[] data) {
		int size = (int) CommonStructures.extractVarInt(data);
		int lengthSize = CommonStructures.getVarIntSize(data);

		return lengthSize + size;
	}

	// TODO IPv6 support
	public static byte[] netAddress(InetAddress addy, int port, boolean includeTS) {
		byte[] outBytes = null;
		int tsOffset = 0;

		if (includeTS) {
			outBytes = new byte[30];
			// TODO impl time stamp
			// ByteOps.appendBytes(data, outBytes, 0);
			tsOffset = 4;
		} else {
			outBytes = new byte[26];
			tsOffset = 0;
		}

		ByteOps.appendBytes(Version.DEFAULT_SERVICE_BYTES, outBytes, tsOffset);
		ByteOps.appendBytes(ByteOps.hexStringToByteArray("ffff"), outBytes, tsOffset + 8 + 10);
		ByteOps.appendBytes(addy.getAddress(), outBytes, tsOffset + 8 + 12);
		ByteOps.appendBytes(CommonStructures.networkShort(port), outBytes, tsOffset + 8 + 16);

		return outBytes;
	}

	public static Contact extractNetAddress(byte[] data) {
		byte[] tsBytes = ByteOps.subArray(data, 4, 0);
		byte[] fullIPBytes = ByteOps.subArray(data, 16, 4 + 8);
		byte[] portBytes = ByteOps.subArray(data, 2, 4 + 8 + 16);

		long ts = (long) CommonStructures.extractSmallEndianInt(tsBytes);
		int port = Integer.parseInt(ByteOps.bytesToHex(portBytes), 16);
		InetAddress ip = null;

		byte[] ipPremableSection = ByteOps.subArray(fullIPBytes, 12, 0);
		if (ByteOps.memComp(ipPremableSection, CommonStructures.ipv4Preamble)) {
			try {
				ip = InetAddress.getByAddress(ByteOps.subArray(fullIPBytes, 4, 12));
			} catch (UnknownHostException e) {
				System.err.println("Invalid IPv4 address presented: "
						+ ByteOps.bytesToHex(ByteOps.subArray(fullIPBytes, 4, 12)));
				return null;
			}
		} else {
			try {
				ip = InetAddress.getByAddress(fullIPBytes);
			} catch (UnknownHostException e) {
				System.err.println("Invalid IPv6 address presented: " + ByteOps.bytesToHex(fullIPBytes));
				return null;
			}
		}
		
		return new Contact(ip, port, ts, false);
	}

	public static void main(String args[]) {
		byte[] aBytes = CommonStructures.varString("hi there");

		String aStr = CommonStructures.extractVarString(aBytes);

		System.out.println(aStr);
		System.out.println(aBytes.length + " " + CommonStructures.getVarStrSize(aBytes));
	}

}
