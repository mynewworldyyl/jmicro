package cn.jmicro.api.codec;

import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import cn.jmicro.api.net.Message;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

public class JDataOutput implements DataOutput {

	private ByteBuffer buf;
	
	public JDataOutput() {
		this(512);
	}
	
	public JDataOutput(int initSize) {
		buf = ByteBuffer.allocate(initSize);
	}
	
	private static ByteBuffer checkCapacity(ByteBuffer buf,int need) {
		if(buf.remaining() >= need) {
			return buf;
		}
		
		int size = buf.capacity();
		for(;size < need;) {
			size *= 2;
		}
		
		size += buf.position();
		
		ByteBuffer bb = ByteBuffer.allocate(size);
		buf.flip();
		bb.put(buf);
		return bb;
		
	}
	
	public int position() {
		return buf.position();
	}
	
	public void position(int pos) {
		buf.position(pos);
	}
	
	public void write(int index,byte v) {
		 buf.put(index, v);
	}
	
	@Override
	public void write(int b) throws IOException {
		this.writeByte(b);
	}

	@Override
	public void write(byte[] b) {
		buf = checkCapacity(buf,b.length);
		buf.put(b);
	}
	
	public void write(ByteBuffer b) {
		buf = checkCapacity(buf,b.remaining());
		buf.put(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		buf = checkCapacity(buf,len);
		buf.put(b,off,len);
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		buf = checkCapacity(buf,1);
		this.writeByte(v ? 1 : 0);
	}
	
	@Override
	public void writeByte(int v) throws IOException {
		buf = checkCapacity(buf,Byte.BYTES);
		buf.put((byte)v);
	}
	
	public void writeByte(Byte v) throws IOException {
		buf = checkCapacity(buf,Byte.BYTES);
		buf.put((byte)v);
	}

	@Override
	public void writeShort(int v) throws IOException {
		buf = checkCapacity(buf,Short.BYTES);
		buf.putShort((short)v);
	}
	
	public void writeShort(Short v) throws IOException {
		buf = checkCapacity(buf,Short.BYTES);
		buf.putShort((short)v);
	}

	@Override
	public void writeChar(int v) throws IOException {
		buf = checkCapacity(buf,Character.BYTES);
		buf.putChar((char)v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		buf = checkCapacity(buf,Integer.BYTES);
		buf.putInt(v);
		//encodeInt(v);
	}
	
	public void writeInt(Integer v) throws IOException {
		buf=checkCapacity(buf,Integer.BYTES);
		buf.putInt(v);
		//encodeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		buf = checkCapacity(buf,Long.BYTES);
		buf.putLong(v);
	}
	
	public void writeLong(Long v) throws IOException {
		buf=checkCapacity(buf,Long.BYTES);
		buf.putLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		buf=checkCapacity(buf,Float.BYTES);
		buf.putFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		buf=checkCapacity(buf,Double.BYTES);
		buf.putDouble(v);
	}
	
	public void writeDouble(Double v) throws IOException {
		buf=checkCapacity(buf,Double.BYTES);
		buf.putDouble(v);
	}

	@Override
	public void writeBytes(String s) throws IOException {
		writeUTF(s);
	}

	@Override
	public void writeChars(String s) throws IOException {
		writeUTF(s);
	}

	@Override
	public void writeUTF(String s) throws IOException {
		buf = writeString(buf,s);
	}
	
	public static int encodeStringLen(String s) {
		byte[] data;
		try {
			data = s.getBytes(Constants.CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new CommonException("writeStringLen:"+s,e);
		}
		if(data.length < Byte.MAX_VALUE) {
			return data.length+1;
		}else if(data.length < Short.MAX_VALUE) {
			//0X7F=01111111=127 byte
			//0X0100=00000001 00000000=128 short
			return data.length+3;
		}else if(data.length < Integer.MAX_VALUE) {
			return data.length+7;
		}else {
			throw new CommonException("String too long for:" + data.length);
		}
	}
	
	public static ByteBuffer writeString(ByteBuffer buf, String s) throws IOException {

		if(s == null) {
			buf = checkCapacity(buf,1);
			//buf.putShort((short)0);
			if(buf.remaining() <= 0) {
				System.out.print(buf);
			}
			buf.put((byte)-1);
			return buf;
		}
		
		byte[] data = s.getBytes(/*"GBK"*/Constants.CHARSET);
		
		if(data.length < Byte.MAX_VALUE) {
			buf = checkCapacity(buf,data.length+1);
			buf.put((byte)data.length);
		}else if(data.length < Short.MAX_VALUE) {
			//0X7F=01111111=127 byte
			//0X0100=00000001 00000000=128 short
			buf = checkCapacity(buf,data.length+3);
			buf.put(Byte.MAX_VALUE);
			buf.putShort((short)data.length);
		}else if(data.length < Integer.MAX_VALUE) {
			buf = checkCapacity(buf,data.length+7);
			buf.put(Byte.MAX_VALUE);
			buf.putShort(Short.MAX_VALUE);
			buf.putInt(data.length);
		}else {
			throw new CommonException("String too long for:" + data.length);
		}
		if(data.length > 0) {
			buf.put(data, 0, data.length);
		}
		return buf;
	}

	public ByteBuffer getBuf() {
		buf.flip();
		return buf;
	}

	public void writeUnsignedByte(short v) {
		 Message.writeUnsignedByte(buf,v);
	}
	
	public void writeUnsignedShort(int v){
		 Message.writeUnsignedShort(buf,v);
	}
	
	public void writeUnsignedInt(long v){
		 Message.writeUnsignedInt(this.buf,v);
	}
	
	public void writeUnsignedLong(long v){
		 Message.wiriteUnsignedLong(buf, v);
	}
	
}
