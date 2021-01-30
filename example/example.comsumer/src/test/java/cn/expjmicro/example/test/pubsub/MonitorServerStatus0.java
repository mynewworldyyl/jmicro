package cn.expjmicro.example.test.pubsub;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.monitor.MC;

public class MonitorServerStatus0 {

public static final short receiveItemCount = MC.Ms_ReceiveItemCnt;
	
	public static final short receiveItemQps = MC.Ms_FailItemCount;
	
	public static final short submitTaskCount = MC.Ms_CheckerSubmitItemCnt;
	
	public static final short taskNormalCount = MC.Ms_TaskSuccessItemCnt;
	
	public static final short taskExceptionCount = MC.Ms_TaskFailItemCnt;
	
	public static final short submitCount = MC.Ms_SubmitCnt;
	
	public static final short submitQps = MC.Ms_Fail2BorrowBasket;

	public static final Short[] TYPES = { receiveItemCount, receiveItemQps, submitTaskCount, taskNormalCount,
			taskExceptionCount, submitQps, submitCount };

	private Map<String, Set> subsriber2Types = new HashMap<>();

	private String instanceName;

	private int subsriberSize;

	private int sendCacheSize;

	private Short[] types = null;

	private double[] qps = null;
	private double[] cur = null;
	private double[] total = null;

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public int getSubsriberSize() {
		return subsriberSize;
	}

	public void setSubsriberSize(int subsriberSize) {
		this.subsriberSize = subsriberSize;
	}

	public int getSendCacheSize() {
		return sendCacheSize;
	}

	public void setSendCacheSize(int sendCacheSize) {
		this.sendCacheSize = sendCacheSize;
	}

	public Map<String, Set> getSubsriber2Types() {
		return subsriber2Types;
	}

	public void setSubsriber2Types(Map<String, Set> subsriber2Types) {
		this.subsriber2Types = subsriber2Types;
	}

	public Short[] getTypes() {
		return types;
	}

	public void setTypes(Short[] types) {
		this.types = types;
	}

	public double[] getQps() {
		return qps;
	}

	public void setQps(double[] qps) {
		this.qps = qps;
	}

	public double[] getCur() {
		return cur;
	}

	public void setCur(double[] cur) {
		this.cur = cur;
	}

	public double[] getTotal() {
		return total;
	}

	public void setTotal(double[] total) {
		this.total = total;
	}

	public void encode(java.io.DataOutput __buffer) throws java.io.IOException {
		MonitorServerStatus0 __obj = this;
		cn.jmicro.api.codec.JDataOutput out = (cn.jmicro.api.codec.JDataOutput) __buffer;
		cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder();
		java.util.Map __val9 = __obj.subsriber2Types;
		if (__val9 == null) {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
		} else {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_PROXY);
			__coder.encode(__buffer, __val9, java.util.Map.class, null);
		}

		java.lang.String __val10 = __obj.instanceName;
		out.writeUTF(__val10 == null ? "" : __val10);

		int __val11 = __obj.subsriberSize;
		out.writeInt(__val11);

		int __val12 = __obj.sendCacheSize;
		out.writeInt(__val12);

		java.lang.Short[] __val13 = __obj.types;
		if (__val13 == null) {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
		} else {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
			__coder.encode(__buffer, __val13, java.lang.Short[].class, null);
		}

		double[] __val14 = __obj.qps;
		if (__val14 == null) {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
		} else {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
			__coder.encode(__buffer, __val14, double[].class, null);
		}

		double[] __val15 = __obj.cur;
		if (__val15 == null) {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
		} else {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
			__coder.encode(__buffer, __val15, double[].class, null);
		}

		double[] __val16 = __obj.total;
		if (__val16 == null) {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
		} else {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
			__coder.encode(__buffer, __val16, double[].class, null);
		}

	}

	public void decode(java.io.DataInput __buffer) throws java.io.IOException {
		MonitorServerStatus0 __obj = this;
		cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder();

		cn.jmicro.api.codec.JDataInput in = (cn.jmicro.api.codec.JDataInput) __buffer;
		java.util.Map __val9;
		if (in.readByte() == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL) {
			__val9 = null;
		} else {
			__val9 = (java.util.Map) __coder.decode(__buffer, java.util.Map.class, null);
		}
		__obj.subsriber2Types = __val9;

		java.lang.String __val10;
		__val10 = __buffer.readUTF();

		__obj.instanceName = __val10;

		int __val11;
		__val11 = in.readInt();

		__obj.subsriberSize = __val11;

		int __val12;
		__val12 = in.readInt();

		__obj.sendCacheSize = __val12;

		java.lang.Short[] __val13;
		if (in.readByte() == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL) {
			__val13 = null;
		} else {
			__val13 = (java.lang.Short[]) __coder.decode(__buffer, java.lang.Short[].class, null);
		}
		__obj.types = __val13;

		double[] __val14;
		if (in.readByte() == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL) {
			__val14 = null;
		} else {
			__val14 = (double[]) __coder.decode(__buffer, double[].class, null);
		}
		__obj.qps = __val14;

		double[] __val15;
		if (in.readByte() == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL) {
			__val15 = null;
		} else {
			__val15 = (double[]) __coder.decode(__buffer, double[].class, null);
		}
		__obj.cur = __val15;

		double[] __val16;
		if (in.readByte() == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL) {
			__val16 = null;
		} else {
			__val16 = (double[]) __coder.decode(__buffer, double[].class, null);
		}
		__obj.total = __val16;

		return;
	}
}
