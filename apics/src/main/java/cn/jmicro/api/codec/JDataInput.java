package cn.jmicro.api.codec;

import java.io.DataInput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import cn.jmicro.api.net.Message;
import cn.jmicro.common.Constants;

public class JDataInput implements DataInput {
	
	private ByteBuffer buf ;
	
	public JDataInput(ByteBuffer buf) {
		this.buf = buf;
	}
	
	public int remaining() {
		return this.buf.remaining();
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
	
	public Boolean readBoolean0() throws IOException {
		return new Boolean(buf.get() == 1);
	}

	@Override
	public byte readByte() throws IOException {
		return buf.get();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		//throw new IOException("Not support readUnsignedByte");
		return Message.readUnsignedByte(buf);
	}
	
	@Override
	public int readUnsignedShort() throws IOException {
		return Message.readUnsignedShort(buf);
	}
	
	/*
	public long readUnsignedInt() throws IOException{
		return Message.readUnsignedInt(this.buf);
	}

	public long readUsignedLong() {
		return Message.readUnsignedLong(buf);
	}*/
	
	@Override
	public short readShort() throws IOException {
		return buf.getShort();
	}

	@Override
	public char readChar() throws IOException {
		return buf.getChar();
	}

	@Override
	public int readInt() throws IOException {
		return buf.getInt();
		//return readInt0();
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
	
	public static String readString(JDataInput buffer) {
		return readString(buffer.buf);
	}
	
	public static String readString(ByteBuffer buffer) {
		
		int len = buffer.get();
		if(len == -1) {
			return null;
		}else if(len == 0) {
			return "";
		}
		
		if(len == Byte.MAX_VALUE) {
			len = buffer.getShort();
			if(len == Short.MAX_VALUE) {
				len = buffer.getInt();
			}
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
	
	public static String readString(byte[] data,int pos) {
		
		int len = data[pos];
		pos += 1;
		
		if(len == -1) {
			return null;
		}else if(len == 0) {
			return "";
		}
		
		if(len == Byte.MAX_VALUE) {
			len = Message.readUnsignedShort(data, pos);
			pos += 2;
			if(len == Short.MAX_VALUE) {
				len = Message.readInt(data, pos);
				pos += 4;
			}
		}
		try {
			return new String(data,pos,len,Constants.CHARSET);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static int stringTakeLen(byte[] data,int pos) {
		
		int tl = 1;
		
		int len = data[pos];
		if(len == -1 || len == 0) {
			return tl;
		}
		
		pos += 1;
		
		if(len == Byte.MAX_VALUE) {
			len = Message.readUnsignedShort(data, pos);
			tl += 2;
			
			pos += 2;
			if(len == Short.MAX_VALUE) {
				len = Message.readInt(data, pos);
				pos += 4;
				tl += 4;
			}
		}
		
		return tl+len;
	}

}
