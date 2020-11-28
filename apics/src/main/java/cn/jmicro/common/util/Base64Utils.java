package cn.jmicro.common.util;

public class Base64Utils {

	private static final byte[] ENC_TAB = {65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83,
			84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112,
			113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47};

	private static final byte[] DEC_TAB = new byte[128];
	
	public static String encodeToStr(String inStr) throws Exception {
		byte[] tmp = encode(inStr.getBytes("UTF-8"));
		return new String(tmp);
	}

	public static String decodeToStr(String inStr) throws Exception {
		byte[] tmp = decode(inStr.getBytes());
		return new String(tmp, "UTF-8");
	}

	public static byte[] encode(byte[] data) {
		int modulus = data.length % 3;
		byte[] bytes;
		if (modulus == 0) {
			bytes = new byte[4 * data.length / 3];
		} else {
			bytes = new byte[4 * (data.length / 3 + 1)];
		}
		int dataLength = data.length - modulus;

		int i = 0;
		for (int j = 0; i < dataLength; j += 4) {
			int a1 = data[i] & 0xFF;
			int a2 = data[(i + 1)] & 0xFF;
			int a3 = data[(i + 2)] & 0xFF;
			bytes[j] = ENC_TAB[(a1 >>> 2 & 0x3F)];
			bytes[(j + 1)] = ENC_TAB[((a1 << 4 | a2 >>> 4) & 0x3F)];
			bytes[(j + 2)] = ENC_TAB[((a2 << 2 | a3 >>> 6) & 0x3F)];
			bytes[(j + 3)] = ENC_TAB[(a3 & 0x3F)];

			i += 3;
		}

		switch (modulus) {
			case 0 :
				break;
			case 1 :
				int d1 = data[(data.length - 1)] & 0xFF;
				int b1 = d1 >>> 2 & 0x3F;
				int b2 = d1 << 4 & 0x3F;
				bytes[(bytes.length - 4)] = ENC_TAB[b1];
				bytes[(bytes.length - 3)] = ENC_TAB[b2];
				bytes[(bytes.length - 2)] = 61;
				bytes[(bytes.length - 1)] = 61;
				break;
			case 2 :
				int d11 = data[(data.length - 2)] & 0xFF;
				int d2 = data[(data.length - 1)] & 0xFF;
				int b11 = d11 >>> 2 & 0x3F;
				int b21 = (d11 << 4 | d2 >>> 4) & 0x3F;
				int b3 = d2 << 2 & 0x3F;
				bytes[(bytes.length - 4)] = ENC_TAB[b11];
				bytes[(bytes.length - 3)] = ENC_TAB[b21];
				bytes[(bytes.length - 2)] = ENC_TAB[b3];
				bytes[(bytes.length - 1)] = 61;
		}

		return bytes;
	}

	public static byte[] decode(byte[] data) {
		data = discardNonBase64Bytes(data);
		byte[] bytes;
		if (data[(data.length - 2)] == 61) {
			bytes = new byte[(data.length / 4 - 1) * 3 + 1];
		} else {
			if (data[(data.length - 1)] == 61) {
				bytes = new byte[(data.length / 4 - 1) * 3 + 2];
			} else {
				bytes = new byte[data.length / 4 * 3];
			}
		}
		int i = 0;
		for (int j = 0; i < data.length - 4; j += 3) {
			byte b1 = DEC_TAB[data[i]];
			byte b2 = DEC_TAB[data[(i + 1)]];
			byte b3 = DEC_TAB[data[(i + 2)]];
			byte b4 = DEC_TAB[data[(i + 3)]];
			bytes[j] = (byte) (b1 << 2 | b2 >> 4);
			bytes[(j + 1)] = (byte) (b2 << 4 | b3 >> 2);
			bytes[(j + 2)] = (byte) (b3 << 6 | b4);

			i += 4;
		}

		if (data[(data.length - 2)] == 61) {
			byte b1 = DEC_TAB[data[(data.length - 4)]];
			byte b2 = DEC_TAB[data[(data.length - 3)]];
			bytes[(bytes.length - 1)] = (byte) (b1 << 2 | b2 >> 4);
		} else if (data[(data.length - 1)] == 61) {
			byte b1 = DEC_TAB[data[(data.length - 4)]];
			byte b2 = DEC_TAB[data[(data.length - 3)]];
			byte b3 = DEC_TAB[data[(data.length - 2)]];
			bytes[(bytes.length - 2)] = (byte) (b1 << 2 | b2 >> 4);
			bytes[(bytes.length - 1)] = (byte) (b2 << 4 | b3 >> 2);
		} else {
			byte b1 = DEC_TAB[data[(data.length - 4)]];
			byte b2 = DEC_TAB[data[(data.length - 3)]];
			byte b3 = DEC_TAB[data[(data.length - 2)]];
			byte b4 = DEC_TAB[data[(data.length - 1)]];
			bytes[(bytes.length - 3)] = (byte) (b1 << 2 | b2 >> 4);
			bytes[(bytes.length - 2)] = (byte) (b2 << 4 | b3 >> 2);
			bytes[(bytes.length - 1)] = (byte) (b3 << 6 | b4);
		}
		return bytes;
	}

	public static byte[] decode(String data) {
		data = discardNonBase64Chars(data);
		byte[] bytes;
		if (data.charAt(data.length() - 2) == '=') {
			bytes = new byte[(data.length() / 4 - 1) * 3 + 1];
		} else {
			if (data.charAt(data.length() - 1) == '=') {
				bytes = new byte[(data.length() / 4 - 1) * 3 + 2];
			} else {
				bytes = new byte[data.length() / 4 * 3];
			}
		}
		int i = 0;
		for (int j = 0; i < data.length() - 4; j += 3) {
			byte b1 = DEC_TAB[data.charAt(i)];
			byte b2 = DEC_TAB[data.charAt(i + 1)];
			byte b3 = DEC_TAB[data.charAt(i + 2)];
			byte b4 = DEC_TAB[data.charAt(i + 3)];
			bytes[j] = (byte) (b1 << 2 | b2 >> 4);
			bytes[(j + 1)] = (byte) (b2 << 4 | b3 >> 2);
			bytes[(j + 2)] = (byte) (b3 << 6 | b4);

			i += 4;
		}

		if (data.charAt(data.length() - 2) == '=') {
			byte b1 = DEC_TAB[data.charAt(data.length() - 4)];
			byte b2 = DEC_TAB[data.charAt(data.length() - 3)];
			bytes[(bytes.length - 1)] = (byte) (b1 << 2 | b2 >> 4);
		} else if (data.charAt(data.length() - 1) == '=') {
			byte b1 = DEC_TAB[data.charAt(data.length() - 4)];
			byte b2 = DEC_TAB[data.charAt(data.length() - 3)];
			byte b3 = DEC_TAB[data.charAt(data.length() - 2)];
			bytes[(bytes.length - 2)] = (byte) (b1 << 2 | b2 >> 4);
			bytes[(bytes.length - 1)] = (byte) (b2 << 4 | b3 >> 2);
		} else {
			byte b1 = DEC_TAB[data.charAt(data.length() - 4)];
			byte b2 = DEC_TAB[data.charAt(data.length() - 3)];
			byte b3 = DEC_TAB[data.charAt(data.length() - 2)];
			byte b4 = DEC_TAB[data.charAt(data.length() - 1)];
			bytes[(bytes.length - 3)] = (byte) (b1 << 2 | b2 >> 4);
			bytes[(bytes.length - 2)] = (byte) (b2 << 4 | b3 >> 2);
			bytes[(bytes.length - 1)] = (byte) (b3 << 6 | b4);
		}
		return bytes;
	}

	private static byte[] discardNonBase64Bytes(byte[] data) {
		byte[] temp = new byte[data.length];
		int bytesCopied = 0;
		for (int i = 0; i < data.length; ++i) {
			if (!(isValidBase64Byte(data[i])))
				continue;
			temp[(bytesCopied++)] = data[i];
		}

		byte[] newData = new byte[bytesCopied];
		System.arraycopy(temp, 0, newData, 0, bytesCopied);
		return newData;
	}

	private static String discardNonBase64Chars(String data) {
		StringBuffer sb = new StringBuffer();
		int length = data.length();
		for (int i = 0; i < length; ++i) {
			if (!(isValidBase64Byte((byte) data.charAt(i))))
				continue;
			sb.append(data.charAt(i));
		}

		return sb.toString();
	}

	private static boolean isValidBase64Byte(byte b) {
		if (b == 61) {
			return true;
		}
		if ((b < 0) || (b >= 128)) {
			return false;
		}

		return (DEC_TAB[b] != -1);
	}

	static {
		for (int i = 0; i < 128; ++i) {
			DEC_TAB[i] = -1;
		}
		for (int i = 65; i <= 90; ++i) {
			DEC_TAB[i] = (byte) (i - 65);
		}
		for (int i = 97; i <= 122; ++i) {
			DEC_TAB[i] = (byte) (i - 97 + 26);
		}
		for (int i = 48; i <= 57; ++i) {
			DEC_TAB[i] = (byte) (i - 48 + 52);
		}
		DEC_TAB[43] = 62;
		DEC_TAB[47] = 63;
	}
	
}
