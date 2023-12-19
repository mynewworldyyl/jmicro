/* eslint-disabled */
import utils from './utils';
import { Constants } from './message';

let JDataInput = function (buf) {
    if (!buf) {
        throw 'Read buf cannot be null';
    }

    if (buf instanceof Array) {
        this.buf = new DataView(new ArrayBuffer(buf.length), 0, buf.length);

        for (let i = 0; i < buf.length; i++) {
            this.buf.setInt8(i, buf[i]);
        }
    } else {
        this.buf = new DataView(buf, 0, buf.byteLength);
       /* if (buf instanceof ArrayBuffer) {
        } else {
            throw 'Not support construct ArrayBuffer from ' + typeof buf;
        }*/
    }

    this.readPos = 0;
}; //public static int

JDataInput.prototype.readDouble = function () {
    let fd = this.buf.getFloat64(this.readPos);
    this.readPos += 8;
    return fd;
};

JDataInput.prototype.readFloat = function () {
    let fd = this.buf.getFloat32(this.readPos);
    this.readPos += 4;
    return fd;
};

JDataInput.prototype.readBoolean = function () {
    return this.readUByte() != 0;
};

JDataInput.prototype.readChar = function () {
    return this.readUnsignedShort();
};

//public static int
JDataInput.prototype.readUnsignedShort = function () {
    let firstByte = this._atByte();
    let secondByte = this._atByte();
    let anUnsignedShort = (firstByte << 8) | secondByte;
    return anUnsignedShort;
};

JDataInput.prototype.readShort = function () {
	let v = this.buf.getInt16(this.readPos);
	this.readPos+=2
	return v
};

JDataInput.prototype.readDouble = function () {
    let v = this.buf.getFloat64(this.readPos);
	this.readPos+=8
	return v
};

JDataInput.prototype.readInt = function () {
    let firstByte = this._atByte();
    let secondByte = this._atByte();
    let thirdByte = this._atByte();
    let fourthByte = this._atByte();
    let anUnsignedInt = (firstByte << 24) | (secondByte << 16) | (thirdByte << 8) | fourthByte;
    /* & 0xFFFFFFFF*/
    return anUnsignedInt;
};


JDataInput.prototype.readUByte = function () {
    return this.buf.getUint8(this.readPos++);
};

JDataInput.prototype.readByte = function () {
    return this.buf.getInt8(this.readPos++);
};

JDataInput.prototype._atByte = function () {
    return 255 & this.readUByte();
};

JDataInput.prototype.remaining = function () {
    return this.buf.byteLength - this.readPos;
}; //public static long

JDataInput.prototype.readLong = function () {
	let v = 
	(this._atByte() << 56) | (this._atByte() << 48) | (this._atByte() << 40) | (this._atByte() << 32)
	| (this._atByte() << 24) | (this._atByte() << 16) | (this._atByte() << 8) | (this._atByte());
	return v;
};

JDataInput.prototype.readByteArray = function (len) {
    if (len > this.remaining()) {
        throw 'Index out of bound';
    }

    let pa = [];

    for (let i = 0; i < len; i++) {
        pa.push(this.readUByte());
    }

    return pa;
};

JDataInput.prototype.readUtf8String = function () {
    let len = this.readByte();

    if (len == -1) {
        return null;
    } else {
        if (len == 0) {
            return '';
        }
    } //Byte.MAX_VALUE

    if (len == Constants.MAX_BYTE_VALUE) {
        len = this.readUnsignedShort(); //Short.MAX_VALUE
        if (len == Constants.MAX_SHORT_VALUE) {
            len = this.readInt();
        }
    }

    let arr = [];

    for (let i = 0; i < len; i++) {
        arr.push(this.readUByte());
    }

    return utils.fromUTF8Array(arr);
};

export default JDataInput;
