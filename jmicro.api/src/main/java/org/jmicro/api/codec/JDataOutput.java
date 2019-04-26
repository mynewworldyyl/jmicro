package org.jmicro.api.codec;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jmicro.common.Constants;

public class JDataOutput implements DataOutput {

	private ByteBuffer buf;
	
	public JDataOutput() {
		this(512);
	}
	
	public JDataOutput(int initSize) {
		buf = ByteBuffer.allocate(initSize);
	}
	
	private void checkCapacity(int need) {
		if(buf.remaining() >= need) {
			return;
		}
		
		int size = buf.capacity();
		for(;size < need;) {
			size *= 2;
		}
		
		size += buf.position();
		
		ByteBuffer bb = ByteBuffer.allocate(size);
		buf.flip();
		bb.put(buf);
		buf = bb;
		
	}
	
	@Override
	public void write(int b) throws IOException {
		this.writeByte(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		checkCapacity(b.length);
		buf.put(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		checkCapacity(len);
		buf.put(b,off,len);
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		checkCapacity(1);
		this.writeByte(v ? 1 : 0);
	}

	@Override
	public void writeByte(int v) throws IOException {
		checkCapacity(Byte.BYTES);
		buf.put((byte)v);
	}

	@Override
	public void writeShort(int v) throws IOException {
		checkCapacity(Short.BYTES);
		buf.putShort((short)v);
	}

	@Override
	public void writeChar(int v) throws IOException {
		checkCapacity(Character.BYTES);
		buf.putChar((char)v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		checkCapacity(Integer.BYTES);
		//buf.putInt(v);
		encodeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		checkCapacity(Long.BYTES);
		buf.putLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		checkCapacity(Float.BYTES);
		buf.putFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		checkCapacity(Double.BYTES);
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
		if(s == null || s.length() == 0) {
			checkCapacity(2);
			buf.putShort((short)0);
			return;
		}
		
		byte[] data = s.getBytes(Constants.CHARSET);
		checkCapacity(data.length+2);
		buf.putShort((short)data.length);
		buf.put(data, 0, data.length);
	}

	public ByteBuffer getBuf() {
		buf.flip();
		return buf;
	}

	private void encodeInt(int n) {
		// move sign to low-order bit, and flip others if negative
		n = (n << 1) ^ (n >> 31);
		if ((n & ~0x7F) != 0) {
			buf.put((byte) ((n | 0x80) & 0xFF));
			n >>>= 7;
			if (n > 0x7F) {
				buf.put((byte) ((n | 0x80) & 0xFF));
				n >>>= 7;
				if (n > 0x7F) {
					buf.put((byte) ((n | 0x80) & 0xFF));
					n >>>= 7;
					if (n > 0x7F) {
						buf.put((byte) ((n | 0x80) & 0xFF));
						n >>>= 7;
					}
				}
			}
		}
		buf.put((byte) n);
	}
	
}
