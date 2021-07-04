import JDataInput from "@/rpc/datainput";
import utils from "@/rpc/utils";
import security from "@/rpc/security";
import config from "@/rpc/config";
import JDataOutput from "@/rpc/dataoutput";

let PREFIX_TYPE_ID = -128;

let Constants = {

    //MSG_TYPE_REQ_RAW : 0x03,  //纯二进制数据请求
    //MSG_TYPE_RRESP_RAW : 0x04, //纯二进制数据响应

    MSG_TYPE_REQ_JRPC : 0x01, //普通RPC调用请求，发送端发IRequest，返回端返回IResponse
    MSG_TYPE_RRESP_JRPC : 0x02,//返回端返回IResponse

    MSG_TYPE_ASYNC_RESP : 0x06,

    FNV_HASH_METHOD_KEY: "cn.jmicro.api.gateway.IBaseGatewayService##apigateway##0.0.1########fnvHash1a",

    SERVICE_NAMES : "serviceNames",
    SERVICE_NAMESPACES : "namespaces",
    SERVICE_VERSIONS : "versions",
    SERVICE_METHODS : "methods",
    INSTANCES : "instances",
    NAMED_TYPES : "nameTypes",
    ALL_INSTANCES : "allInstances",
    MONITOR_RESOURCE_NAMES : "resourceNames",

    LOG_NO : 0,
    LOG_FINAL : 6,

    CHARSET : 'UTF-8',

    LOGIN:1,
    LOGOUT:2,

    //public static final int HEADER_LEN = 13; // 2(flag)+2(data len with short)+1(type)
    HEADER_LEN : 13,
    //public static final int EXT_HEADER_LEN = 2; //附加数据头部长度
    EXT_HEADER_LEN : 2,
    //public static final int SEC_LEN = 128;

    //public static final byte PROTOCOL_BIN = 0;
    PROTOCOL_BIN : 0,

    //public static final byte PROTOCOL_JSON = 1;
    PROTOCOL_JSON : 1,

    //public static final byte PRIORITY_0 = 0;
    PRIORITY_0 : 0,
    //public static final byte PRIORITY_1 = 1;
    PRIORITY_1 : 1,
    //public static final byte PRIORITY_2 = 2;
    PRIORITY_2 : 2,
    //public static final byte PRIORITY_3 = 3;
    PRIORITY_3 : 3,
    //public static final byte PRIORITY_4 = 4;
    PRIORITY_4 : 4,
    //public static final byte PRIORITY_5 = 5;
    PRIORITY_5 : 5,
    //public static final byte PRIORITY_6 = 6;
    PRIORITY_6 : 6,
    //public static final byte PRIORITY_7 = 7;
    PRIORITY_7 : 7,

    //public static final byte PRIORITY_MIN = PRIORITY_0;
    PRIORITY_MIN : 0,
    //public static final byte PRIORITY_NORMAL = PRIORITY_3;
    PRIORITY_NORMAL : 3,
    //public static final byte PRIORITY_MAX = PRIORITY_7;
    PRIORITY_MAX : 7,

    //public static final int MAX_SHORT_VALUE = ((int)Short.MAX_VALUE)*2;
    MAX_SHORT_VALUE : 32767,

    //public static final short MAX_BYTE_VALUE = ((short)Byte.MAX_VALUE)*2;
    MAX_BYTE_VALUE : 126,

    //public static final long MAX_INT_VALUE = ((long)Integer.MAX_VALUE)*2;
    MAX_INT_VALUE :  0x7FFFFFFF,//10M 0x7FFFFFFF,

    //public static final long MAX_LONG_VALUE = Long.MAX_VALUE*2;

    //public static final byte MSG_VERSION = (byte)1;

    //长度字段类型，1表示整数，0表示短整数
    //public static final short FLAG_LENGTH_INT = 1 << 0;
    FLAG_LENGTH_INT : 1 << 0,

    //public static final short FLAG_UP_PROTOCOL = 1<<1;
    FLAG_UP_PROTOCOL : 1 << 1,
    //public static final short FLAG_DOWN_PROTOCOL = 1 << 2;
    FLAG_DOWN_PROTOCOL : 1 << 2,

    //可监控消息
    //public static final short FLAG_MONITORABLE = 1 << 3;
    FLAG_MONITORABLE : 1 << 3,

    //包含Extra数据
    //public static final short FLAG_EXTRA = 1 << 4;
    FLAG_EXTRA : 1 << 4,

    //需要响应的请求  or down message is error
    //public static final short FLAG_NEED_RESPONSE = 1 << 5;

    //来自外网消息，由网关转发到内网
//public static final short FLAG_OUT_MESSAGE = 1 << 5;
    FLAG_OUT_MESSAGE : 1 << 5,

    //public static final short FLAG_ERROR = 1 << 6;
    FLAG_ERROR : 1 << 6,

    FLAG_FORCE_RESP_JSON : 1 << 7,

    //public static final short FLAG_LOG_LEVEL = 13;
    FLAG_LOG_LEVEL :  13,

    //public static final short FLAG_RESP_TYPE = 11;
    FLAG_RESP_TYPE : 11,

    /****************  extra constants flag   *********************/

    //调试模式
    //public static final int EXTRA_FLAG_DEBUG_MODE = 1 << 0;
    EXTRA_FLAG_DEBUG_MODE : 1 << 0,

    //public static final int EXTRA_FLAG_PRIORITY = 1;
    EXTRA_FLAG_PRIORITY : 1,

    //DUMP上行数据
    //public static final int EXTRA_FLAG_DUMP_UP = 1 << 3;
    EXTRA_FLAG_DUMP_UP : 1 << 3,

    //DUMP下行数据
    //public static final int EXTRA_FLAG_DUMP_DOWN = 1 << 4;
    EXTRA_FLAG_DUMP_DOWN : 1 << 4,

    //加密参数 0：没加密，1：加密
    //public static final int EXTRA_FLAG_UP_SSL = 1 << 5;
    EXTRA_FLAG_UP_SSL : 1 << 5,

    //是否签名
    //public static final int EXTRA_FLAG_DOWN_SSL = 1 << 6;
    EXTRA_FLAG_DOWN_SSL : 1 << 6,

    //public static final int EXTRA_FLAG_IS_FROM_WEB = 1 << 7;
    EXTRA_FLAG_IS_FROM_WEB : 1 << 7,

    //public static final int EXTRA_FLAG_IS_SEC = 1 << 8;
    EXTRA_FLAG_IS_SEC : 1 << 8,

    //是否签名： 0:无签名； 1：有签名
    //public static final int EXTRA_FLAG_IS_SIGN = 1 << 9;
    EXTRA_FLAG_IS_SIGN : 1 << 9,

    //加密方式： 0:对称加密，1：RSA 非对称加密
    //public static final int EXTRA_FLAG_ENC_TYPE = 1 << 10;
    EXTRA_FLAG_ENC_TYPE : 1 << 10,

    //public static final int EXTRA_FLAG_RPC_MCODE = 1 << 11;
    EXTRA_FLAG_RPC_MCODE : 1 << 11,

    //public static final int EXTRA_FLAG_SECTET_VERSION = 1 << 12;
    EXTRA_FLAG_SECTET_VERSION : 1 << 12,

    //是否包含实例ID
    //public static final int EXTRA_FLAG_INS_ID = 1 << 12;
    EXTRA_FLAG_INS_ID : 1 << 13,

    //public static final Byte EXTRA_KEY_LINKID = -127;
    EXTRA_KEY_LINKID : -127,
    //public static final Byte EXTRA_KEY_INSID = -126;
    EXTRA_KEY_INSID :-126,
    //public static final Byte EXTRA_KEY_TIME = -125;
    EXTRA_KEY_TIME : -125,
    //public static final Byte EXTRA_KEY_SM_CODE = -124;
    EXTRA_KEY_SM_CODE : -124,
    //public static final Byte EXTRA_KEY_SM_NAME = -123;
    EXTRA_KEY_SM_NAME : -123,
    //public static final Byte EXTRA_KEY_SIGN = -122;
    EXTRA_KEY_SIGN : -122,

    //public static final Byte EXTRA_KEY_SALT = -121;
    EXTRA_KEY_SALT : -121,

    //public static final Byte EXTRA_KEY_SEC = -120;
    EXTRA_KEY_SEC : -120,

    //public static final Byte EXTRA_KEY_LOGIN_KEY = -119;
    EXTRA_KEY_LOGIN_KEY : -119,

    //public static final Byte EXTRA_KEY_ARRAY = -116;
    //public static final Byte EXTRA_KEY_FLAG = -118;
    EXTRA_KEY_FLAG : -118,

    //public static final Byte EXTRA_KEY_MSG_ID = -117;
    EXTRA_KEY_MSG_ID : -117,

    //RPC METHOD NAME
     EXTRA_KEY_METHOD : 127,

     EXTRA_KEY_EXT0 : 126,
     EXTRA_KEY_EXT1 : 125,
     EXTRA_KEY_EXT2 :124,
     EXTRA_KEY_EXT3 : 123,

    //public static final byte MSG_TYPE_PINGPONG = 0;//默认请求响应模式
    MSG_TYPE_PINGPONG : 0,

    //public static final byte MSG_TYPE_NO_RESP = 1;//单向模式
    MSG_TYPE_NO_RESP : 1,

    //public static final byte MSG_TYPE_MANY_RESP = 2;//多个响应模式，如消息订阅
    MSG_TYPE_MANY_RESP : 2,

    /****************  extra constants flag   *********************/

    //PREFIX_TYPE_ID : -128,
    //空值编码
    PREFIX_TYPE_NULL : PREFIX_TYPE_ID++,

    //FINAL
    PREFIX_TYPE_FINAL : PREFIX_TYPE_ID++,

    //类型编码写入编码中
    PREFIX_TYPE_SHORT : PREFIX_TYPE_ID++,
    //全限定类名作为前缀串写入编码中
    PREFIX_TYPE_STRING : PREFIX_TYPE_ID++,

    //以下对高使用频率非final类做快捷编码

    //列表类型编码，指示接下业读取一个列表，取列表编码器直接解码
    PREFIX_TYPE_LIST : PREFIX_TYPE_ID++,
    //集合类型编码，指示接下来读取一个集合，取SET编码器直接解码
    PREFIX_TYPE_SET : PREFIX_TYPE_ID++,
    //Map类型编码，指示接下来读取一个Map，取Map编码器直接解码
    PREFIX_TYPE_MAP : PREFIX_TYPE_ID++,

    PREFIX_TYPE_BYTE : PREFIX_TYPE_ID++,
    PREFIX_TYPE_SHORTT : PREFIX_TYPE_ID++,
    PREFIX_TYPE_INT : PREFIX_TYPE_ID++,
    PREFIX_TYPE_LONG : PREFIX_TYPE_ID++,
    PREFIX_TYPE_FLOAT :PREFIX_TYPE_ID++,
    PREFIX_TYPE_DOUBLE : PREFIX_TYPE_ID++,
    PREFIX_TYPE_CHAR : PREFIX_TYPE_ID++,
    PREFIX_TYPE_BOOLEAN : PREFIX_TYPE_ID++,
    PREFIX_TYPE_STRINGG :PREFIX_TYPE_ID++,
    PREFIX_TYPE_DATE : PREFIX_TYPE_ID++,
    PREFIX_TYPE_BYTEBUFFER : PREFIX_TYPE_ID++,
    PREFIX_TYPE_REQUEST : PREFIX_TYPE_ID++,
    PREFIX_TYPE_RESPONSE : PREFIX_TYPE_ID++,
    PREFIX_TYPE_PROXY : PREFIX_TYPE_ID++,

}


 function Message () {

    //0B00111000 5---3
    //public static final short FLAG_LEVEL = 0X38;

    //是否启用服务级log
    //public static final short FLAG_LOGGABLE = 1 << 3;
    //private transient long startTime = -1;

    //此消息所占字节数，用于记录流量
    //private transient int len = -1;

    //1 byte length
    //private byte version;

    //payload length with byte,4 byte length
    //private int len;

    /**
     * 0        S:       data length type 0:short 1:int
     * 1        UPR:     up protocol  0: bin,  1: json
     * 2        DPR:     down protocol 0: bin, 1: json
     * 3        M:       Monitorable
     * 4        Extra    Contain extradata
     * 5        Innet    message from outer network
     * 6
     * 7
     * 8
     * 9
     * 10
     * 11，12   Resp type  MSG_TYPE_PINGPONG，MSG_TYPE_NO_RESP，MSG_TYPE_MANY_RESP
     * 13 14 15 LLL      Log level
     * @return
     */
    //private short flag = 0;
    this.flag = 0;

    // 1 byte
    //private byte type;
    this.type=0;

    //2 byte length
    //private byte ext;

    //normal message ID	or JRPC request ID
    //private long msgId;
    this.msgId = 0;

    //private Object payload;
    this.payload = null;


    //private Map<Byte,Object> extraMap;
    this.extraMap = [];
    //*****************extra data begin******************//

    /**
     * 0        dm:       is development mode EXTRA_FLAG_DEBUG_MODE = 1 << 1;
     * 1,2      PP:       Message priority   EXTRA_FLAG_PRIORITY
     * 3        up:       dump up stream data
     * 4        do:       dump down stream data
     * 5 	    US        上行SSL  0:no encrypt 1:encrypt
     * 6        DS        下行SSL  0:no encrypt 1:encrypt
     * 7        SV        对称密钥版本
     * 8        MK        RPC方法编码
     * 9        WE        从Web浏览器过来的请求
     * 10       SE        密码
     * 11       SI        是否有签名值 0：无，1：有
     * 12       ENT       encrypt type 0:对称加密，1：RSA 非对称加密
     * 13
     ENT SI  SE  WE MK SV  DS   US   DO   UP  P    P   dm
     |    |   |   |  |   |   |  |  |   |    |    |    |   |    |   |
     15  14  13  12  11  10  9  8  7   6    5    4    3   2    1   0

     |    |   |   |  |   |   |    |   |   |    |    |    |   |    |   |
     31  30  29  28  27  26  25   24  23  22   21   20   19  18   17  16

     *
     * @return
     */
    //private transient int extrFlag = 0;
    this.extrFlag = 0;
    //附加数居
    //private transient ByteBuffer extra = null;
    this.extra = null;
}

Message.prototype.decodeExtra=function(/*ByteBuffer*/ extra) {
    if(extra == null || extra.length == 0) return null;

    let ed = [];

    let b = new JDataInput(extra);

    while(b.remaining() > 0) {
        let v = this.decodeVal(b);
        ed.push(v);
    }
    return ed;

}

Message.prototype.decodeVal=function(/*JDataInput*/ b)  {
    let k = b.getByte();
    let type = b.getByte();
    if(Constants.PREFIX_TYPE_LIST == type){
        let len = b.readUnsignedShort();
        if(len == 0) {
            return new [];
        }
        let arr = b.readByteArray(len);
        return {v:arr, type:Constants.PREFIX_TYPE_LIST, key:k};
    }else if(type == Constants.PREFIX_TYPE_INT){
        let v = b.readInt();
        return {v:v, type:Constants.PREFIX_TYPE_INT, key:k};
    }else if(Constants.PREFIX_TYPE_BYTE == type){
        let v = b.getByte();
        return {v:v, type:Constants.PREFIX_TYPE_BYTE, key:k};
    }else if(Constants.PREFIX_TYPE_SHORTT == type){
        let v = b.readUnsignedShort();
        return {v:v, type:Constants.PREFIX_TYPE_SHORTT, key:k};
    }else if(Constants.PREFIX_TYPE_LONG == type){
        let v = b.readUnsignedLong();
        return {v:v, type:Constants.PREFIX_TYPE_LONG, key:k};
    }else if(Constants.PREFIX_TYPE_FLOAT == type){
        let v =  b.readFloat();
        return {v:v, type:Constants.PREFIX_TYPE_FLOAT, key:k};
    }else if(Constants.PREFIX_TYPE_DOUBLE == type){
        let v = b.readDouble();
        return {v:v, type:Constants.PREFIX_TYPE_DOUBLE, key:k};
    }else if(Constants.PREFIX_TYPE_BOOLEAN == type){
        let v = b.readBoolean();
        return {v:v, type:Constants.PREFIX_TYPE_BOOLEAN, key:k};
    }else if(Constants.PREFIX_TYPE_CHAR == type){
        let v = b.readChar();
        return {v:v, type:Constants.PREFIX_TYPE_CHAR, key:k};
    }else if(Constants.PREFIX_TYPE_STRING == type){
        let str = b.readUtf8String();
        return {v:str, type:Constants.PREFIX_TYPE_STRING, key:k};
    }else if(Constants.PREFIX_TYPE_NULL == type){
        return {v:null, type:Constants.PREFIX_TYPE_NULL, key:k};
    } else {
        throw "not support header type: " + type;
    }
}

Message.prototype.encodeExtra=function(/*Map<Byte, Object> */extras) {
    if(extras == null || extras.length == 0) return null;

    let b =  new JDataOutput(128);

    //JDataOutput b = new JDataOutput(64);
    for(let i = 0; i < extras.length; i++) {
        let e = extras[i];
        b.writeUByte(e.key);
        this.encodeVal(b,e);
    }
    return b.getBuf();
}

Message.prototype.encodeVal=function(/*JDataOutput*/ b, /*Object*/ v)  {
    if(v == null) {
        b.writeUByte(Constants.PREFIX_TYPE_NULL);
        return;
    }

    if(v.type == Constants.PREFIX_TYPE_LIST){
        b.writeUByte(Constants.PREFIX_TYPE_LIST);
        let arr = v.v;
        b.writeByteArrayWithShortLen(arr);
    }else if(v.type == Constants.PREFIX_TYPE_STRING) {
        b.writeUByte(Constants.PREFIX_TYPE_STRING);
        b.writeUtf8String(v.v);
    }else if(v.type == Constants.PREFIX_TYPE_INT){
        b.writeUByte(Constants.PREFIX_TYPE_INT);
        b.writeInt(v.v);
    }else if(v.type == Constants.PREFIX_TYPE_BYTE){
        b.writeUByte(Constants.PREFIX_TYPE_BYTE);
        b.writeUByte(v.v);
    }else if(v.type == Constants.PREFIX_TYPE_SHORTT){
        b.writeUByte(Constants.PREFIX_TYPE_SHORTT);
        b.writeUnsignedShort(v.v);
    }else if(v.type == Constants.PREFIX_TYPE_LONG){
        b.writeUByte(Constants.PREFIX_TYPE_LONG);
        b.writeUnsignedLong(v.v);
    }else if(v.type == Constants.PREFIX_TYPE_FLOAT){
        b.writeUByte(Constants.PREFIX_TYPE_FLOAT);
        b.writeFloat(v.v);
    }else if(v.type == Constants.PREFIX_TYPE_DOUBLE){
        b.writeUByte(Constants.PREFIX_TYPE_DOUBLE);
        b.writeDouble(v.v);
    }else if(v.type == Constants.PREFIX_TYPE_BOOLEAN){
        b.writeUByte(Constants.PREFIX_TYPE_BOOLEAN);
        b.writeBoolean(v.v);
    }else if(v.type == Constants.PREFIX_TYPE_CHAR){
        b.writeUByte(Constants.PREFIX_TYPE_CHAR);
        b.writeChar(v.v);
    } else {
        throw "not support header type for val: " + v+", with type: "+v.type;
    }
}

Message.prototype.decode=function(/*JDataInput*/ arr) {

    //Message msg = new Message();
    let msg = this;
    let b = new JDataInput(arr);

    //第0,1个字节
    let flag = b.readUnsignedShort();

    msg.flag =  flag;
    //ByteBuffer b = ByteBuffer.wrap(data);
    let len = 0;
    if(msg.isLengthInt()) {
        len = b.readInt();
    } else {
        len = b.readUnsignedShort(); // len = 数据长度 + 附加数据长度
    }

    if(b.remaining() < len){
        throw "Message len not valid";
    }

    //第3个字节
    //msg.setVersion(b.readByte());

    //read type
    //第4个字节
    msg.type = b.getUByte();
    msg.setMsgId(b.readUnsignedLong());

    if(msg.isReadExtra()) {
        let elen = b.readUnsignedShort();
        if(elen < 1) throw 'invalid array length: ' + elen;
        let edata = b.readByteArray(elen);
        //b.readFully(edata,0,elen);

        msg.extra = edata;//ByteBuffer.wrap(edata);
        len = len - Constants.EXT_HEADER_LEN - elen;
        msg.extraMap = this.decodeExtra(msg.extra);
        //msg.setLen(len + elen);
        if(msg.containExtra(Constants.EXTRA_KEY_FLAG)/*msg.extraMap(Constants.EXTRA_KEY_FLAG)*/) {
            msg.extrFlag = msg.getExtra(Constants.EXTRA_KEY_FLAG);// msg.extraMap.get(Constants.EXTRA_KEY_FLAG);
        }
    }/* else {
            msg.setLen(len);
        }*/

    if(len > 0){
        msg.payload = b.readByteArray(len);

        if(msg.isDownSsl()) {
            security.checkAndDecrypt(msg);
        }

        if(this.getDownProtocol() == Constants.PROTOCOL_JSON) {
            let json = utils.fromUTF8Array(msg.payload);
            //console.log(json);
            msg.payload = JSON.parse(json);
        }
    } else {
        msg.payload = null;
    }
    msg.len = len + Constants.HEADER_LEN;
}

Message.prototype.encode=function() {

    let b =  new JDataOutput(1024);
    let len = 0;//数据长度 + 测试模式时附加数据长度

    let data = this.payload;

    if(!(data instanceof ArrayBuffer || data instanceof Array)) {
        let json = JSON.stringify(data);
        data = utils.toUTF8Array(json);
        this.setUpProtocol(Constants.PROTOCOL_JSON)
    }else if (data instanceof ArrayBuffer) {
        let arrData = [];
        let buf = new DataView(data,0, data.byteLength) ;
        for(let i = 0; i < data.byteLength; i++) {
            arrData.push(buf.getUint8(i));
        }
        data = arrData;
    }

    this.payload = data;

    if(config.sslEnable) {
        this.setUpSsl(true);
        this.setEncType(false);
        this.setDownSsl(true);
        security.encrypt(this);
    }

    if (this.payload instanceof ArrayBuffer) {
        len = this.payload.byteLength;
    }else {
        len = this.payload.length;
    }

    //data.mark();

    if(this.extrFlag != 0) {
        this.putExtra(Constants.EXTRA_KEY_FLAG, this.extrFlag,Constants.PREFIX_TYPE_INT);
    }

    if(this.isWriteExtra()) {
        this.extra = this.encodeExtra(this.extraMap);
        if( this.extra== null || this.extra.length > 4092) {
            throw "Too long extra: " + this.extraMap.toString();
        }
        len += this.extra.byteLength + Constants.EXT_HEADER_LEN;
        this.setExtra(true);
    }

    //第1，2个字节 ,len = 数据长度 + 测试模式时附加数据长度
    if(len < Constants.MAX_SHORT_VALUE) {
        this.setLengthType(false);
    } else if(len < Constants.MAX_INT_VALUE){
        this.setLengthType(true);
    } else {
        throw "Data length too long than :" + Constants.MAX_INT_VALUE + ", but value "+len;
    }

    //第0,1,2,3个字节，标志头
    //b.put(this.flag);
    b.writeUnsignedShort(this.flag);

    if(len < 32767) {
        //第2，3个字节 ,len = 数据长度 + 测试模式时附加数据长度
        b.writeUnsignedShort(len);
    }else if(len < Constants.MAX_VALUE){
        //消息内内容最大长度为MAX_VALUE 2,3,4,5
        b.writeInt(len);
    } else {
        throw "Max int value is :"+ Constants.MAX_VALUE+", but value "+len;
    }

    //b.putShort((short)0);

    //第3个字节
    //b.put(this.version);
    //b.writeByte(this.method);

    //第4个字节
    //writeUnsignedShort(b, this.type);
    //b.put(this.type);
    b.writeUByte(this.type);

    b.writeUnsignedLong(this.msgId);

    if(this.isWriteExtra()) {
        //b.writeInt(this.extrFlag);
        b.writeUnsignedShort(this.extra.byteLength);
        this.writeArray(b,this.extra);
    }

    if(data != null){
        //b.put(data);
        // b.write(data);
        //data.reset();
        this.writeArray(b,this.payload);
        //b.writeByteArrayWithShortLen(this.data);
    }

    return b.getBuf();
}

Message.prototype.writeArray = function(buf,data) {
    if(data instanceof ArrayBuffer) {
        let size = data.byteLength;
        let dv = new DataView(data);
        buf.checkCapacity(size);
        for(let i = 0; i < size; i++) {
            buf.writeUByte(dv.getUint8(i))
        }
    } else {
        let size = data.length;
        buf.checkCapacity(size);
        for(let i = 0; i < size; i++) {
            buf.writeUByte(data[i])
        }
    }
}

//public static boolean
Message.prototype.is = function( flag,  mask) {
    return (flag & mask) != 0;
}

Message.prototype.setMsgId = function(msgId) {
    this.msgId = msgId;
}

Message.prototype.set = function( isTrue, f, mask) {
    return isTrue ?(f |= mask):(f &= ~mask);
}

Message.prototype.containExtra=function(key) {
    let pm = this.extraMap;
    if(!pm || pm.length == 0) return false;
    for(let i = 0; i < pm.length; i++) {
        if(pm[i].key == key) return true;
    }
    return false;
}

Message.prototype.getExtra = function(key) {
    let pm = this.extraMap;
    if(!pm || pm.length == 0) return null;
    for(let i = 0; i < pm.length; i++) {
        if(pm[i].key == key) return pm[i].v;
    }
    return null;
}

Message.prototype.putExtra = function(key, val,type) {
    let pm = this.extraMap;
    let e = {};
    let f = false;
    if(pm) {
        for(let i = 0; i < pm.length; i++) {
            if(pm[i].key == key) {
                e = pm[i];
                f = true;
                break;
            }
        }
    }
    e.key = key;
    e.type = type;
    e.v = val;

    if(!f) {
        pm.push(e);
    }
}

Message.prototype.isWriteExtra=function() {
    return this.extraMap != null && this.extraMap.length > 0 /*&& !this.extraMap.isEmpty()*/;
}

Message.prototype.isReadExtra=function() {
    return this.is(this.flag,Constants.FLAG_EXTRA);
}

Message.prototype.setExtra=function(/*boolean*/ f) {
    this.flag = this.set(f,this.flag,Constants.FLAG_EXTRA);
}

Message.prototype.isUpSsl=function() {
    return this.is(this.extrFlag,Constants.EXTRA_FLAG_UP_SSL);
}

Message.prototype.setUpSsl=function(/*boolean*/ f) {
    this.extrFlag = this.set(f,this.extrFlag,Constants.EXTRA_FLAG_UP_SSL);
}

Message.prototype.isDownSsl=function() {
    return this.is(this.extrFlag,Constants.EXTRA_FLAG_DOWN_SSL);
}

Message.prototype.setDownSsl=function(/*boolean*/ f) {
    this.extrFlag = this.set(f,this.extrFlag,Constants.EXTRA_FLAG_DOWN_SSL);
}

Message.prototype.isRsaEnc=function() {
    return this.is(this.extrFlag,Constants.EXTRA_FLAG_ENC_TYPE);
}

Message.prototype.setEncType=function(/*boolean*/ f) {
    this.extrFlag = this.set(f,this.extrFlag,Constants.EXTRA_FLAG_ENC_TYPE);
}

Message.prototype.isSecretVersion=function() {
    return this.is(this.extrFlag,Constants.EXTRA_FLAG_SECTET_VERSION);
}

Message.prototype.setSecretVersion=function(/*boolean*/ f) {
    this.extrFlag = this.set(f,this.extrFlag,Constants.EXTRA_FLAG_SECTET_VERSION);
}

Message.prototype.isSign=function() {
    return this.is(this.extrFlag,Constants.EXTRA_FLAG_IS_SIGN);
}

Message.prototype.setSign=function(f) {
    this.extrFlag = this.set(f,this.extrFlag,Constants.EXTRA_FLAG_IS_SIGN);
}

Message.prototype.isSec=function() {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_IS_SEC);
}

Message.prototype.setSec=function( f) {
    this.extrFlag = this.set(f,this.extrFlag, Constants.EXTRA_FLAG_IS_SEC);
}

Message.prototype.isFromWeb=function() {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_IS_FROM_WEB);
}

Message.prototype.setFromWeb=function( f) {
    this.extrFlag = this.set(f,this.extrFlag, Constants.EXTRA_FLAG_IS_FROM_WEB);
}

Message.prototype.isRpcMk=function() {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_RPC_MCODE);
}

Message.prototype.setRpcMk=function(/*boolean*/ f) {
    this.extrFlag = this.set(f,this.extrFlag, Constants.EXTRA_FLAG_RPC_MCODE);
}

Message.prototype.isDumpUpStream=function() {
    return this.is(this.extrFlag,Constants.EXTRA_FLAG_DUMP_UP);
}

Message.prototype.setDumpUpStream=function( f) {
    //flag0 |= f ? FLAG0_DUMP_UP : 0 ;
    this.extrFlag = this.set(f,this.extrFlag,Constants.EXTRA_FLAG_DUMP_UP);
}

Message.prototype.isDumpDownStream=function() {
    return this.is(this.extrFlag,Constants.EXTRA_FLAG_DUMP_DOWN);
}

Message.prototype.setDumpDownStream=function( f) {
    this.extrFlag = this.set(f,this.extrFlag,Constants.EXTRA_FLAG_DUMP_DOWN);
}

Message.prototype.isLoggable=function() {
    return this.getLogLevel() > 0;
}

Message.prototype.isDebugMode=function() {
    return this.is(this.extrFlag,Constants.EXTRA_FLAG_DEBUG_MODE);
}

Message.prototype.setDebugMode=function( f) {
    this.extrFlag = this.set(f,this.extrFlag,Constants.EXTRA_FLAG_DEBUG_MODE);
}

Message.prototype.isMonitorable=function() {
    return this.is(this.flag,Constants.FLAG_MONITORABLE);
}

Message.prototype.setMonitorable=function( f) {
    this.flag = this.set(f,this.flag,Constants.FLAG_MONITORABLE);
}

Message.prototype.isError=function() {
    return this.is(this.flag,Constants.FLAG_ERROR);
}

Message.prototype.setError=function( f) {
    this.flag = this.set(f,this.flag,Constants.FLAG_ERROR);
}

Message.prototype.isOuterMessage=function() {
    return this.is(this.flag,Constants.FLAG_OUT_MESSAGE);
}

Message.prototype.isForce2Json=function() {
    return this.is(this.flag,Constants.FLAG_FORCE_RESP_JSON);
}

Message.prototype.setForce2Json=function(f) {
    this.flag = this.set(f, this.flag,Constants.FLAG_FORCE_RESP_JSON);
}

Message.prototype.setOuterMessage=function( f) {
    this.flag = this.set(f,this.flag,Constants.FLAG_OUT_MESSAGE);
}

Message.prototype.isNeedResponse=function() {
    let rt = this.getRespType();
    return rt != Constants.MSG_TYPE_NO_RESP;
}

Message.prototype.isPubsubMessage=function() {
    let rt = this.getRespType();
    return rt == Constants.MSG_TYPE_MANY_RESP;
}

Message.prototype.isPingPong=function() {
    let rt = this.getRespType();
    return rt != Constants.MSG_TYPE_PINGPONG;
}

/**
 * @param f true 表示整数，false表示短整数
 */
Message.prototype.setLengthType=function( f) {
    //flag |= f ? FLAG_LENGTH_INT : 0 ;
    this.flag = this.set(f,this.flag,Constants.FLAG_LENGTH_INT);
}

Message.prototype.isLengthInt=function() {
    return this.is(this.flag,Constants.FLAG_LENGTH_INT);
}

Message.prototype.getPriority=function() {
    return ((this.extrFlag >>> Constants.EXTRA_FLAG_PRIORITY) & 0x03);
}

Message.prototype.setPriority=function( l) {
    if(l > Constants.PRIORITY_3 || l < Constants.PRIORITY_0) {
        throw "Invalid priority: "+l;
    }
    this.extrFlag = (l << Constants.EXTRA_FLAG_PRIORITY) | this.extrFlag;
}

Message.prototype.getLogLevel=function() {
    return ((this.flag >>> Constants.FLAG_LOG_LEVEL) & 0x07);
}
//000 001 010 011 100 101 110 111
Message.prototype.setLogLevel=function( v) {
    if(v < 0 || v > 6) {
        throw "Invalid Log level: "+v;
    }
    this.flag = ((v << Constants.FLAG_LOG_LEVEL) | this.flag);
}

Message.prototype.getRespType=function() {
    return ((this.flag >>> Constants.FLAG_RESP_TYPE) & 0x03);
}

Message.prototype.setRespType=function( v) {
    if(v < 0 || v > 3) {
        throw ("Invalid message response type: "+v);
    }
    this.flag = ((v << Constants.FLAG_RESP_TYPE)|this.flag);
}

Message.prototype.getUpProtocol=function() {
    return this.is(this.flag,Constants.FLAG_UP_PROTOCOL)?1:0;
}

Message.prototype.setUpProtocol=function( protocol) {
    //flag |= protocol == PROTOCOL_JSON ? FLAG_UP_PROTOCOL : 0 ;
    this.flag = this.set(protocol == Constants.PROTOCOL_JSON,this.flag,Constants.FLAG_UP_PROTOCOL);
}

Message.prototype.getDownProtocol=function() {
    return this.is(this.flag,Constants.FLAG_DOWN_PROTOCOL)?1:0;
}

Message.prototype.setDownProtocol=function( protocol) {
    //flag |= protocol == PROTOCOL_JSON ? FLAG_DOWN_PROTOCOL : 0 ;
    this.flag = this.set(protocol == Constants.PROTOCOL_JSON,this.flag,Constants.FLAG_DOWN_PROTOCOL);
}

Message.prototype.writeUnsignedShort=function( b, v) {
    if(v > Constants.MAX_SHORT_VALUE) {
        throw "Max short value is :"+Constants.MAX_SHORT_VALUE+", but value "+v;
    }
    let data = ((v >>> 8) & 0xFF);
    b.put(data);
    data = ((v >>> 0) & 0xFF);
    b.put(data);
}

Message.prototype.readUnsignedShort=function( b) {
    let firstByte = (0xFF & (b.get()));
    let secondByte = (0xFF & (b.get()));
    let anUnsignedShort  =  (firstByte << 8 | secondByte);
    return anUnsignedShort;
}

Message.prototype.readUnsignedLong=function( b) {
    let firstByte = (0xFF & (b.get()));
    let secondByte = (0xFF & (b.get()));
    let thirdByte = (0xFF & (b.get()));
    let fourByte = (0xFF & (b.get()));

    let fiveByte = (0xFF & (b.get()));
    let sixByte = (0xFF & (b.get()));
    let sevenByte = (0xFF & (b.get()));
    let eigthByte = (0xFF & (b.get()));

    let anUnsignedShort  = (
        firstByte << 56 | secondByte<<48
        | thirdByte << 40 | fourByte<<32
        | fiveByte << 24 | sixByte<<16
        | sevenByte << 8 | eigthByte
    );
    return anUnsignedShort;
}

Message.prototype.wiriteUnsignedLong=function( b, val) {

    b.put((0xFF & (val >>> 56)));
    b.put((0xFF & (val >>> 48)));
    b.put((0xFF & (val >>> 40)));
    b.put((0xFF & (val >>> 32)));

    b.put((0xFF & (val >>> 24)));
    b.put((0xFF & (val >>> 16)));
    b.put((0xFF & (val >>> 8)));
    b.put((0xFF & (val >>> 0)));

    return;
}

Message.prototype.writeUnsignedByte=function( b, v) {
    if(v > Constants.MAX_BYTE_VALUE) {
        throw "Max byte value is :"+Constants.MAX_BYTE_VALUE+", but value "+v;
    }
    let vv = ((v >>> 0) & 0xFF);
    b.put(vv);
}

Message.prototype.readUnsignedByte=function( b) {
    let vv =  (b.get() & 0xff);
    return vv;
}

Message.prototype.readUnsignedInt=function( buf) {
    /*int firstByte = (0xFF & ((int)b.get()));
    int secondByte = (0xFF & ((int)b.get()));
    int thirdByte = (0xFF & ((int)b.get()));
    int fourthByte = (0xFF & ((int)b.get()));
     long anUnsignedInt  =
             ((long) (firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte))
             & 0xFFFFFFFFL;
     return anUnsignedInt;*/

    let b = buf.get() & 0xff;
    let n = b & 0x7f;
    if (b > 0x7f) {
        b = buf.get() & 0xff;
        n ^= (b & 0x7f) << 7;
        if (b > 0x7f) {
            b = buf.get() & 0xff;
            n ^= (b & 0x7f) << 14;
            if (b > 0x7f) {
                b = buf.get() & 0xff;
                n ^= (b & 0x7f) << 21;
                if (b > 0x7f) {
                    b = buf.get() & 0xff;
                    n ^= (b & 0x7f) << 28;
                    if (b > 0x7f) {
                        throw "Invalid int encoding";
                    }
                }
            }
        }
    }
    return (n >>> 1) ^ -(n & 1);

}

Message.prototype.writeUnsignedInt=function( buf, n) {
    if(n > Constants.MAX_INT_VALUE) {
        throw "Max int value is :"+Constants.MAX_INT_VALUE+", but value "+n;
    }
    /*b.put((byte)((v >>> 24)&0xFF));
    b.put((byte)((v >>> 16)&0xFF));
    b.put((byte)((v >>> 8)&0xFF));
    b.put((byte)((v >>> 0)&0xFF));*/

    n = (n << 1) ^ (n >> 31);
    if ((n & ~0x7F) != 0) {
        buf.put( ((n | 0x80) & 0xFF));
        n >>>= 7;
        if (n > 0x7F) {
            buf.put(((n | 0x80) & 0xFF));
            n >>>= 7;
            if (n > 0x7F) {
                buf.put(((n | 0x80) & 0xFF));
                n >>>= 7;
                if (n > 0x7F) {
                    buf.put(((n | 0x80) & 0xFF));
                    n >>>= 7;
                }
            }
        }
    }
    buf.put(n);
}

Message.prototype.setInsId=function(insId) {
    if(insId < 0) {
        insId = -insId;
    }
    this.putExtra(Constants.EXTRA_KEY_INSID, insId,Constants.PREFIX_TYPE_INT);
}

Message.prototype.getInsId=function() {
    let v = this.getExtra(Constants.EXTRA_KEY_INSID);
    return v ? 0:v;
}

Message.prototype.setSignData=function( data) {
    this.putExtra(Constants.EXTRA_KEY_SIGN, data,Constants.PREFIX_TYPE_LIST);
}

Message.prototype.getSignData=function() {
    return this.getExtra(Constants.EXTRA_KEY_SIGN);
}

Message.prototype.setLinkId=function( insId) {
    this.putExtra(Constants.EXTRA_KEY_LINKID, insId,Constants.PREFIX_TYPE_LONG);
}

Message.prototype.getLinkId=function() {
    let v = this.getExtra(Constants.EXTRA_KEY_LINKID);
    return v ? 0 : v;
}

Message.prototype.setSaltData=function(/*byte[] */data) {
    this.putExtra(Constants.EXTRA_KEY_SALT, data,Constants.PREFIX_TYPE_LIST);
}

Message.prototype.getSaltData=function() {
    return this.getExtra(Constants.EXTRA_KEY_SALT);
}

Message.prototype.setSecData=function(/*byte[]*/ data) {
    this.putExtra(Constants.EXTRA_KEY_SEC, data,Constants.PREFIX_TYPE_LIST);
}

Message.prototype.getSecData=function() {
    return this.getExtra(Constants.EXTRA_KEY_SEC);
}

Message.prototype.setSmKeyCode=function(/*Integer*/ code) {
    this.putExtra(Constants.EXTRA_KEY_SM_CODE, code,Constants.PREFIX_TYPE_INT);
}

Message.prototype.getSmKeyCode=function() {
    let v = this.getExtra(Constants.EXTRA_KEY_SM_CODE);
    return v == null? 0:v;
}

Message.prototype.setMethod=function(/*String*/ method) {
    this.putExtra(Constants.EXTRA_KEY_SM_NAME, method,Constants.PREFIX_TYPE_STRING);
}

Message.prototype.getMethod=function() {
    return this.getExtra(Constants.EXTRA_KEY_SM_NAME);
}

Message.prototype.setTime=function(/*Long*/ time) {
    this.putExtra(Constants.EXTRA_KEY_TIME, time,Constants.PREFIX_TYPE_LONG);
}

Message.prototype.getTime=function() {
    let v = this.getExtra(Constants.EXTRA_KEY_TIME);
    return !v ? 0:v;
}

export {Constants}

export {Message}
