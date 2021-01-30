package cn.expjmicro.example.test.pubsub;

import cn.jmicro.agent.AddSerializedToObject;

public class ReportData0 {

	private Short[] types;

	private String[] labels;

	private Double[] datas;

	public Short[] getTypes() {
		return types;
	}

	public void setTypes(Short[] types) {
		this.types = types;
	}

	public String[] getLabels() {
		return labels;
	}

	public void setLabels(String[] labels) {
		this.labels = labels;
	}

	public Double[] getDatas() {
		return datas;
	}

	public void setDatas(Double[] datas) {
		this.datas = datas;
	}

	public void encode(java.io.DataOutput __buffer, Object obj) throws java.io.IOException {
		ReportData0 __obj = this;
		cn.jmicro.api.codec.JDataOutput out = (cn.jmicro.api.codec.JDataOutput) __buffer;
		cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder();
		java.lang.Short[] __val0 = __obj.types;
		byte flag0 = 0;
		int flagIndex0 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val0 == null) {
			flag0 |= cn.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex0, flag0);
		} else { // block0
			int size = __val0.length;
			__buffer.writeShort(size);
			if (size > 0) { // if block1
				boolean writeEvery = false;
				if (java.lang.Short.class != null && (java.lang.reflect.Modifier
						.isFinal(java.lang.Short.class.getModifiers())
						|| cn.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(java.lang.Short.class))) {
					flag0 |= cn.jmicro.common.Constants.GENERICTYPEFINAL;
				} else { // block2
					boolean sameElt = cn.jmicro.agent.AddSerializedToObject.sameArrayTypeEles(__val0);
					boolean isFinal = cn.jmicro.agent.AddSerializedToObject.seriaFinalClass(__val0);
					boolean hasNull = cn.jmicro.agent.AddSerializedToObject.hasNullElement(__val0);
					if (sameElt && isFinal && !hasNull) { // block3
						flag0 |= cn.jmicro.common.Constants.HEADER_ELETMENT;
						writeEvery = false;
						Short c0 = cn.jmicro.api.codec.TypeCoderFactory.getIns().getCodeByClass(__val0[0].getClass());
						if (c0 == null) {
							flag0 |= cn.jmicro.common.Constants.ELEMENT_TYPE_CODE;
							__buffer.writeUTF(__val0[0].getClass().getName());
						} //
						else {
							__buffer.writeShort(c0.intValue());
						}
					} // block3
					else { // block4
						writeEvery = true;
					} // block4
				} // block2
				for (int i = 0; i < size; i++) { // loop block5
					Object v = __val0[i];
					if (writeEvery) {
						if (v == null) {
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
							continue;
						}
						Short cc0 = cn.jmicro.api.codec.TypeCoderFactory.getIns().getCodeByClass(v.getClass());
						if (cc0 == null) {
							cn.jmicro.agent.AddSerializedToObject.errorToSerializeObjectCode(v.getClass().getName());
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_PROXY);
							__buffer.writeUTF(v.getClass().getName());
						} else {
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_SHORT);
							__buffer.writeShort(cc0.intValue());
						}
					}
					cn.jmicro.agent.AddSerializedToObject.encodeListElement(__buffer, v);
				} // end for loop block5
				out.write(flagIndex0, flag0);
			} // end if block1
		} // end else block0

		java.lang.String[] __val1 = __obj.labels;
		byte flag1 = 0;
		int flagIndex1 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val1 == null) {
			flag1 |= cn.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex1, flag1);
		} else { // block0
			int size = __val1.length;
			__buffer.writeShort(size);
			if (size > 0) { // if block1
				boolean writeEvery = false;
				boolean hasNull = cn.jmicro.agent.AddSerializedToObject.hasNullElement(__val1);
				if (!hasNull && (java.lang.reflect.Modifier
						.isFinal(java.lang.String.class.getModifiers())
						|| cn.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(java.lang.String.class))) {
					flag1 |= cn.jmicro.common.Constants.GENERICTYPEFINAL;
				} else { // block2
					boolean sameElt = cn.jmicro.agent.AddSerializedToObject.sameArrayTypeEles(__val1);
					boolean isFinal = cn.jmicro.agent.AddSerializedToObject.seriaFinalClass(__val1);
					
					if (sameElt && isFinal && !hasNull) { // block3
						flag1 |= cn.jmicro.common.Constants.HEADER_ELETMENT;
						writeEvery = false;
						Short c1 = cn.jmicro.api.codec.TypeCoderFactory.getIns().getCodeByClass(__val1[0].getClass());
						if (c1 == null) {
							flag1 |= cn.jmicro.common.Constants.ELEMENT_TYPE_CODE;
							__buffer.writeUTF(__val1[0].getClass().getName());
						} //
						else {
							__buffer.writeShort(c1.intValue());
						}
					} // block3
					else { // block4
						writeEvery = true;
					} // block4
				} // block2
				for (int i = 0; i < size; i++) { // loop block5
					Object v = __val1[i];
					if (writeEvery) {
						if (v == null) {
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
							continue;
						}
						Short cc1 = cn.jmicro.api.codec.TypeCoderFactory.getIns().getCodeByClass(v.getClass());
						if (cc1 == null) {
							cn.jmicro.agent.AddSerializedToObject.errorToSerializeObjectCode(v.getClass().getName());
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_PROXY);
							__buffer.writeUTF(v.getClass().getName());
						} else {
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_SHORT);
							__buffer.writeShort(cc1.intValue());
						}
					}
					cn.jmicro.agent.AddSerializedToObject.encodeListElement(__buffer, v);
				} // end for loop block5
				out.write(flagIndex1, flag1);
			} // end if block1
		} // end else block0

		java.lang.Double[] __val2 = __obj.datas;
		byte flag2 = 0;
		int flagIndex2 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val2 == null) {
			flag2 |= cn.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex2, flag2);
		} else { // block0
			int size = __val2.length;
			__buffer.writeShort(size);
			if (size > 0) { // if block1
				boolean writeEvery = false;
				boolean hasNull = cn.jmicro.agent.AddSerializedToObject.hasNullElement(__val2);
				if (!hasNull && (java.lang.reflect.Modifier
						.isFinal(java.lang.Double.class.getModifiers())
						|| cn.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(java.lang.Double.class))) {
					flag2 |= cn.jmicro.common.Constants.GENERICTYPEFINAL;
				} else { // block2
					boolean sameElt = cn.jmicro.agent.AddSerializedToObject.sameArrayTypeEles(__val2);
					boolean isFinal = cn.jmicro.agent.AddSerializedToObject.seriaFinalClass(__val2);
					
					if (sameElt && isFinal && !hasNull) { // block3
						flag2 |= cn.jmicro.common.Constants.HEADER_ELETMENT;
						writeEvery = false;
						Short c2 = cn.jmicro.api.codec.TypeCoderFactory.getIns().getCodeByClass(__val2[0].getClass());
						if (c2 == null) {
							flag2 |= cn.jmicro.common.Constants.ELEMENT_TYPE_CODE;
							__buffer.writeUTF(__val2[0].getClass().getName());
						} //
						else {
							__buffer.writeShort(c2.intValue());
						}
					} // block3
					else { // block4
						writeEvery = true;
					} // block4
				} // block2
				for (int i = 0; i < size; i++) { // loop block5
					Object v = __val2[i];
					if (writeEvery) {
						if (v == null) {
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
							continue;
						}
						Short cc2 = cn.jmicro.api.codec.TypeCoderFactory.getIns().getCodeByClass(v.getClass());
						if (cc2 == null) {
							cn.jmicro.agent.AddSerializedToObject.errorToSerializeObjectCode(v.getClass().getName());
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_PROXY);
							__buffer.writeUTF(v.getClass().getName());
						} else {
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_SHORT);
							__buffer.writeShort(cc2.intValue());
						}
					}
					cn.jmicro.agent.AddSerializedToObject.encodeListElement(__buffer, v);
				} // end for loop block5
				out.write(flagIndex2, flag2);
			} // end if block1
		} // end else block0

	}

	public void decode(java.io.DataInput __buffer) throws java.io.IOException {
		ReportData0 __obj = this;
		cn.jmicro.api.codec.JDataInput in = (cn.jmicro.api.codec.JDataInput) __buffer;
		java.lang.Short[] __val0;
		byte flagName0 = __buffer.readByte();
		__val0 = null;
		if (0 != (cn.jmicro.common.Constants.NULL_VAL & flagName0)) {
			__val0 = null;
		} else { // block0
			cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder();
			String clsName = null;
			short c = 0;
			int size = __buffer.readShort();
			if (size > 0) { // block2
				boolean readEvery = true;
				Class eleCls = null;
				clsName = null;
				c = 0;
				if (0 == (cn.jmicro.common.Constants.GENERICTYPEFINAL & flagName0)) { // blockgenic,不能从泛型获取足够信息
					if (0 != (cn.jmicro.common.Constants.HEADER_ELETMENT & flagName0)) {
						readEvery = false;
						if (0 == (cn.jmicro.common.Constants.ELEMENT_TYPE_CODE & flagName0)) {
							c = __buffer.readShort();
							eleCls = cn.jmicro.api.codec.TypeCoderFactory.getIns().getClassByCode(new Short(c));
						} else {
							clsName = __buffer.readUTF();
							eleCls = cn.jmicro.agent.AddSerializedToObject.loadClazz(clsName);
						}
					}
				} // blockgenic
				else {
					eleCls = java.lang.Short.class;
					readEvery = false;
				}
				__val0 = new java.lang.Short[size];
				for (int i = 0; i < size; i++) { // block5
					if (readEvery) { // block6
						short prefixCode = __buffer.readByte();
						if (prefixCode == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL) {
							__val0[i] = null;
							continue;
						}
						if (prefixCode == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_SHORT) {
							c = __buffer.readShort();
							eleCls = cn.jmicro.api.codec.TypeCoderFactory.getIns().getClassByCode(new Short(c));
						} else {
							java.lang.String cn = __buffer.readUTF();
							eleCls = AddSerializedToObject.loadClazz(cn);
						}
					} // block6
					java.lang.Short elt = (java.lang.Short) cn.jmicro.agent.AddSerializedToObject
							.decodeListElement(__buffer, eleCls);
					if (elt != null) { // block7
						__val0[i] = elt;
					} // block7
				} // block5
			} // block2
		} // block0
		__obj.types = __val0;

		java.lang.String[] __val1;
		byte flagName1 = __buffer.readByte();
		__val1 = null;
		if (0 != (cn.jmicro.common.Constants.NULL_VAL & flagName1)) {
			__val1 = null;
		} else { // block0
			cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder();
			String clsName = null;
			short c = 0;
			int size = __buffer.readShort();
			if (size > 0) { // block2
				boolean readEvery = true;
				Class eleCls = null;
				clsName = null;
				c = 0;
				if (0 == (cn.jmicro.common.Constants.GENERICTYPEFINAL & flagName1)) { // blockgenic,不能从泛型获取足够信息
					if (0 != (cn.jmicro.common.Constants.HEADER_ELETMENT & flagName1)) {
						readEvery = false;
						if (0 == (cn.jmicro.common.Constants.ELEMENT_TYPE_CODE & flagName1)) {
							c = __buffer.readShort();
							eleCls = cn.jmicro.api.codec.TypeCoderFactory.getIns().getClassByCode(new Short(c));
						} else {
							clsName = __buffer.readUTF();
							eleCls = cn.jmicro.agent.AddSerializedToObject.loadClazz(clsName);
						}
					}
				} // blockgenic
				else {
					eleCls = java.lang.String.class;
					readEvery = false;
				}
				__val1 = new java.lang.String[size];
				for (int i = 0; i < size; i++) { // block5
					if (readEvery) { // block6
						short prefixCode = __buffer.readByte();
						if (prefixCode == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL) {
							__val1[i] = null;
							continue;
						}
						if (prefixCode == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_SHORT) {
							c = __buffer.readShort();
							eleCls = cn.jmicro.api.codec.TypeCoderFactory.getIns().getClassByCode(new Short(c));
						} else {
							java.lang.String cn = __buffer.readUTF();
							eleCls = AddSerializedToObject.loadClazz(cn);
						}
					} // block6
					java.lang.String elt = (java.lang.String) cn.jmicro.agent.AddSerializedToObject
							.decodeListElement(__buffer, eleCls);
					if (elt != null) { // block7
						__val1[i] = elt;
					} // block7
				} // block5
			} // block2
		} // block0
		__obj.labels = __val1;

		java.lang.Double[] __val2;
		byte flagName2 = __buffer.readByte();
		__val2 = null;
		if (0 != (cn.jmicro.common.Constants.NULL_VAL & flagName2)) {
			__val2 = null;
		} else { // block0
			cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder();
			String clsName = null;
			short c = 0;
			int size = __buffer.readShort();
			if (size > 0) { // block2
				boolean readEvery = true;
				Class eleCls = null;
				clsName = null;
				c = 0;
				if (0 == (cn.jmicro.common.Constants.GENERICTYPEFINAL & flagName2)) { // blockgenic,不能从泛型获取足够信息
					if (0 != (cn.jmicro.common.Constants.HEADER_ELETMENT & flagName2)) {
						readEvery = false;
						if (0 == (cn.jmicro.common.Constants.ELEMENT_TYPE_CODE & flagName2)) {
							c = __buffer.readShort();
							eleCls = cn.jmicro.api.codec.TypeCoderFactory.getIns().getClassByCode(new Short(c));
						} else {
							clsName = __buffer.readUTF();
							eleCls = cn.jmicro.agent.AddSerializedToObject.loadClazz(clsName);
						}
					}
				} // blockgenic
				else {
					eleCls = java.lang.Double.class;
					readEvery = false;
				}
				__val2 = new java.lang.Double[size];
				for (int i = 0; i < size; i++) { // block5
					if (readEvery) { // block6
						short prefixCode = __buffer.readByte();
						if (prefixCode == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL) {
							__val2[i] = null;
							continue;
						}
						if (prefixCode == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_SHORT) {
							c = __buffer.readShort();
							eleCls = cn.jmicro.api.codec.TypeCoderFactory.getIns().getClassByCode(new Short(c));
						} else {
							java.lang.String cn = __buffer.readUTF();
							eleCls = AddSerializedToObject.loadClazz(cn);
						}
					} // block6
					java.lang.Double elt = (java.lang.Double) cn.jmicro.agent.AddSerializedToObject
							.decodeListElement(__buffer, eleCls);
					if (elt != null) { // block7
						__val2[i] = elt;
					} // block7
				} // block5
			} // block2
		} // block0
		__obj.datas = __val2;

		return;
	}
}
