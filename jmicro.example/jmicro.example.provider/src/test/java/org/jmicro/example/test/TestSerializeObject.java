package org.jmicro.example.test;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.jmicro.api.codec.ISerializeObject;

public class TestSerializeObject implements Serializable,ISerializeObject{

	/*private String val1111 = "dfsadfffffffffffff";
	
	private byte bvssssssssssss = 1;
	
	private int iv222222222222222 = 9;
	
	private short sv33333333333333 = 52;
	
	private long lvdddddddddddddd = 2;
	
	private float fv222222222222222222222222 = 2222;
	
	private double dv22234324 = 22;
	
	private char cvfdksafjdlaj = '2';
	
	private Date date = new Date();*/
	
	//public Set<Person> setv = new HashSet<>();
	
	//public List<Person> listv = new ArrayList<>();
	
	/*public List<Person> listv = new ArrayList<>();
	
	public Map<String,Person> mapv = new HashMap<>();*/
	
	//private Person p = new Person();
	
	public Set<Integer> seti = new HashSet<>();
	
	public void encode(java.io.DataOutput __buffer, Object obj) throws IOException {
		org.jmicro.example.test.TestSerializeObject __obj = this;
		java.util.Set __val0 = __obj.seti;
		org.jmicro.api.codec.JDataOutput out = (org.jmicro.api.codec.JDataOutput) __buffer;
		byte flag0 = 0;
		int flagIndex0 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val0 == null) {
			flag0 = org.jmicro.agent.SerializeProxyFactory.setNull(flag0);
			out.write(flagIndex0, flag0);
		} else { // block0
			System.out.println("out:" + out);
			System.out.println("__buffer:" + __buffer);
			System.out.println("__val:" + __val0);
			org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();
			Short c = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val0.getClass());
			if (c == null) {
				flag0 = org.jmicro.agent.SerializeProxyFactory.setType(flag0);
				__buffer.writeUTF(__val0.getClass().getName());
			} else {
				__buffer.writeShort(c.intValue());
			}
			int size = __val0.size();
			__buffer.writeShort(size);
			if (size > 0) { // if block1
				boolean writeEvery = false;
				if (java.lang.Integer.class != null && (java.lang.reflect.Modifier
						.isFinal(java.lang.Integer.class.getModifiers())
						|| org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(java.lang.Integer.class))) {
					flag0 = org.jmicro.agent.SerializeProxyFactory.setGenericTypeFinal(flag0);
				} else { // block2
					boolean sameElt = org.jmicro.agent.SerializeProxyFactory.sameCollectionTypeEles(__val0);
					boolean isFinal = org.jmicro.agent.SerializeProxyFactory
							.seriaFinalClass(__val0.iterator().next().getClass());
					if (sameElt && isFinal) { // block3
						flag0 = org.jmicro.agent.SerializeProxyFactory.setHeaderELetment(flag0);
						writeEvery = false;
						Short c0 = org.jmicro.api.codec.TypeCoderFactory
								.getCodeByClass(__val0.iterator().next().getClass());
						if (c == null) {
							flag0 = org.jmicro.agent.SerializeProxyFactory.setElementTypeCode(flag0);
							__buffer.writeUTF(__val0.iterator().next().getClass().getName());
						} //
						else {
							__buffer.writeShort(c0.intValue());
						}
					} // block3
					else { // block4
						writeEvery = true;
					} // block4
				} // block2
				java.util.Iterator ite = __val0.iterator();
				while (ite.hasNext()) { // loop block5
					Object v = ite.next();
					if (writeEvery) {
						Short cc0 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(v.getClass());
						__buffer.writeShort(cc0.intValue());
					}
					org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer, v);
				} // end for loop block5
				out.write(flagIndex0, flag0);
			} // end if block1
		} // end else block0

	}
	
	public void decode(java.io.DataInput __buffer) throws IOException {
		org.jmicro.example.test.TestSerializeObject __obj = this;
		java.util.Set __val0;
		byte flagName0 = __buffer.readByte();
		System.out.println("Decoder Flag: " + flagName0);
		__val0 = null;
		if (org.jmicro.agent.SerializeProxyFactory.isNull(flagName0)) {
			__val0 = null;
		} else { // block0
			org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();
			String clsName = null;
			short c = 0;
			if (org.jmicro.agent.SerializeProxyFactory.isType(flagName0)) {
				clsName = __buffer.readUTF();
			} else {
				c = __buffer.readShort();
			}
			if (__obj.seti == null) { // block0
				Class cls = null;
				if (clsName != null) {
					cls = org.jmicro.agent.SerializeProxyFactory.loadClazz(clsName);
				} else {
					cls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
				}
				__val0 = (java.util.Set) org.jmicro.agent.SerializeProxyFactory.newInstance(cls);
				__obj.seti = __val0;
			} // block0
			else { // block1
				__val0 = __obj.seti;
			} // block1
			int size = __buffer.readShort();
			if (size > 0) { // block2
				boolean readEvery = true;
				Class eleCls = null;
				clsName = null;
				c = 0;
				if (!org.jmicro.agent.SerializeProxyFactory.isGenericTypeFinal(flagName0)) { // blockgenic
					if (org.jmicro.agent.SerializeProxyFactory.isHeaderELetment(flagName0)) {
						readEvery = false;
						if (org.jmicro.agent.SerializeProxyFactory.isElementTypeCode(flagName0)) {
							c = __buffer.readShort();
							eleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
						} else {
							clsName = __buffer.readUTF();
							eleCls = org.jmicro.agent.SerializeProxyFactory.loadClazz(clsName);
						}
					}
				} // blockgenic
				else {
					eleCls = java.lang.Integer.class;
					readEvery = false;
				}
				int cnt = 0;
				while (cnt < size) { // block5
					++cnt;
					if (readEvery) { // block6
						c = __buffer.readShort();
						eleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
					} // block6
					Object elt = org.jmicro.agent.SerializeProxyFactory.decodeListElement(__buffer, eleCls);
					if (elt != null) { // block7
						__val0.add(elt);
					} // block7
				} // block5
			} // block2
		} // block0
		__obj.seti = __val0;

	}

}
