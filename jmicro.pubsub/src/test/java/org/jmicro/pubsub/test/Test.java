package org.jmicro.pubsub.test;

public class Test {
	
	public void encode(java.io.DataOutput __buffer,Object obj) { 
		org.jmicro.example.test.SerializeObject __obj =  this;
		  java.util.Date __val0= __obj.date;
		__val0 == null ? __buffer.writeLong(0L) :  __buffer.writeLong( __val0.getTime()); 


		 java.lang.String __val1= __obj.val1111;
		 __buffer.writeUTF( __val1); 


		 byte __val2= __obj.bvssssssssssss;
		 __buffer.writeByte( __val2); 


		 int __val3= __obj.iv222222222222222;
		 __buffer.writeInt( __val3); 


		 short __val4= __obj.sv33333333333333;
		 __buffer.writeShort( __val4); 


		 long __val5= __obj.lvdddddddddddddd;
		 __buffer.writeLong( __val5); 


		 float __val6= __obj.fv222222222222222222222222;
		 __buffer.writeFloat( __val6); 


		 double __val7= __obj.dv22234324;
		 __buffer.writeDouble( __val7); 


		 char __val8= __obj.cvfdksafjdlaj;
		 __buffer.writeChar( __val8); 


		 java.util.Set __val9= __obj.setv;
		 org.jmicro.api.codec.JDataOutput out = (org.jmicro.api.codec.JDataOutput)__buffer;
		 byte flag9 = 0; 
		 int flagIndex9 = out.position(); 
		 __buffer.writeByte(0); // forward one byte  
		if(__val9 == null)  { flag9 = org.jmicro.agent.SerializeProxyFactory.setNull(flag9); 
		 out.write(flagIndex9,flag9);
		}   else { //block0 
		  org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder(); 
		 Short c = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val9.getClass()); 
		 if(c == null) { flag9 = org.jmicro.agent.SerializeProxyFactory.setType(flag9); 
		 __buffer.writeUTF(__val9.getClass().getName());
		 } 
		 else { 
		 __buffer.writeShort(c.intValue());} 
		 int size = __val9.size(); 
		 __buffer.writeShort(size); 
		 if(size > 0) { //if block1 
		 boolean writeEvery = false;
		if(org.jmicro.api.test.Person.class!= null && (java.lang.reflect.Modifier.isFinal(org.jmicro.api.test.Person.class.getModifiers()) ||org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(org.jmicro.api.test.Person.class))) { 
		 flag9 = org.jmicro.agent.SerializeProxyFactory.setGenericTypeFinal(flag9); 
		 } else { // block2 
		 boolean sameElt = org.jmicro.agent.SerializeProxyFactory.sameCollectionTypeEles(__val9); 
		 boolean isFinal = org.jmicro.agent.SerializeProxyFactory.seriaFinalClass(__val9.iterator().next().getClass());
		 if(sameElt && isFinal) { //block3 
		 flag9 = org.jmicro.agent.SerializeProxyFactory.setHeaderELetment(flag9); 
		 writeEvery = false; 
		 Short c9 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val9.iterator().next().getClass());
		 if(c == null) { flag9 = org.jmicro.agent.SerializeProxyFactory.setElementTypeCode(flag9); 
		  __buffer.writeUTF(__val9.iterator().next().getClass().getName());  } // 
		 else { 
		 __buffer.writeShort(c9.intValue());
		 }  } //block3 
		 else { //block4 
		 writeEvery = true;
		 } // block4 
		 } // block2 
		 java.util.Iterator ite = __val9.iterator();
		 while(ite.hasNext()) { //loop block5 
		 Object v = ite.next(); 
		 if(writeEvery) { 
		 Short cc9 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(v.getClass()); 
		 __buffer.writeShort(cc9.intValue());
		}
		 org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer,v); 
		 } //end for loop block5 
		  System.out.println("Encoder Flag: "+flagName); 
		 out.write(flagIndex9,flag9);
		 } // end if block1 
		  } //end else block0 


		 java.util.List __val10= __obj.listv;
		 org.jmicro.api.codec.JDataOutput out = (org.jmicro.api.codec.JDataOutput)__buffer;
		 byte flag10 = 0; 
		 int flagIndex10 = out.position(); 
		 __buffer.writeByte(0); // forward one byte  
		if(__val10 == null)  { flag10 = org.jmicro.agent.SerializeProxyFactory.setNull(flag10); 
		 out.write(flagIndex10,flag10);
		}   else { //block0 
		  org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder(); 
		 Short c = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val10.getClass()); 
		 if(c == null) { flag10 = org.jmicro.agent.SerializeProxyFactory.setType(flag10); 
		 __buffer.writeUTF(__val10.getClass().getName());
		 } 
		 else { 
		 __buffer.writeShort(c.intValue());} 
		 int size = __val10.size(); 
		 __buffer.writeShort(size); 
		 if(size > 0) { //if block1 
		 boolean writeEvery = false;
		if(org.jmicro.api.test.Person.class!= null && (java.lang.reflect.Modifier.isFinal(org.jmicro.api.test.Person.class.getModifiers()) ||org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(org.jmicro.api.test.Person.class))) { 
		 flag10 = org.jmicro.agent.SerializeProxyFactory.setGenericTypeFinal(flag10); 
		 } else { // block2 
		 boolean sameElt = org.jmicro.agent.SerializeProxyFactory.sameCollectionTypeEles(__val10); 
		 boolean isFinal = org.jmicro.agent.SerializeProxyFactory.seriaFinalClass(__val10.iterator().next().getClass());
		 if(sameElt && isFinal) { //block3 
		 flag10 = org.jmicro.agent.SerializeProxyFactory.setHeaderELetment(flag10); 
		 writeEvery = false; 
		 Short c10 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val10.iterator().next().getClass());
		 if(c == null) { flag10 = org.jmicro.agent.SerializeProxyFactory.setElementTypeCode(flag10); 
		  __buffer.writeUTF(__val10.iterator().next().getClass().getName());  } // 
		 else { 
		 __buffer.writeShort(c10.intValue());
		 }  } //block3 
		 else { //block4 
		 writeEvery = true;
		 } // block4 
		 } // block2 
		 java.util.Iterator ite = __val10.iterator();
		 while(ite.hasNext()) { //loop block5 
		 Object v = ite.next(); 
		 if(writeEvery) { 
		 Short cc10 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(v.getClass()); 
		 __buffer.writeShort(cc10.intValue());
		}
		 org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer,v); 
		 } //end for loop block5 
		  System.out.println("Encoder Flag: "+flagName); 
		 out.write(flagIndex10,flag10);
		 } // end if block1 
		  } //end else block0 


		 org.jmicro.api.test.Person __val11= __obj.p;
		 org.jmicro.api.codec.JDataOutput out = (org.jmicro.api.codec.JDataOutput)__buffer;
		 byte flag11 = 0; 
		 int flagIndex11 = out.position(); 
		 __buffer.writeByte(0); // forward one byte  
		if(__val11 == null)  { flag11 = org.jmicro.agent.SerializeProxyFactory.setNull(flag11); 
		 out.write(flagIndex11,flag11);
		}   else { //block0 
		  ((org.jmicro.api.codec.ISerializeObject)__val11).encode(__buffer,null);
		 out.write(flagIndex11,flag11);
		 } //end else block0 


		}
}
