package cn.expjmicro.example.test;

import java.util.HashSet;

public class TestActInfo {

	private int id;

	private String actName;
	private String loginKey;
	// private int clientId;
	private String pwd;
	private String email;
	private String mobile;

	private String token;
	private byte tokenType;

	private long registTime;

	private byte statuCode;

	private long lastActiveTime;

	// 最后一次登陆时间
	private long lastLoginTime;

	// 登陆次数
	private long loginNum;

	private Boolean isAdmin;

	private boolean guest;

	private HashSet<String> pers = new HashSet<>();

	public TestActInfo() {
	};

	public void decode(java.io.DataInput __buffer) throws java.io.IOException {
		TestActInfo __obj = this;
		cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns()
				.getDefaultCoder();

		cn.jmicro.api.codec.JDataInput in = (cn.jmicro.api.codec.JDataInput) __buffer;
		int __val7;
		__val7 = in.readInt();

		__obj.id = __val7;

		java.lang.String __val8;
		__val8 = __buffer.readUTF();

		__obj.actName = __val8;

		java.lang.String __val9;
		__val9 = __buffer.readUTF();

		__obj.loginKey = __val9;

		java.lang.String __val10;
		__val10 = __buffer.readUTF();

		__obj.pwd = __val10;

		java.lang.String __val11;
		__val11 = __buffer.readUTF();

		__obj.email = __val11;

		java.lang.String __val12;
		__val12 = __buffer.readUTF();

		__obj.mobile = __val12;

		java.lang.String __val13;
		__val13 = __buffer.readUTF();

		__obj.token = __val13;

		byte __val14;
		__val14 = in.readByte();

		__obj.tokenType = __val14;

		long __val15;
		__val15 = in.readLong();

		__obj.registTime = __val15;

		byte __val16;
		__val16 = in.readByte();

		__obj.statuCode = __val16;

		long __val17;
		__val17 = in.readLong();

		__obj.lastActiveTime = __val17;

		long __val18;
		__val18 = in.readLong();

		__obj.lastLoginTime = __val18;

		long __val19;
		__val19 = in.readLong();

		__obj.loginNum = __val19;

		java.lang.Boolean __val20;
		__val20 = in.readBoolean();

		__obj.isAdmin = __val20;

		boolean __val21;
		__val21 = in.readBoolean();

		__obj.guest = __val21;

		java.util.HashSet __val22;
		if (in.readByte() == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL) {
			__val22 = null;
		} else {
			__val22 = (java.util.HashSet) __coder.decode(__buffer, java.util.HashSet.class, null);
		}
		__obj.pers = __val22;

		return;
	}
}
