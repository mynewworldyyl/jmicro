import { Constants } from './message'

export default {

	/**
	 * @param {JDataOutput} buf
	 * @param {Array or ArrayBuffer} byte array
	 */
	writeArray(buf, array) {
	    if (array instanceof ArrayBuffer) {
	        let size = array.byteLength;
	        let dv = new DataView(array);
	        buf.checkCapacity(size);
	
	        for (let i = 0; i < size; i++) {
	            buf.writeUByte(dv.getUint8(i));
	        }
	    } else {
	        let size = array.length;
	        buf.checkCapacity(size);
	        for (let i = 0; i < size; i++) {
	            buf.writeUByte(array[i]);
	        }
	    }
	},
	
	/**
	 * @param {JDataOutput} b
	 * @param {Object} any value
	 */
	decodeJSVal(b) {
		
		let type = b.readByte()
		if(type == Constants.PREFIX_TYPE_NULL) {
		    return null;
		}
		
		if(Constants.PREFIX_TYPE_BYTE == type) {
			return b.readByte();
		}else if(Constants.PREFIX_TYPE_SHORTT == type) {
			return b.readShort();
		}else if(Constants.PREFIX_TYPE_INT == type) {
			return b.readInt();
		} else if(Constants.PREFIX_TYPE_LONG == type) {
			return b.readLong();
		}else if(Constants.PREFIX_TYPE_DOUBLE == type) {
			return b.readDouble();
		}else if(Constants.PREFIX_TYPE_BOOLEAN == type) {
			return b.readByte()==1;
		}else if(Constants.PREFIX_TYPE_STRINGG == type) {
			return b.readUtf8String();
		}else if(Constants.PREFIX_TYPE_LIST == type) {
			let len = b.readInt();
			let ar = []
			for(let i = 0; i < len; i++) {
				ar.push(this.decodeJSVal(b))
			}
			return ar
		}else if(Constants.PREFIX_TYPE_PROXY == type) {
			//对象
			//b.writeByte(Constants.PREFIX_TYPE_PROXY);
			let len = b.readUnsignedShort()
			let obj = {}
			for(let i = 0; i < len; i++) {
				let k = b.readUtf8String()
				let v = this.decodeJSVal(b)
				obj[k] = v
			}
			return obj
		}
	},
	
	/**
	 * @param {JDataOutput} b
	 * @param {Object} any value
	 */
	encodeJSVal(b,v) {
		if(v == null) {
		    b.writeByte(Constants.PREFIX_TYPE_NULL);
		    return;
		}
		
		if(typeof v == 'number') {
			let sv = v+''
			if(sv.indexOf('.') == -1) {
				//不带小数点，则为整数
				let iv = parseInt(v)
				if(iv>=-127 && iv <= 126) {
					b.writeByte(Constants.PREFIX_TYPE_BYTE);
					b.writeByte(v.v);
				}else if(iv>=-32767 && iv <= 32766) {
					b.writeByte(Constants.PREFIX_TYPE_SHORT);
					b.writeShort(v);
				}else if(iv>=-0x80000000 && iv <= 0x7fffffff) {
					b.writeByte(Constants.PREFIX_TYPE_INT);
					b.writeInt(v);
				} else {
					b.writeByte(Constants.PREFIX_TYPE_LONG);
					b.writeLong(v);
				}
			} else {
				//浮点数
				b.writeByte(Constants.PREFIX_TYPE_DOUBLE);
				b.writeDouble(parseFloat(v));
			}
		}else if(typeof v == 'boolean') {
			b.writeByte(Constants.PREFIX_TYPE_BOOLEAN);
			b.writeBoolean(v?1:0);
		}else if(typeof v == 'string') {
			b.writeByte(Constants.PREFIX_TYPE_STRINGG);
			b.writeUtf8String(v);
		}else if(typeof v == 'object') {
			if(v instanceof Array) {
				 b.writeByte(Constants.PREFIX_TYPE_BYTEBUFFER);
				 b.writeInt(v.length);
				for(let i = 0; i < v.length; i++) {
					this.encodeJSVal(b,v[i])
				}
			} else {
				//对象
				 b.writeByte(Constants.PREFIX_TYPE_PROXY);
				let len = 0
				for(let k in this.cxt) {
					len++
				}
				b.writeUnsignedShort(len)
				for(let k in this.cxt) {
					b.writeUtf8String(k)
					this.encodeJSVal(b, e);
				}
			}
		}
	},
	
	/**
	 * before this method called have to write the value key
	 * @param {JDataOutput} b
	 * @param {Object} any value supported by Message.Constants.PREFIX_TYPE_*
	 * v = {v:v,type:dataType,k:key}
	 */
	encodeVal(b,v) {
	    if(v == null) {
	        b.writeByte(Constants.PREFIX_TYPE_NULL);
	        return;
	    }
	
	    if (v.type == Constants.PREFIX_TYPE_BYTEBUFFER) {
	        b.writeByte(Constants.PREFIX_TYPE_BYTEBUFFER);
	        let arr = v.v;
	        b.writeByteArrayWithShortLen(arr);
	    } else {
	        if (v.type == Constants.PREFIX_TYPE_STRINGG) {
	            b.writeByte(Constants.PREFIX_TYPE_STRINGG);
	            b.writeUtf8String(v.v);
	        } else {
	            if (v.type == Constants.PREFIX_TYPE_INT) {
	                b.writeByte(Constants.PREFIX_TYPE_INT);
	                b.writeInt(v.v);
	            } else {
	                if (v.type == Constants.PREFIX_TYPE_BYTE) {
	                    b.writeByte(Constants.PREFIX_TYPE_BYTE);
	                    b.writeByte(v.v);
	                } else {
	                    if (v.type == Constants.PREFIX_TYPE_SHORTT) {
	                        b.writeByte(Constants.PREFIX_TYPE_SHORTT);
	                        b.writeShort(v.v);
	                    } else {
	                        if (v.type == Constants.PREFIX_TYPE_LONG) {
	                            b.writeByte(Constants.PREFIX_TYPE_LONG);
	                            b.writeLong(v.v);
	                        } else {
	                            if (v.type == Constants.PREFIX_TYPE_FLOAT) {
	                                b.writeByte(Constants.PREFIX_TYPE_FLOAT);
	                                b.writeFloat(v.v);
	                            } else {
	                                if (v.type == Constants.PREFIX_TYPE_DOUBLE) {
	                                    b.writeByte(Constants.PREFIX_TYPE_DOUBLE);
	                                    b.writeDouble(v.v);
	                                } else {
	                                    if (v.type == Constants.PREFIX_TYPE_BOOLEAN) {
	                                        b.writeByte(Constants.PREFIX_TYPE_BOOLEAN);
	                                        b.writeBoolean(v.v);
	                                    } else {
	                                        if (v.type == Constants.PREFIX_TYPE_CHAR) {
	                                            b.writeByte(Constants.PREFIX_TYPE_CHAR);
	                                            b.writeChar(v.v);
	                                        } else {
	                                            throw 'not support header type for val: ' + v + ', with type: ' + v.type;
	                                        }
	                                    }
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    }
	},
	
	/**
	 * 从字节流读取一个类型值
	 * @param {JDataInput} b
	 */
	decodeVal(b,k) {
	   
	    let type = b.readByte();
	
	    if (Constants.PREFIX_TYPE_BYTEBUFFER == type) {
	        let len = b.readUnsignedShort();
	
	        if (len == 0) {
	            return new []();
	        }
	
	        let arr = b.readByteArray(len);
	        return {
	            v: arr,
	            type: Constants.PREFIX_TYPE_BYTEBUFFER,
	            key: k
	        };
	    } else {
	        if (type == Constants.PREFIX_TYPE_INT) {
	            let v = b.readInt();
	            return {
	                v: v,
	                type: Constants.PREFIX_TYPE_INT,
	                key: k
	            };
	        } else {
	            if (Constants.PREFIX_TYPE_BYTE == type) {
	                let v = b.readByte();
	                return {
	                    v: v,
	                    type: Constants.PREFIX_TYPE_BYTE,
	                    key: k
	                };
	            } else {
	                if (Constants.PREFIX_TYPE_SHORTT == type) {
	                    let v = b.readShort();
	                    return {
	                        v: v,
	                        type: Constants.PREFIX_TYPE_SHORTT,
	                        key: k
	                    };
	                } else {
	                    if (Constants.PREFIX_TYPE_LONG == type) {
	                        let v = b.readLong();
	                        return {
	                            v: v,
	                            type: Constants.PREFIX_TYPE_LONG,
	                            key: k
	                        };
	                    } else {
	                        if (Constants.PREFIX_TYPE_FLOAT == type) {
	                            let v = b.readFloat();
	                            return {
	                                v: v,
	                                type: Constants.PREFIX_TYPE_FLOAT,
	                                key: k
	                            };
	                        } else {
	                            if (Constants.PREFIX_TYPE_DOUBLE == type) {
	                                let v = b.readDouble();
	                                return {
	                                    v: v,
	                                    type: Constants.PREFIX_TYPE_DOUBLE,
	                                    key: k
	                                };
	                            } else {
	                                if (Constants.PREFIX_TYPE_BOOLEAN == type) {
	                                    let v = b.readBoolean();
	                                    return {
	                                        v: v,
	                                        type: Constants.PREFIX_TYPE_BOOLEAN,
	                                        key: k
	                                    };
	                                } else {
	                                    if (Constants.PREFIX_TYPE_CHAR == type) {
	                                        let v = b.readChar();
	                                        return {
	                                            v: v,
	                                            type: Constants.PREFIX_TYPE_CHAR,
	                                            key: k
	                                        };
	                                    } else {
	                                        if (Constants.PREFIX_TYPE_STRINGG == type) {
	                                            let str = b.readUtf8String();
	                                            return {
	                                                v: str,
	                                                type: Constants.PREFIX_TYPE_STRINGG,
	                                                key: k
	                                            };
	                                        } else {
	                                            if (Constants.PREFIX_TYPE_NULL == type) {
	                                                return {
	                                                    v: null,
	                                                    type: Constants.PREFIX_TYPE_NULL,
	                                                    key: k
	                                                };
	                                            } else {
	                                                throw 'not support header type: ' + type;
	                                            }
	                                        }
	                                    }
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    }
	},
	
}