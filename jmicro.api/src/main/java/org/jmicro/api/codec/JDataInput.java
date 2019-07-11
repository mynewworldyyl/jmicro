package org.jmicro.api.codec;

import java.io.DataInput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.jmicro.common.Constants;

public class JDataInput implements DataInput {
	
	private ByteBuffer buf ;
	
	public JDataInput(ByteBuffer buf) {
		this.buf = buf;
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		buf.get(b);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		buf.get(b,off,len);
	}

	@Override
	public int skipBytes(int n) throws IOException {
		if(n > buf.remaining()) {
			throw new IOException("Data end when skip "+n +" bytes");
		}
		this.buf.position(buf.position()+n);
		return n;
	}

	@Override
	public boolean readBoolean() throws IOException {
		return buf.get() == 1;
	}

	@Override
	public byte readByte() throws IOException {
		return buf.get();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		throw new IOException("Not support readUnsignedByte");
	}

	@Override
	public short readShort() throws IOException {
		return buf.getShort();
	}

	@Override
	public int readUnsignedShort() throws IOException {
		throw new IOException("Not support readUnsignedShort");
	}

	@Override
	public char readChar() throws IOException {
		return buf.getChar();
	}

	@Override
	public int readInt() throws IOException {
		//return buf.getInt();
		return readInt0();
	}

	@Override
	public long readLong() throws IOException {
		return buf.getLong();
	}

	@Override
	public float readFloat() throws IOException {
		return buf.getFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return buf.getDouble();
	}

	@Override
	public String readLine() throws IOException {
		return readUTF();
	}

	@Override
	public String readUTF() throws IOException {
		return readString(buf);
	}
	
	public static String readString(ByteBuffer buffer) {
		
		int len = buffer.get();
		if(len == Byte.MAX_VALUE) {
			len = buffer.getShort();
			if(len == Short.MAX_VALUE) {
				len = buffer.getInt();
			}
		}
		if(len <= 0) {
			return null;
		}
		byte[] data = new byte[len];
		buffer.get(data,0,len);
		try {
			return new String(data,Constants.CHARSET);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	private int readInt0() throws IOException{
		int b = buf.get() & 0xff;
		int n = b & 0x7f;
		if (b > 0x7f) {
			b = buf.get() & 0xff;
			n ^= (b & 0x7f) << 7;
			if (b > 0x7f) {
				b = buf.get() & 0xff;
				n ^= (b & 0x7f) << 14;
				if (b > 0x7f) {
					b = buf.get() & 0xff;
					n ^= (b & 0x7f) << 21;
					if (b > 0x7f) {
						b = buf.get() & 0xff;
						n ^= (b & 0x7f) << 28;
						if (b > 0x7f) {
							throw new IOException("Invalid int encoding");
						}
					}
				}
			}
		}
		return (n >>> 1) ^ -(n & 1); // back to two's-complement
	}
	
}
