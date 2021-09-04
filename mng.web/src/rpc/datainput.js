import utils from "./utils";
import {Constants} from "./message";

let JDataInput = function(buf) {
    if(!buf) {
        throw 'Read buf cannot be null';
    }
    if(buf instanceof Array) {
        this.buf = new DataView(new ArrayBuffer(buf.length),0, buf.length) ;
        for(let i = 0; i < buf.length; i++) {
            this.buf.setInt8(i,buf[i]);
        }
    }else if (buf instanceof ArrayBuffer) {
        this.buf = new DataView(buf,0, buf.byteLength);
    }else {
        throw 'Not support construct ArrayBuffer from '+(typeof buf);
    }

    this.readPos = 0;
}

//public static int
JDataInput.prototype.readDouble = function() {
    let fd = this.buf.getFloat64(this.readPos,false);
    this.readPos += 8;
    return fd;
},

    JDataInput.prototype.readFloat = function() {
        let fd = this.buf.getFloat32(this.readPos,false);
        this.readPos += 4;
        return fd;
    },

    JDataInput.prototype.readBoolean = function() {
        return this.getUByte() != 0;
    },

    JDataInput.prototype.readChar = function() {
        return this.readUnsignedShort();
    },

//public static int
    JDataInput.prototype.readUnsignedShort = function() {
        let firstByte = this.atByte();
        let secondByte = this.atByte();
        let anUnsignedShort  =  firstByte << 8 | secondByte;
        return anUnsignedShort;
    },

    JDataInput.prototype.readInt = function() {
        let firstByte = this.atByte();
        let secondByte = this.atByte();
        let thirdByte = this.atByte();
        let fourthByte = this.atByte();
        let anUnsignedInt  = ((firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte))/* & 0xFFFFFFFF*/;
        return anUnsignedInt;
    },

//public static long
    JDataInput.prototype.readUnsignedInt = function() {
        return this.readInt();
        /*let b = this.getUByte() & 0xff;
        let n = b & 0x7f;
        if (b > 0x7f) {
            b = this.getUByte() & 0xff;
            n ^= (b & 0x7f) << 7;
            if (b > 0x7f) {
                b = this.getUByte() & 0xff;
                n ^= (b & 0x7f) << 14;
                if (b > 0x7f) {
                    b = this.getUByte() & 0xff;
                    n ^= (b & 0x7f) << 21;
                    if (b > 0x7f) {
                        b = this.getUByte() & 0xff;
                        n ^= (b & 0x7f) << 28;
                        if (b > 0x7f) {
                            throw "Invalid int encoding";
                        }
                    }
                }
            }
        }
        return (n >>> 1) ^ -(n & 1);*/ // back to two's-complement
    },

    JDataInput.prototype.getUByte = function() {
        return this.buf.getUint8(this.readPos++);
    }

JDataInput.prototype.getByte = function() {
    return this.buf.getInt8(this.readPos++);
}

JDataInput.prototype.atByte = function() {
    return 0xFF & this.getUByte();
}

JDataInput.prototype.remaining = function() {
    return this.buf.byteLength - this.readPos;
}

//public static long
JDataInput.prototype.readUnsignedLong = function() {
    let firstByte = this.atByte();
    let secondByte = this.atByte();
    let thirdByte = this.atByte();
    let fourthByte = this.atByte();

    let fiveByte = this.atByte();
    let sixByte = this.atByte();
    let sevenByte = this.atByte();
    let eightByte = this.atByte();

    let anUnsignedLong  =
        (firstByte << 56 | secondByte << 48 | thirdByte << 40 | fourthByte << 32 |
            fiveByte << 24 | sixByte << 16 | sevenByte << 8 | eightByte) /*& 0xFFFFFFFFFFFFFFFF*/;
    return anUnsignedLong;
},

    JDataInput.prototype.readByteArray = function(len) {
        if(len > this.remaining()) {
            throw "Index out of bound";
        }
        let pa = [];
        for(let i = 0; i < len ; i++) {
            pa.push(this.getUByte());
        }
        return pa;
    }

JDataInput.prototype.readUtf8String = function() {
    let len = this.getUByte();

    if(len == -1) {
        return null;
    }else if(len == 0) {
        return "";
    }

    //Byte.MAX_VALUE
    if(len == Constants.MAX_BYTE_VALUE) {
        len = this.readUnsignedShort();
        //Short.MAX_VALUE
        if(len == Constants.MAX_SHORT_VALUE) {
            len = this.readUnsignedInt();
        }
    }

    let arr = [];
    for(let i = 0; i < len; i++) {
        arr.push(this.getUByte());
    }

    return utils.fromUTF8Array(arr);
}


export default JDataInput;