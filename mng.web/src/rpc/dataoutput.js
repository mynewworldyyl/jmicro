import {Constants} from "@/rpc/message"
import utils from "@/rpc/utils"


const JDataOutput = function(buf) {
    if( buf instanceof ArrayBuffer) {
        this._buf = buf;
    } else if(typeof buf == 'number') {
        let size = parseInt(buf);
        this._buf = new ArrayBuffer(size);
    }
    this.buf = new DataView(this._buf);
    this.oriSize = this._buf.byteLength;
    this.writePos = 0;
}

JDataOutput.prototype.getBuf = function() {
    return this._buf.slice(0,this.writePos);
}

JDataOutput.prototype.writeUByte = function(v) {
    this.buf.setUint8(this.writePos++,v);
}

JDataOutput.prototype.writeFloat = function(v) {
    this.buf.setFloat32(this.writePos,v,false);
    this.writePos += 4;
}

JDataOutput.prototype.writeDouble = function(v) {
    this.buf.setFloat64(this.writePos,v,false);
    this.writePos += 8;
}

JDataOutput.prototype.writeBoolean = function(v) {
    this.writeUByte(v ? 1 : 0);
}

JDataOutput.prototype.writeChar = function(v) {
    this.writeUnsignedShort(v);
}

JDataOutput.prototype.remaining = function() {
    return this._buf.byteLength - this.writePos;
}

JDataOutput.prototype.checkCapacity = function(len) {
    let rem = this.remaining();
    if(rem >= len) {
        return;
    }

    let size = this.oriSize;
    while(size - (this.oriSize-rem) < len) {
        size *= 2;
    }

    this.oriSize = size;

    let buf0 = new ArrayBuffer(this.oriSize);
    let newBuf = new DataView(buf0,0,this.oriSize);
    for(let i = 0; i < this.writePos; i++) {
        newBuf.setUint8(i,this.buf.getUint8(i));
    }
    this._buf = buf0;
    this.buf = newBuf;

}

//public static void
JDataOutput.prototype.writeUnsignedShort = function(v) {
    /*if(v > Constants.MAX_SHORT_VALUE) {
        throw "Max short value is :"+Constants.MAX_SHORT_VALUE+", but value "+v;
    }*/
    this.checkCapacity(2);
    this.writeUByte((v >>> 8) & 0xFF);
    this.writeUByte((v >>> 0) & 0xFF);
},

//public static void
JDataOutput.prototype.writeUnsignedByte = function(v) {
        if(v > Constants.MAX_BYTE_VALUE) {
            throw "Max byte value is :"+Constants.MAX_BYTE_VALUE+", but value "+v;
        }
        this.checkCapacity(1);
        this.writeUByte(v);
    },

JDataOutput.prototype.writeInt = function(v) {
        if(v > Constants.MAX_INT_VALUE) {
            throw "Max int value is :"+Constants.MAX_INT_VALUE+", but value "+v;
        }
        this.checkCapacity(4);
        //高字节底地址
        this.writeUByte((v >>> 24)&0xFF);
        this.writeUByte((v >>> 16)&0xFF);
        this.writeUByte((v >>> 8)&0xFF);
        this.writeUByte((v >>> 0)&0xFF);

    }

//public static void
JDataOutput.prototype.writeUnsignedInt = function(n) {
    if(n > Constants.MAX_UINT_VALUE) {
        throw "Max int value is: "+Constants.MAX_UINT_VALUE+", but value "+n;
    }
    this.writeInt(n);

    /* n = (n << 1) ^ (n >> 31);
     if ((n & ~0x7F) != 0) {
         this.writeUByte((n | 0x80) & 0xFF);
         n >>>= 7;
         if (n > 0x7F) {
             this.writeUByte((n | 0x80) & 0xFF);
             n >>>= 7;
             if (n > 0x7F) {
                 this.writeUByte((n | 0x80) & 0xFF);
                 n >>>= 7;
                 if (n > 0x7F) {
                     this.writeUByte((n | 0x80) & 0xFF);
                     n >>>= 7;
                 }
             }
         }
     }
     this.writeUByte(n);*/

}

JDataOutput.prototype.writeUnsignedLong = function(v) {
    if(v > Constants.MAX_INT_VALUE) {
        throw "Max int value is :"+Constants.MAX_INT_VALUE+", but value "+v;
    }
    //JS无64位表示
    this.checkCapacity(8);
    //this.buf.setBigInt64(this.writePos,v,false);
    //this.writePos += 8;

    this.writeUByte((v >> 56) & 0xFF);
    this.writeUByte((v >> 48) & 0xFF);
    this.writeUByte((v >> 40) & 0xFF);
    this.writeUByte((v >> 32) & 0xFF);
    this.writeUByte((v >> 24) & 0xFF);
    this.writeUByte((v >> 16) & 0xFF);
    this.writeUByte((v >> 8) & 0xFF);
    this.writeUByte((v >> 0) & 0xFF);
}

JDataOutput.prototype.writeByteArrayWithShortLen = function(arr) {
    if(!arr || arr.length == 0) {
        this.checkCapacity(4);
        this.writeUnsignedShort(0)
        return;
    }
    let size = arr.length;
    this.checkCapacity(4+size);
    this.writeUnsignedShort(size);
    for(let i = 0; i < size; i++) {
        this.writeUByte(arr[i])
    }
}

JDataOutput.prototype.writeByteArray = function(arr) {
    if(!arr || arr.length == 0) {
        this.checkCapacity(4);
        this.writeUnsignedInt(0)
        return;
    }
    let size = arr.length;
    this.checkCapacity(4+size);
    this.writeUnsignedInt(size);
    for(let i = 0; i < size; i++) {
        this.writeUByte(arr[i])
    }
}

JDataOutput.prototype.writeArrayBuffer = function(ab) {
    if(!ab || ab.byteLength == 0) {
        this.checkCapacity(4);
        this.writeUnsignedInt(0)
    }else {
        let size = ab.byteLength;
        this.checkCapacity(4+size);
        this.writeUnsignedInt(size);
        for(let i = 0; i < size; i++) {
            this.writeUByte(ab[i])
        }
    }
}

JDataOutput.prototype.writeUtf8String = function(s) {

    if(s == null) {
        this.checkCapacity(1);
        this.writeUByte(-1);
        return 0;
    } else if(s.length == 0) {
        this.checkCapacity(1);
        this.writeUByte(0);
        return 0;
    }

    let self = this;
    let data = utils.toUTF8Array(s);

    let le = data.length;
    let needLen = le;
    if(le < Constants.MAX_BYTE_VALUE) {
        needLen = le +1;
        self.checkCapacity(needLen);
        self.writeUByte(le);
    }else if(le < Constants.MAX_SHORT_VALUE) {
        needLen = le +3;
        this.checkCapacity(needLen);
        self.writeUByte(Constants.MAX_BYTE_VALUE);
        self.writeUnsignedShort(le);
    }else if(le < Constants.MAX_INT_VALUE) {
        needLen = le +7;
        self.checkCapacity(needLen);
        self.writeUByte(Constants.MAX_BYTE_VALUE);
        self.writeUnsignedShort(Constants.MAX_SHORT_VALUE);
        self.writeUnsignedInt(le);
    }else {
        throw "String too long for:" + le;
    }
    if(le > 0) {
        for(let i = 0; i < le; i++) {
            this.writeUByte(data[i]);
        }
    }
    return needLen;
}

JDataOutput.prototype.writeObject = function(obj) {
    let len = 0;
    for(let key in obj) {
        key+"";
        len++;
    }

    this.writeUnsignedInt(len);
    for(let key in obj) {
        //全部写为字符串
        this.writeUtf8String(key+"");
        this.writeUtf8String(obj[key]+"");
    }

}

JDataOutput.prototype.writeObjectArray = function(arr) {
    if(arr == null || arr.length == 0) {
        this.checkCapacity(4);
        this.writeUnsignedInt(0);
    } else {
        this.checkCapacity(4);
        this.writeUnsignedInt(arr.length);
        for(let i = 0; i <arr.length; i++) {
            let o = arr[i];
            let t = typeof o;
            if(t == 'undefined') {
                throw 'RPC param cannot be undefined';
            }else if(t == 'string') {
                this.writeUtf8String(o);
            }else if(t == 'boolean') {
                if(o) {
                    this.writeUByte(1);
                }else {
                    this.writeUByte(0);
                }
            }else if(t == 'number') {
                this.writeUnsignedLong(o);
            }else if(o instanceof  Array) {
                this.writeByteArray(o);
            }else if (o instanceof ArrayBuffer ) {
                this.writeArrayBuffer(o)
            }else if (t == 'object' ) {
                this.writeObject(o)
            }else {
                throw 'not support encode: ' + o;
            }
        }
    }
}

export default JDataOutput;