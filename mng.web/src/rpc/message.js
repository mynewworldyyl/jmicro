import JDataInput from './datainput';
import utils from './utils';
import config from './config';
import JDataOutput from './dataoutput'; //import localStorage from "./localStorage";
import serial from './serialize.js'

let PREFIX_TYPE_ID = -128;
let security = null;

if (config.sslEnable) {
    security = require('./security');
}

let Constants = {
	VER_DATA_KEY:'versionData_',
    TOKEN: '-119',
    USER_INFO: 'userInfo',
    ACT_NAME_KEY: 'actName',
    ACT_PWD_KEY: 'pwd',
    ACT_REM_PWD_KEY: 'rememberPwd',
	ACT_AUTO_LOGIN_KEY: 'autoLogin',
    ACT_GUEST_NAME_KEY: 'guestName',
	MSG_TYPE_PUBSUB : 3, //订阅消息
	MSG_TYPE_PUBSUB_RESP : 4, //订阅消息响应
    //MSG_TYPE_REQ_RAW : 0x03,  //纯二进制数据请求
    //MSG_TYPE_RRESP_RAW : 0x04, //纯二进制数据响应
    MSG_TYPE_REQ_JRPC: 1,
    //普通RPC调用请求，发送端发IRequest，返回端返回IResponse
    MSG_TYPE_RRESP_JRPC: 2,
    //返回端返回IResponse
    MSG_TYPE_ASYNC_RESP: 6,
    SERVICE_NAMES: 'serviceNames',
    SERVICE_NAMESPACES: 'namespaces',
    SERVICE_VERSIONS: 'versions',
    SERVICE_METHODS: 'methods',
    INSTANCES: 'instances',
    NAMED_TYPES: 'nameTypes',
    ALL_INSTANCES: 'allInstances',
    MONITOR_RESOURCE_NAMES: 'resourceNames',
    LOG_NO: 0,
    LOG_FINAL: 6,
    CHARSET: 'UTF-8',
    LOGIN: 1,
    LOGOUT: 2,
    HEADER_LEN: 13,
//附加数据头部长度
    EXT_HEADER_LEN: 2,
    PROTOCOL_BIN: 0,
    PROTOCOL_JSON: 1,
	PROTOCOL_EXTRA: 2,
    PRIORITY_0: 0,
    PRIORITY_1: 1,
    PRIORITY_2: 2,
    PRIORITY_3: 3,
    PRIORITY_4: 4,
    PRIORITY_5: 5,
    PRIORITY_6: 6,
    PRIORITY_7: 7,
    PRIORITY_MIN: 0,
    PRIORITY_NORMAL: 3,
    PRIORITY_MAX: 7,
    MAX_SHORT_VALUE: 32767,
    MAX_BYTE_VALUE: 126,
    MAX_INT_VALUE: 2147483647,
    //10M 0x7FFFFFFF,
    //长度字段类型，1表示整数，0表示短整数
    FLAG_LENGTH_INT: 1 << 0,
    FLAG_UP_PROTOCOL : 1, //1,2位一起表示上行数据打包协议
   	FLAG_DOWN_PROTOCOL : 8, //8，9位一起表示下行数据打包协议
    //可监控消息
    FLAG_MONITORABLE: 1 << 3,
    //包含Extra数据
    FLAG_EXTRA: 1 << 4,
    //需要响应的请求  or down message is error
    //来自外网消息，由网关转发到内网
    FLAG_OUT_MESSAGE: 1 << 5,
    FLAG_ERROR: 1 << 6,
    FLAG_FORCE_RESP_JSON: 1 << 7,
	
	FLAG_DEV : 1<<10,
    FLAG_LOG_LEVEL: 13,
    FLAG_RESP_TYPE: 11,

    /****************  extra constants flag   *********************/
    //调试模式
    EXTRA_FLAG_DEBUG_MODE: 1 << 0,
    EXTRA_FLAG_PRIORITY: 1,
    //DUMP上行数据
    EXTRA_FLAG_DUMP_UP: 1 << 3,
    //DUMP下行数据
    EXTRA_FLAG_DUMP_DOWN: 1 << 4,
    //加密参数 0：没加密，1：加密
    EXTRA_FLAG_UP_SSL: 1 << 5,
    //是否签名
    EXTRA_FLAG_DOWN_SSL: 1 << 6,
    EXTRA_FLAG_IS_FROM_WEB: 1 << 7,
    EXTRA_FLAG_IS_SEC: 1 << 8,
    //是否签名： 0:无签名； 1：有签名
    EXTRA_FLAG_IS_SIGN: 1 << 9,
    //加密方式： 0:对称加密，1：RSA 非对称加密
    EXTRA_FLAG_ENC_TYPE: 1 << 10,
    EXTRA_FLAG_RPC_MCODE: 1 << 11,
    EXTRA_FLAG_SECTET_VERSION: 1 << 12,
    //是否包含实例ID
    EXTRA_FLAG_INS_ID: 1 << 13,
    EXTRA_KEY_LINKID: -127,
    EXTRA_KEY_INSID: -126,
    EXTRA_KEY_TIME: -125,
    EXTRA_KEY_SM_CODE: -124,
    EXTRA_KEY_SM_NAME: -123,
    EXTRA_KEY_SIGN: -122,
    EXTRA_KEY_SALT: -121,
    EXTRA_KEY_SEC: -120,
    EXTRA_KEY_LOGIN_KEY: -119,
    EXTRA_KEY_FLAG: -118,
    EXTRA_KEY_MSG_ID: -117,
    EXTRA_KEY_LOGIN_SYS: -116,
    EXTRA_KEY_ARG_HASH: -115,
	
	EXTRA_KEY_PS_OP_CODE : -114,
	EXTRA_KEY_PS_ARGS : -113,
	EXTRA_KEY_SMSG_ID : -112,//服务器返回全局唯一标识ID
		
    //RPC METHOD NAME
    EXTRA_KEY_METHOD: 127,
    EXTRA_KEY_EXT0: 126,
    EXTRA_KEY_EXT1: 125,
    EXTRA_KEY_EXT2: 124,
    EXTRA_KEY_EXT3: 123,
	EXTRA_KEY_CLIENT_ID: 122,
    //public static final byte MSG_TYPE_PINGPONG = 0;//默认请求响应模式
    MSG_TYPE_PINGPONG: 0,
    //public static final byte MSG_TYPE_NO_RESP = 1;//单向模式
    MSG_TYPE_NO_RESP: 1,
    //public static final byte MSG_TYPE_MANY_RESP = 2;//多个响应模式，如消息订阅
    MSG_TYPE_MANY_RESP: 2,

    /****************  extra constants flag   *********************/
    //PREFIX_TYPE_ID : -128,
    //空值编码
    PREFIX_TYPE_NULL: PREFIX_TYPE_ID++,
    //FINAL
    PREFIX_TYPE_FINAL: PREFIX_TYPE_ID++,
    //类型编码写入编码中
    PREFIX_TYPE_SHORT: PREFIX_TYPE_ID++,
    //全限定类名作为前缀串写入编码中
    PREFIX_TYPE_STRING: PREFIX_TYPE_ID++,
    //以下对高使用频率非final类做快捷编码
    //列表类型编码，指示接下业读取一个列表，取列表编码器直接解码
    PREFIX_TYPE_LIST: PREFIX_TYPE_ID++,
    //集合类型编码，指示接下来读取一个集合，取SET编码器直接解码
    PREFIX_TYPE_SET: PREFIX_TYPE_ID++,
    //Map类型编码，指示接下来读取一个Map，取Map编码器直接解码
    PREFIX_TYPE_MAP: PREFIX_TYPE_ID++,
    
	PREFIX_TYPE_BYTE: PREFIX_TYPE_ID++,
    PREFIX_TYPE_SHORTT: PREFIX_TYPE_ID++,
    PREFIX_TYPE_INT: PREFIX_TYPE_ID++,
    PREFIX_TYPE_LONG: PREFIX_TYPE_ID++,
    PREFIX_TYPE_FLOAT: PREFIX_TYPE_ID++,
    PREFIX_TYPE_DOUBLE: PREFIX_TYPE_ID++,
    PREFIX_TYPE_CHAR: PREFIX_TYPE_ID++,
    PREFIX_TYPE_BOOLEAN: PREFIX_TYPE_ID++,
    PREFIX_TYPE_STRINGG: PREFIX_TYPE_ID++,
    PREFIX_TYPE_DATE: PREFIX_TYPE_ID++,
    PREFIX_TYPE_BYTEBUFFER: PREFIX_TYPE_ID++,
    PREFIX_TYPE_REQUEST: PREFIX_TYPE_ID++,
    PREFIX_TYPE_RESPONSE: PREFIX_TYPE_ID++,
    PREFIX_TYPE_PROXY: PREFIX_TYPE_ID++
};

const  PREFIX_TYPE_2DESC = {}
PREFIX_TYPE_2DESC[Constants.PREFIX_TYPE_BYTE]="字节型"
PREFIX_TYPE_2DESC[Constants.PREFIX_TYPE_SHORTT]="短整型"
PREFIX_TYPE_2DESC[Constants.PREFIX_TYPE_INT]="整型"
PREFIX_TYPE_2DESC[Constants.PREFIX_TYPE_LONG]="长整型"
PREFIX_TYPE_2DESC[Constants.PREFIX_TYPE_FLOAT]="浮点型"
PREFIX_TYPE_2DESC[Constants.PREFIX_TYPE_DOUBLE]="双精度浮点数"
PREFIX_TYPE_2DESC[Constants.PREFIX_TYPE_CHAR]="单字符"
PREFIX_TYPE_2DESC[Constants.PREFIX_TYPE_BOOLEAN]="布尔型"
PREFIX_TYPE_2DESC[Constants.PREFIX_TYPE_STRINGG]="字符串"

Constants.PREFIX_TYPE_2DESC = PREFIX_TYPE_2DESC

function Message() {
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
     * 1        
     * 2        UPR:     1，2位一起表示 up protocol  0: bin,  1: json, 2: extra key value
     * 3        M:       Monitorable
     * 4        Extra    Contain extradata
     * 5        Innet    message from outer network
	 * 6        Error    是否出错
	 * 7        force response JSON 强制响应JSON
     * 8
    *  9        DPR:     8，9位一起表示 down protocol 0: bin,  1: json, 2: extra key value
     * 10
     * 11，12   Resp type  MSG_TYPE_PINGPONG，MSG_TYPE_NO_RESP，MSG_TYPE_MANY_RESP
     * 13 14 15 LLL      Log level
     * @return
     */
    //private short flag = 0;
    this.flag = 0; // 1 byte
    //private byte type;

    this.type = 0; //2 byte length
    //private byte ext;
    //normal message ID	or JRPC request ID
    //private long msgId;

    this.msgId = 0; //private Object payload;

    this.payload = null; //private Map<Byte,Object> extraMap;

    this.extraMap = []; //*****************extra data begin******************//

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

    this.extrFlag = 0; //附加数居
    //private transient ByteBuffer extra = null;

    this.extra = null;
}

Message.prototype.decodeExtra = function (
    /*ByteBuffer*/
    b
) {

	let eleNum = b.readUByte(); //extra元素个数
	if(eleNum < 0) {
		eleNum += 256; //参考encode方法说明
	}
	
	if(eleNum == 0) return null;
    let ed = [];
	
	b.readUByte(); //discard key type
	
    while (eleNum > 0) {
		eleNum--;
		let k = b.readByte();
        let v = serial.decodeVal(b,k);
        ed.push(v);
    }

    return ed;
};

Message.prototype.encodeExtra = function (
    /*Map<Byte, Object> */
    extras,b
) {
    if (extras == null || extras.length == 0) {
		b.writeUByte(0);
        return null;
    }
	b.writeUByte(extras.length);
	
	b.writeByte(0);// Byte key type
	
    for (let i = 0; i < extras.length; i++) {
        let e = extras[i];
        b.writeUByte(e.key);
       serial.encodeVal(b,e)
    }
    //return b.getBuf();
};

Message.prototype.decode = function (arr) {
    //Message msg = new Message();
    let that = this;
    let b = new JDataInput(arr); //第0,1个字节

    let flag = b.readUnsignedShort();
    that.flag = flag; //ByteBuffer b = ByteBuffer.wrap(data);

    let len = 0;

    if (that.isLengthInt()) {
        len = b.readInt();
    } else {
        len = b.readUnsignedShort(); // len = 数据长度 + 附加数据长度
    }

    if (b.remaining() < len) {
        throw 'Message len not valid';
    } 
	//第3个字节
    //msg.setVersion(b.readByte());
    //read type
    //第4个字节

    that.type = b.readUByte();
    that.setMsgId(b.readLong());

    if (that.isReadExtra()) {
		let remainding = b.remaining();
		that.extraMap = this.decodeExtra(b); //msg.setLen(len + elen);
        len = len  - (remainding - b.remaining());
        if (that.containExtra(Constants.EXTRA_KEY_FLAG)) {
            that.extrFlag = that.getExtra(Constants.EXTRA_KEY_FLAG); // msg.extraMap.get(Constants.EXTRA_KEY_FLAG);
        }
    }
    /* else {
         msg.setLen(len);
     }*/

    if (len > 0) {
        that.payload = b.readByteArray(len);

        if (that.isDownSsl() && security) {
            security.checkAndDecrypt(that);
        }

        if (this.getDownProtocol() == Constants.PROTOCOL_JSON) {
            let json = utils.fromUTF8Array(that.payload); //console.log(json);
            that.payload = JSON.parse(json);
        }
    } else {
        that.payload = null;
    }

    that.len = len + Constants.HEADER_LEN;
};

Message.prototype.encode = function () {
    let b = new JDataOutput(1024);
    let len = 0; //数据长度 + 测试模式时附加数据长度

    let data = this.payload;

    if (!(data instanceof ArrayBuffer || data instanceof Array)) {
        let json = JSON.stringify(data);
        data = utils.toUTF8Array(json);
        this.setUpProtocol(Constants.PROTOCOL_JSON);
    } else {
        if (data instanceof ArrayBuffer) {
            let arrData = [];
            let buf = new DataView(data, 0, data.byteLength);

            for (let i = 0; i < data.byteLength; i++) {
                arrData.push(buf.getUint8(i));
            }

            data = arrData;
        }
    }

    this.payload = data;

    if (config.sslEnable) {
        this.setUpSsl(true);
        this.setEncType(false);
        this.setDownSsl(true);
        security.encrypt(this);
    }

    if (this.payload instanceof ArrayBuffer) {
        len = this.payload.byteLength;
    } else {
        len = this.payload.length;
    } //data.mark();

    if (this.extrFlag != 0) {
        this.putExtra(Constants.EXTRA_KEY_FLAG, this.extrFlag, Constants.PREFIX_TYPE_INT);
    }

    if(this.isWriteExtra()) {
		let b = new JDataOutput(512)
        this.encodeExtra(this.extraMap,b)
		this.extra = b.getBuf()
        if (this.extra == null || this.extra.length > 4092) {
            throw 'Too long extra: ' + this.extraMap.toString();
        }

        len += this.extra.byteLength;
        this.setExtra(true);
    } //第1，2个字节 ,len = 数据长度 + 测试模式时附加数据长度

    if (len < Constants.MAX_SHORT_VALUE) {
        this.setLengthType(false);
    } else {
        if(len < Constants.MAX_INT_VALUE) {
            this.setLengthType(true);
        } else {
            throw 'Data length too long than :' + Constants.MAX_INT_VALUE + ', but value ' + len;
        }
    }//第0,1,2,3个字节，标志头
    //b.put(this.flag);

    b.writeUnsignedShort(this.flag);

    if (len < Constants.MAX_SHORT_VALUE) {
        //第2，3个字节 ,len = 数据长度 + 测试模式时附加数据长度
        b.writeUnsignedShort(len);
    } else {
        if (len < Constants.MAX_INT_VALUE) {
            //消息内内容最大长度为MAX_VALUE 2,3,4,5
            b.writeInt(len);
        } else {
            throw 'Max data length is :' + Constants.MAX_INT_VALUE + ', but value ' + len;
        }
    } //b.putShort((short)0);
    //第3个字节
    //b.put(this.version);
    //b.writeByte(this.method);
    //第4个字节
    //writeUnsignedShort(b, this.type);
    //b.put(this.type);

    b.writeUByte(this.type);
    b.writeLong(this.msgId);

    if (this.isWriteExtra()) {
        //b.writeInt(this.extrFlag);
       // b.writeUnsignedShort(this.extra.byteLength);
        this.writeArray(b, this.extra);
    }

    if(data != null) {
        //b.put(data);
        // b.write(data);
        //data.reset();
        this.writeArray(b, this.payload); //b.writeByteArrayWithShortLen(this.data);
    }

    return b.getBuf();
};

Message.prototype.writeArray = function (buf, data) {
   serial.writeArray(buf,data)
};

Message.prototype.is = function (flag, mask) {
    return (flag & mask) != 0;
};

Message.prototype.setMsgId = function (msgId) {
    this.msgId = msgId;
};

Message.prototype.set = function (isTrue, f, mask) {
    return isTrue ? (f |= mask) : (f &= ~mask);
};

Message.prototype.containExtra = function (key) {
    let pm = this.extraMap;

    if (!pm || pm.length == 0) {
        return false;
    }

    for (let i = 0; i < pm.length; i++) {
        if (pm[i].key == key) {
            return true;
        }
    }

    return false;
};

Message.prototype.getExtra = function (key) {
    let pm = this.extraMap;

    if (!pm || pm.length == 0) {
        return null;
    }

    for (let i = 0; i < pm.length; i++) {
        if (pm[i].key == key) {
            return pm[i].v;
        }
    }

    return null;
};

Message.prototype.putExtra = function (key, val, type) {
    let pm = this.extraMap;
    let e = {};
    let f = false;

    if (pm) {
        for (let i = 0; i < pm.length; i++) {
            if (pm[i].key == key) {
                e = pm[i];
                f = true;
                break;
            }
        }
    }

    e.key = key;
    e.type = type;
    e.v = val;

    if (!f) {
        pm.push(e);
    }
};

Message.prototype.isWriteExtra = function () {
    return (
        this.extraMap != null && this.extraMap.length > 0
        /*&& !this.extraMap.isEmpty()*/
    );
};

Message.prototype.isReadExtra = function () {
    return this.is(this.flag, Constants.FLAG_EXTRA);
};

Message.prototype.setExtra = function (
    /*boolean*/
    f
) {
    this.flag = this.set(f, this.flag, Constants.FLAG_EXTRA);
};

Message.prototype.isUpSsl = function () {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_UP_SSL);
};

Message.prototype.setUpSsl = function (
    /*boolean*/
    f
) {
    this.extrFlag = this.set(f, this.extrFlag, Constants.EXTRA_FLAG_UP_SSL);
};

Message.prototype.isDownSsl = function () {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_DOWN_SSL);
};

Message.prototype.setDownSsl = function (
    /*boolean*/
    f
) {
    this.extrFlag = this.set(f, this.extrFlag, Constants.EXTRA_FLAG_DOWN_SSL);
};

Message.prototype.isRsaEnc = function () {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_ENC_TYPE);
};

Message.prototype.setEncType = function (
    /*boolean*/
    f
) {
    this.extrFlag = this.set(f, this.extrFlag, Constants.EXTRA_FLAG_ENC_TYPE);
};

Message.prototype.isSecretVersion = function () {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_SECTET_VERSION);
};

Message.prototype.setSecretVersion = function (
    /*boolean*/
    f
) {
    this.extrFlag = this.set(f, this.extrFlag, Constants.EXTRA_FLAG_SECTET_VERSION);
};

Message.prototype.isSign = function () {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_IS_SIGN);
};

Message.prototype.setSign = function (f) {
    this.extrFlag = this.set(f, this.extrFlag, Constants.EXTRA_FLAG_IS_SIGN);
};

Message.prototype.isSec = function () {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_IS_SEC);
};

Message.prototype.setSec = function (f) {
    this.extrFlag = this.set(f, this.extrFlag, Constants.EXTRA_FLAG_IS_SEC);
};

Message.prototype.isFromWeb = function () {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_IS_FROM_WEB);
};

Message.prototype.setFromWeb = function (f) {
    this.extrFlag = this.set(f, this.extrFlag, Constants.EXTRA_FLAG_IS_FROM_WEB);
};

Message.prototype.isRpcMk = function () {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_RPC_MCODE);
};

Message.prototype.setRpcMk = function (
    /*boolean*/
    f
) {
    this.extrFlag = this.set(f, this.extrFlag, Constants.EXTRA_FLAG_RPC_MCODE);
};

Message.prototype.isDumpUpStream = function () {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_DUMP_UP);
};

Message.prototype.setDumpUpStream = function (f) {
    //flag0 |= f ? FLAG0_DUMP_UP : 0 ;
    this.extrFlag = this.set(f, this.extrFlag, Constants.EXTRA_FLAG_DUMP_UP);
};

Message.prototype.isDumpDownStream = function () {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_DUMP_DOWN);
};

Message.prototype.setDumpDownStream = function (f) {
    this.extrFlag = this.set(f, this.extrFlag, Constants.EXTRA_FLAG_DUMP_DOWN);
};

Message.prototype.isLoggable = function () {
    return this.getLogLevel() > 0;
};

Message.prototype.isDebugMode = function () {
    return this.is(this.extrFlag, Constants.EXTRA_FLAG_DEBUG_MODE);
};

Message.prototype.setDebugMode = function (f) {
    this.extrFlag = this.set(f, this.extrFlag, Constants.EXTRA_FLAG_DEBUG_MODE);
};

Message.prototype.isMonitorable = function () {
    return this.is(this.flag, Constants.FLAG_MONITORABLE);
};

Message.prototype.setMonitorable = function (f) {
    this.flag = this.set(f, this.flag, Constants.FLAG_MONITORABLE);
};

Message.prototype.isDev= function () {
    return this.is(this.flag, Constants.FLAG_DEV);
};

Message.prototype.setDev = function (f) {
    this.flag = this.set(f, this.flag, Constants.FLAG_DEV);
};

Message.prototype.isError = function () {
    return this.is(this.flag, Constants.FLAG_ERROR);
};

Message.prototype.setError = function (f) {
    this.flag = this.set(f, this.flag, Constants.FLAG_ERROR);
};

Message.prototype.isOuterMessage = function () {
    return this.is(this.flag, Constants.FLAG_OUT_MESSAGE);
};

Message.prototype.isForce2Json = function () {
    return this.is(this.flag, Constants.FLAG_FORCE_RESP_JSON);
};

Message.prototype.setForce2Json = function (f) {
    this.flag = this.set(f, this.flag, Constants.FLAG_FORCE_RESP_JSON);
};

Message.prototype.setOuterMessage = function (f) {
    this.flag = this.set(f, this.flag, Constants.FLAG_OUT_MESSAGE);
};

Message.prototype.isNeedResponse = function () {
    let rt = this.getRespType();
    return rt != Constants.MSG_TYPE_NO_RESP;
};

Message.prototype.isPubsubMessage = function () {
    let rt = this.getRespType();
    return rt == Constants.MSG_TYPE_MANY_RESP;
};

Message.prototype.isPingPong = function () {
    let rt = this.getRespType();
    return rt != Constants.MSG_TYPE_PINGPONG;
};
/**
 * @param f true 表示整数，false表示短整数
 */

Message.prototype.setLengthType = function (f) {
    //flag |= f ? FLAG_LENGTH_INT : 0 ;
    this.flag = this.set(f, this.flag, Constants.FLAG_LENGTH_INT);
};

Message.prototype.isLengthInt = function () {
    return this.is(this.flag, Constants.FLAG_LENGTH_INT);
};

Message.prototype.getPriority = function () {
    return (this.extrFlag >>> Constants.EXTRA_FLAG_PRIORITY) & 3;
};

Message.prototype.setPriority = function (l) {
    if (l > Constants.PRIORITY_3 || l < Constants.PRIORITY_0) {
        throw 'Invalid priority: ' + l;
    }

    this.extrFlag = (l << Constants.EXTRA_FLAG_PRIORITY) | this.extrFlag;
};

Message.prototype.getLogLevel = function () {
    return (this.flag >>> Constants.FLAG_LOG_LEVEL) & 7;
}; //000 001 010 011 100 101 110 111

Message.prototype.setLogLevel = function (v) {
    if (v < 0 || v > 6) {
        throw 'Invalid Log level: ' + v;
    }

    this.flag = (v << Constants.FLAG_LOG_LEVEL) | this.flag;
};

Message.prototype.getRespType = function () {
    return (this.flag >>> Constants.FLAG_RESP_TYPE) & 3;
};

Message.prototype.setRespType = function (v) {
    if (v < 0 || v > 3) {
        throw 'Invalid message response type: ' + v;
    }

    this.flag = (v << Constants.FLAG_RESP_TYPE) | this.flag;
};

Message.prototype.getUpProtocol = function () {
	return (this.flag >> Constants.FLAG_UP_PROTOCOL) & 0x03;
};

Message.prototype.setUpProtocol = function (protocol) {
    //this.flag = this.set(protocol == Constants.PROTOCOL_JSON, this.flag, Constants.FLAG_UP_PROTOCOL);
	this.flag = this.flag | (protocol << Constants.FLAG_UP_PROTOCOL);
};

Message.prototype.getDownProtocol = function () {
    //return this.is(this.flag, Constants.FLAG_DOWN_PROTOCOL) ? 1 : 0;
	return (this.flag >> Constants.FLAG_DOWN_PROTOCOL) & 0x03;
};

Message.prototype.setDownProtocol = function (protocol) {
    //flag |= protocol == PROTOCOL_JSON ? FLAG_DOWN_PROTOCOL : 0 ;
	//this.flag = this.flag | (protocol << Constants.FLAG_DOWN_PROTOCOL);
	this.flag = this.flag | (protocol << Constants.FLAG_DOWN_PROTOCOL);
};

Message.prototype.writeUnsignedShort = function (b, v) {
    if (v > Constants.MAX_SHORT_VALUE) {
        throw 'Max short value is :' + Constants.MAX_SHORT_VALUE + ', but value ' + v;
    }

    let data = (v >>> 8) & 255;
    b.put(data);
    data = (v >>> 0) & 255;
    b.put(data);
};

Message.prototype.readUnsignedShort = function (b) {
    let firstByte = 255 & b.get();
    let secondByte = 255 & b.get();
    let anUnsignedShort = (firstByte << 8) | secondByte;
    return anUnsignedShort;
};

Message.prototype.writeUnsignedByte = function (b, v) {
    if (v > Constants.MAX_BYTE_VALUE) {
        throw 'Max byte value is :' + Constants.MAX_BYTE_VALUE + ', but value ' + v;
    }

    let vv = (v >>> 0) & 255;
    b.put(vv);
};

Message.prototype.readUnsignedByte = function (b) {
    let vv = b.get() & 255;
    return vv;
};

Message.prototype.setInsId = function (insId) {
    if (insId < 0) {
        insId = -insId;
    }

    this.putExtra(Constants.EXTRA_KEY_INSID, insId, Constants.PREFIX_TYPE_INT);
};

Message.prototype.getInsId = function () {
    let v = this.getExtra(Constants.EXTRA_KEY_INSID);
    return v ? 0 : v;
};

Message.prototype.setSignData = function (data) {
    this.putExtra(Constants.EXTRA_KEY_SIGN, data, Constants.PREFIX_TYPE_BYTEBUFFER);
};

Message.prototype.getSignData = function () {
    return this.getExtra(Constants.EXTRA_KEY_SIGN);
};

Message.prototype.setLinkId = function (insId) {
    this.putExtra(Constants.EXTRA_KEY_LINKID, insId, Constants.PREFIX_TYPE_LONG);
};

Message.prototype.getLinkId = function () {
    let v = this.getExtra(Constants.EXTRA_KEY_LINKID);
    return v ? 0 : v;
};

Message.prototype.setSaltData = function (
    /*byte[] */
    data
) {
    this.putExtra(Constants.EXTRA_KEY_SALT, data, Constants.PREFIX_TYPE_BYTEBUFFER);
};

Message.prototype.getSaltData = function () {
    return this.getExtra(Constants.EXTRA_KEY_SALT);
};

Message.prototype.setSecData = function (
    /*byte[]*/
    data
) {
    this.putExtra(Constants.EXTRA_KEY_SEC, data, Constants.PREFIX_TYPE_BYTEBUFFER);
};

Message.prototype.getSecData = function () {
    return this.getExtra(Constants.EXTRA_KEY_SEC);
};

Message.prototype.setSmKeyCode = function (
    /*Integer*/
    code
) {
    this.putExtra(Constants.EXTRA_KEY_SM_CODE, code, Constants.PREFIX_TYPE_INT);
};

Message.prototype.getSmKeyCode = function () {
    let v = this.getExtra(Constants.EXTRA_KEY_SM_CODE);
    return v == null ? 0 : v;
};

Message.prototype.setMethod = function (
    /*String*/
    method
) {
    this.putExtra(Constants.EXTRA_KEY_SM_NAME, method, Constants.PREFIX_TYPE_STRINGG);
};

Message.prototype.getMethod = function () {
    return this.getExtra(Constants.EXTRA_KEY_SM_NAME);
};

Message.prototype.setTime = function (
    time
) {
    this.putExtra(Constants.EXTRA_KEY_TIME, time, Constants.PREFIX_TYPE_LONG);
};

Message.prototype.getTime = function () {
    let v = this.getExtra(Constants.EXTRA_KEY_TIME);
    return !v ? 0 : v;
};

export { Constants };
export { Message };
