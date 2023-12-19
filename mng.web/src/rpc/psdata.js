import utils from './utils'
import serial from './serialize.js'
import JDataOutput from './dataoutput.js'
import JDataInput from './datainput.js'

import { Constants } from './message';

let PSConstants = {
    FLAG_DEFALUT: 0,
    FLAG_QUEUE: 1 << 0,
    FLAG_PUBSUB: 0 << 0,
	
    //1右移1位，异步方法，决定回调方法的参数类型为消息通知的返回值
    FLAG_ASYNC_METHOD: 1 << 1,
    //1右移两位，消息回调通知，决定回调方法的参数类型为消息通知的返回值分别为 消息发送状态码，消息ID，消息上下文透传
    FLAG_MESSAGE_CALLBACK: 1 << 2,
    FLAG_PERSIST: 1 << 3,
    FLAG_CALLBACK_TOPIC: 1 << 4,
    FLAG_CALLBACK_METHOD: 0 << 4,
	
	//第5，6两位一起表示data字段的编码类型
	FLAG_DATA_TYPE : 5,
	
	FLAG_DATA_STRING : 0,
	FLAG_DATA_BIN : 1,
	FLAG_DATA_JSON : 2,
	FLAG_DATA_NONE : 3,
   
	RESULT_SUCCCESS: 0,
    PUB_OK: 0,
    //无消息服务可用,需要启动消息服务
    PUB_SERVER_NOT_AVAILABALE: -1,
    //消息队列已经满了,客户端可以重发,或等待一会再重发
    PUB_SERVER_DISCARD: -2,
    //消息服务线程队列已满,客户端可以重发,或等待一会再重发,可以考虑增加消息服务线程池大小,或增加消息服务
    PUB_SERVER_BUSUY: -3,
    PUB_TOPIC_INVALID: -4,
    //消息服务器不可用
    RESULT_FAIL_SERVER_DISABLE: -5,
    //发送给消息订阅者失败
    RESULT_FAIL_DISPATCH: -6,
    //回调结果通知失败
    RESULT_FAIL_CALLBACK: -7,
	
	INVALID_ITEM_COUNT: -8,
	
}

let PSData = function (topic, data) {
	//消息发送结果回调的RPC方法，用于消息服务器给发送者回调
    //消息ID,唯一标识一个消息
   
	this.flag = PSConstants.FLAG_DEFALUT
	this.dataFlag = 0
	this.fr = null;	
	
	this.id = 0
	this.type = 0
    this.topic = topic
    this.srcClientId = -1
	this.to = null;
	this.delay = 0;
    this.data = data
	this.cxt = null
    this.callback = null

}

PSData.Constants = PSConstants;

PSData.prototype.decode = function(r) {
//PSData.prototype.decode = function(in) {
		
	//JDataInput in = (JDataInput)out1;
	
	this.dataFlag = r.readUByte();
	this.flag = r.readUByte();
	this.fr = r.readInt();
	
	if(this.isDataFlag(0)) {
		this.id  = r.readLong();
	}
	
	if(this.isDataFlag(1)) {
		this.type  = r.readByte();
	}
	
	if(this.isDataFlag(2)) {
		this.topic  = r.readUtf8String();
	}
	
	if(this.isDataFlag(3)) {
		this.srcClientId  = r.readInt();
	}
	
	if(this.isDataFlag(4)) {
		this.to  = r.readInt();
	}
	
	if(this.isDataFlag(5)) {
		this.callback  = r.readUtf8String();
	}
	
	/*
	if(this.isDataFlag(6)) {
		this.delay  = r.readByte();
	}
	*/
   
	if(this.isDataFlag(6)) {
		this.cxt  =  this.decodeExtra(r);
	}
	
	if(this.isDataFlag(7)) {
		if(PSConstants.FLAG_DATA_BIN == this.getDataType()) {
			//依赖于接口实现数据编码，服务提供方和使用方需要协商好数据编码和解码方式
			let len = r.readUnsignedShort();
			if(len > 0) {
				this.data = r.readByteArray(len)
			} else {
				this.data = null
			}
			
		}else if(PSConstants.FLAG_DATA_STRING == this.getDataType()){
			this.data = r.readUtf8String();
		}else if(PSConstants.FLAG_DATA_JSON== this.getDataType()){
			let json = r.readUtf8String();
			this.data = JSON.parse(json);
			//out.writeUTF(JsonUtils.getIns().toJson(this.data));
		} else {
			//对几种基本数据类型做解码
			this.data = this.decodeExtra(r);
		}
	}
}

PSData.prototype.decodeExtra = function(b) {
	
	let ed = {};

	let eleNum = b.readUByte(); //extra元素个数
	/*
	if(eleNum < 0) {
		eleNum += 256; //参考encode方法说明
	}
	*/
   
	if(eleNum == 0) return null;
	
	b.readUByte();//discad key type
	
	while(eleNum > 0) {
		let k = b.readUtf8String();
		let v = serial.decodeVal(b);
		v.key = k
		ed[k] = v;
		eleNum--;
	}
	return ed;
}
	
PSData.prototype.encode = function(){
		let b= new JDataOutput(128)
		//let out = new JDataOutput(512)
		//JDataOutput out = (JDataOutput)out1;
		
		if(!!this.id) this.setDataFlag(0);
		if(!!this.type) this.setDataFlag(1);
		if(!!this.topic) this.setDataFlag(2);
		if(!!this.srcClientId) this.setDataFlag(3);
		if(!!this.to) this.setDataFlag(4);
		if(!!this.callback) this.setDataFlag(5);
		//if(this.delay != 0) this.setDataFlag(6);
		if(this.cxt != null) this.setDataFlag(6);
		if(this.data != null) this.setDataFlag(7);
		b.writeUByte(this.dataFlag);
		b.writeUByte(this.flag);
		b.writeInt(this.fr);
		
		if(!!this.id) {
			b.writeLong(this.id);
		}
		
		if(!!this.type) {
			b.writeUByte(this.type);
		}
		
		if(!!this.topic) {
			b.writeUtf8String(this.topic);
		}
		
		if(!!this.srcClientId) {
			b.writeInt(this.srcClientId);
		}
		
		if(!!this.to) {
			b.writeInt(this.to);
		}
		
		if(!!this.callback) {
			b.writeUtf8String(this.callback);
		}
		
		/*
		if(this.delay != 0) {
			b.writeUByte(this.delay);
		}
		*/
	   
		if(this.cxt != null) {
			this.encodeExtra(this.cxt,b);
		}
		
		if(this.data != null) {
			if(PSConstants.FLAG_DATA_BIN == this.getDataType()) {
				//依赖于接口实现数据编码，服务提供方和使用方需要协商好数据编码和解码方式
				if(this.data.encode) {
					this.data.encode(b);
				} else {
					throw "encode error";
				}
			}else if(PSConstants.FLAG_DATA_STRING == this.getDataType()){
				b.writeUtf8String(this.data);
			}else if(PSConstants.FLAG_DATA_JSON== this.getDataType()){
				//非二进制，转为二进制
				let json = JSON.stringify(data);
				bb.writeUtf8String(this.data);
				//this.jsonData()
			} else {
				//对几种基本数据类型做编码
				//Message.encodeVal(b, this.data);
				this.encodeExtra(this.data,b);
			}
		}
		
		return b.getBuf();
}

PSData.prototype.encodeExtra = function (extra,b) {
    let len = 0
	for(let k in extra) {
		len++
	}
	
	b.writeUByte(len)
	if(len == 0) return

	b.writeUByte(1)//String key type
	
	for(let k in extra) {
		b.writeUtf8String(k)
		serial.encodeJSVal(b, extra[k]);
	}
};


PSData.prototype.isPersist = function () {
    return utils.flagIs(this.flag, PSConstants.FLAG_PERSIST)
}

PSData.prototype.setPersist = function (f) {
    this.flag = utils.flagSet(f, this.flag, PSConstants.FLAG_PERSIST)
}

PSData.prototype.queue = function () {
    this.flag = utils.flagSet(true, this.flag, PSConstants.FLAG_QUEUE)
}

PSData.prototype.pubsub = function () {
    this.flag = utils.flagSet(false, this.flag, PSConstants.FLAG_PUBSUB)
}

/*
PSData.prototype.binData = function () {
    this.flag = utils.flagSet(true, this.flag, PSConstants.FLAG_DATA_TYPE_BIN)
}

PSData.prototype.jsonData = function () {
    this.flag = utils.flagSet(false, this.flag, PSConstants.FLAG_DATA_TYPE_JSON)
}
*/

PSData.prototype.callbackTopic = function () {
    this.flag = utils.flagSet(true, this.flag, PSConstants.FLAG_CALLBACK_TOPIC)
}

PSData.prototype.callbackMethod = function () {
    this.flag = utils.flagSet(false, this.flag, PSConstants.FLAG_CALLBACK_METHOD)
}

PSData.prototype.isCallbackTopic = function () {
    return utils.flagIs(this.flag, PSConstants.FLAG_CALLBACK_TOPIC)
}

PSData.prototype.isCallbackMethod = function () {
    return !utils.flagIs(this.flag, PSConstants.FLAG_CALLBACK_TOPIC)
}

PSData.prototype.isQueue = function () {
    return utils.flagIs(this.flag, PSConstants.FLAG_QUEUE)
}

PSData.prototype.isPubsub = function () {
    return !utils.flagIs(this.flag, PSConstants.FLAG_QUEUE)
}

/*
PSData.prototype.isJsonData = function () {
    return utils.flagIs(this.flag, PSConstants.FLAG_DATA_TYPE_JSON)
}

PSData.prototype.isBinData = function () {
    return !utils.flagIs(this.flag, PSConstants.FLAG_DATA_TYPE_BIN)
}
*/

PSData.prototype.setDataFlag = function(idx) {
	this.dataFlag |= 1 << idx;
}

PSData.prototype.isDataFlag = function(idx) {
	return (this.dataFlag & (1 << idx)) != 0;
}
	
PSData.prototype.getDataType = function() {
	return (this.flag >>> PSConstants.FLAG_DATA_TYPE) & 0x03;
}

PSData.prototype.setDataType = function(v) {
	if(v < 0 || v > 6) {
		 new "Invalid data type: "+v;
	}
	this.flag = ((v << PSConstants.FLAG_DATA_TYPE) | this.flag);
}

export default PSData
