package cn.jmicro.example.test.vo;

import cn.jmicro.api.net.Message;

public class ResponseVo {
	
	private long id;
	
	private transient Message msg;
	
	private Long reqId;
	
	private Object result;
	
	private boolean isMonitorEnable = false;
	
	private boolean success = true;
	
	public void encode(java.io.DataOutput __buffer,Object obj) throws java.io.IOException { 
		ResponseVo __obj =  this;
		  cn.jmicro.api.codec.JDataOutput out = (cn.jmicro.api.codec.JDataOutput)__buffer;
		 cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder(); 
		 long __val0= __obj.id;
		 out.writeLong( __val0); 


		 java.lang.Long __val2= __obj.reqId;
		 out.writeLong( __val2); 


		 java.lang.Object __val3= __obj.result;
		 byte flag3 = 0; 
		 int flagIndex3 = out.position(); 
		 __buffer.writeByte(0); // forward one byte  
		if(__val3 == null)  { flag3 |= cn.jmicro.common.Constants.NULL_VAL; 
		 out.write(flagIndex3,flag3);
		}   else { //block0 
		  __coder.encode(__buffer,__val3,java.lang.Object.class, null );

		 out.write(flagIndex3,flag3);
		 } //end else block0 


		 boolean __val4= __obj.isMonitorEnable;
		 out.writeBoolean( __val4); 


		 boolean __val5= __obj.success;
		 out.writeBoolean( __val5); 


		}
		 public void decode(java.io.DataInput __buffer)  throws java.io.IOException {
			 ResponseVo __obj =  this;
		  cn.jmicro.api.codec.JDataInput in = (cn.jmicro.api.codec.JDataInput)__buffer;
		long __val0; 
		 __val0 = in.readLong();
		__obj.id =  __val0;

		java.lang.Long __val2; 
		 __val2 = new java.lang.Long(in.readLong());
		__obj.reqId =  __val2;

		java.lang.Object __val3; 
		 byte flagName3 = __buffer.readByte(); 
		 __val3  = null;
		  if(0 != (cn.jmicro.common.Constants.NULL_VAL & flagName3)) {  __val3  = null;
		 } else { // block0 
		 cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder();

		 __val3 = (java.lang.Object) __coder.decode(__buffer,java.lang.Object.class, null );
		} //block0 
		__obj.result =  __val3;

		boolean __val4; 
		 __val4 = in.readBoolean();
		__obj.isMonitorEnable =  __val4;

		boolean __val5; 
		 __val5 = in.readBoolean();
		__obj.success =  __val5;

		 return;
		 }
		public long getId() {
			return id;
		}
		public void setId(long id) {
			this.id = id;
		}
		public Message getMsg() {
			return msg;
		}
		public void setMsg(Message msg) {
			this.msg = msg;
		}
		public Long getReqId() {
			return reqId;
		}
		public void setReqId(Long reqId) {
			this.reqId = reqId;
		}
		public Object getResult() {
			return result;
		}
		public void setResult(Object result) {
			this.result = result;
		}
		public boolean isMonitorEnable() {
			return isMonitorEnable;
		}
		public void setMonitorEnable(boolean isMonitorEnable) {
			this.isMonitorEnable = isMonitorEnable;
		}
		public boolean isSuccess() {
			return success;
		}
		public void setSuccess(boolean success) {
			this.success = success;
		}
		 
		 
}
