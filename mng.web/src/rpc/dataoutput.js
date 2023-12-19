/* eslint-disable */
import { Constants } from './message';
import utils from './utils';

let makeSureInt = (v)=>{
	if(typeof v == 'undefined' || v == null) return 0
	if(typeof v == 'number') return v

	if(utils.isValidNumber(v)) {
		if(v.indexOf('.') == -1) {
			return parseInt(v)
		}else {
			return parseFloat(v)
		}
	}
	
	throw 'not number'
}

const JDataOutput = function (buf) {
    if (buf instanceof ArrayBuffer) {
        this._buf = buf;
    } else {
        if (typeof buf == 'number') {
            let size = parseInt(buf);
            this._buf = new ArrayBuffer(size);
        } else {
            this._buf = new ArrayBuffer(256);
        }
    }

    this.buf = new DataView(this._buf);
    this.oriSize = this._buf.byteLength;
    this.writePos = 0;
};

JDataOutput.prototype.getBuf = function () {
    return this._buf.slice(0, this.writePos);
};

JDataOutput.prototype.writeByte = function (v) {
    this.buf.setInt8(this.writePos++, v);
};

JDataOutput.prototype.writeUByte = function (v) {
    this.buf.setUint8(this.writePos++, v);
};

JDataOutput.prototype.writeFloat = function (v) {
    this.buf.setFloat32(this.writePos, v);
    this.writePos += 4;
};

JDataOutput.prototype.writeLong = function (v) {
    //this.buf.setBigInt64(this.writePos, BigInt(v))
    //this.writePos += 8;
	this.checkCapacity(8); 
	
	//高字节底地址 大端
	this.writeUByte((v >>> 56) & 255);
	this.writeUByte((v >>> 48) & 255);
	this.writeUByte((v >>> 40) & 255);
	this.writeUByte((v >>> 32) & 255);
	
	this.writeUByte((v >>> 24) & 255);
	this.writeUByte((v >>> 16) & 255);
	this.writeUByte((v >>> 8) & 255);
	this.writeUByte((v >>> 0) & 255);
	
};

JDataOutput.prototype.writeShort = function (v) {
    this.buf.setInt16(this.writePos, v)
    this.writePos += 2;
};

JDataOutput.prototype.writeDouble = function (v) {
    this.buf.setFloat64(this.writePos, v, false)
    this.writePos += 8;
};

JDataOutput.prototype.writeBoolean = function (v) {
    this.writeUByte(v ? 1 : 0);
};

JDataOutput.prototype.writeChar = function (v) {
    this.writeUnsignedShort(v);
};

JDataOutput.prototype.remaining = function () {
    return this._buf.byteLength - this.writePos;
};

JDataOutput.prototype.checkCapacity = function (len) {
    let rem = this.remaining();

    if (rem >= len) {
        return;
    }

    let size = this.oriSize;

    while (size - (this.oriSize - rem) < len) {
        size *= 2;
    }

    this.oriSize = size;
    let buf0 = new ArrayBuffer(this.oriSize);
    let newBuf = new DataView(buf0, 0, this.oriSize);

    for (let i = 0; i < this.writePos; i++) {
        newBuf.setUint8(i, this.buf.getUint8(i));
    }

    this._buf = buf0;
    this.buf = newBuf;
}; //public static void

JDataOutput.prototype.writeUnsignedShort = function (v) {
    /*if(v > Constants.MAX_SHORT_VALUE) {
      throw "Max short value is :"+Constants.MAX_SHORT_VALUE+", but value "+v;
  }*/
	v = makeSureInt(v)
    this.checkCapacity(2);
    this.writeUByte((v >>> 8) & 255);
    this.writeUByte((v >>> 0) & 255);
};

//public static void
JDataOutput.prototype.writeUnsignedByte = function (v) {
    if (v > Constants.MAX_BYTE_VALUE) {
        throw 'Max byte value is :' + Constants.MAX_BYTE_VALUE + ', but value ' + v;
    }
	v = makeSureInt(v)
    this.checkCapacity(1);
    this.writeUByte(v);
};

JDataOutput.prototype.writeInt = function (v) {
    if (v > Constants.MAX_INT_VALUE) {
        throw 'WriteInt Max int value is :' + Constants.MAX_INT_VALUE + ', but value ' + v;
    }
	
	v = makeSureInt(v)

    this.checkCapacity(4); //高字节底地址

    this.writeUByte((v >>> 24) & 255);
    this.writeUByte((v >>> 16) & 255);
    this.writeUByte((v >>> 8) & 255);
    this.writeUByte((v >>> 0) & 255);
}; //public static void

JDataOutput.prototype.writeByteArrayWithShortLen = function (arr) {
    if (!arr || arr.length == 0) {
        this.checkCapacity(4);
        this.writeUnsignedShort(0);
        return;
    }

    let size = arr.length;
    this.checkCapacity(4 + size);
    this.writeUnsignedShort(size);

    for (let i = 0; i < size; i++) {
        this.writeUByte(arr[i]);
    }
};

JDataOutput.prototype.writeByteArray = function (arr) {
    if (!arr || arr.length == 0) {
        this.checkCapacity(4);
        this.writeInt(0);
        return;
    }

    let size = arr.length;
    this.checkCapacity(4 + size);
    this.writeInt(size);

    for (let i = 0; i < size; i++) {
        this.writeUByte(arr[i]);
    }
};

JDataOutput.prototype.writeArrayBuffer = function (ab) {
    if (!ab || ab.byteLength == 0) {
        this.checkCapacity(4);
        this.writeInt(0);
    } else {
        let size = ab.byteLength;
        this.checkCapacity(4 + size);
        this.writeInt(size);
        let dv = new DataView(ab, 0, size);

        for (let i = 0; i < size; i++) {
            this.writeUByte(dv.getUint8(i));
        }
    }
};

JDataOutput.prototype.writeUtf8String = function (s) {
    if (s == null) {
        this.checkCapacity(1);
        this.writeByte(-1);//代表空字符串
        return 0;
    } else {
        if (s.length == 0) {
            this.checkCapacity(1);
            this.writeByte(0);
            return 0;
        }
    }

    let that = this;
    let data = utils.toUTF8Array(s);
    let le = data.length;
    let needLen = le;

    if (le < Constants.MAX_BYTE_VALUE) {
        needLen = le + 1;
        that.checkCapacity(needLen);
        that.writeByte(le);
    } else {
        if (le < Constants.MAX_SHORT_VALUE) {
            needLen = le + 3;
            this.checkCapacity(needLen);
            that.writeByte(Constants.MAX_BYTE_VALUE);
            that.writeUnsignedShort(le);
        } else {
            if (le < Constants.MAX_INT_VALUE) {
                needLen = le + 7;
                that.checkCapacity(needLen);
                that.writeByte(Constants.MAX_BYTE_VALUE);
                that.writeUnsignedShort(Constants.MAX_SHORT_VALUE);
                that.writeInt(le);
            } else {
                throw 'String too long for:' + le;
            }
        }
    }

    if (le > 0) {
        for (let i = 0; i < le; i++) {
            this.writeUByte(data[i]);
        }
    }

    return needLen;
};

JDataOutput.prototype.writeObject = function (obj) {
    let len = 0;

    for (let key in obj) {
        len++;
    }

    this.writeInt(len);

    for(let key in obj) {
        //全部写为字符串
        this.writeUtf8String(key + '');
        this.writeUtf8String(obj[key] + '');
    }
};

JDataOutput.prototype.writeObjectArray = function (arr) {
    if (arr == null || arr.length == 0) {
        this.checkCapacity(4);
        this.writeInt(0);
    } else {
        this.checkCapacity(4);
        this.writeInt(arr.length);

        for (let i = 0; i < arr.length; i++) {
            let o = arr[i];
            let t = typeof o;

            if (t == 'undefined') {
                throw 'RPC param cannot be undefined';
            } else {
                if (t == 'string') {
                    this.writeUtf8String(o);
                } else {
                    if (t == 'boolean') {
                        if (o) {
                            this.writeUByte(1);
                        } else {
                            this.writeUByte(0);
                        }
                    } else {
                        if (t == 'number') {
                            this.writeLong(o);
                        } else {
                            if (o instanceof Array) {
                                this.writeByteArray(o);
                            } else {
                                if (o instanceof ArrayBuffer) {
                                    this.writeArrayBuffer(o);
                                } else {
                                    if (t == 'object') {
                                        this.writeObject(o);
                                    } else {
                                        throw 'not support encode: ' + o;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
};

export default JDataOutput;
