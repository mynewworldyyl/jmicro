/* eslint-disable */
//import cons from "./constants";
import { Constants } from './message';
const b64map = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
let wxs = null;

if (typeof uni == 'object') {
    wxs = uni;
}

String.prototype.startWith = function (str) {
    var reg = new RegExp('^' + str);
    return reg.test(this);
};

String.prototype.endWith = function (str) {
    var reg = new RegExp(str + '$');
    return reg.test(this);
};

if(!String.prototype.replaceAll) {
	String.prototype.replaceAll = function (oldStr, newStr) {
	    var reg = new RegExp(oldStr,"gm");
	    return this.replace(reg,newStr);
	};
}

Date.prototype.toDecDay = function () {
    let curDate = new Date();
    let decVal = curDate.getTime() - this.getTime();

    if (decVal <= 0) {
        return '0';
    }

    let minuteMS = 60 * 1000;
    let hourMS = 60 * minuteMS;
    let dateMS = 24 * hourMS;
    let str = '';
    let v = parseInt(decVal / dateMS);

    if (v > 0) {
        str += v + 'D,';
    }

    decVal = decVal % dateMS;
    v = parseInt(decVal / hourMS);

    if (v > 0) {
        str += v + 'H,';
    }

    decVal = decVal % hourMS;
    v = parseInt(decVal / minuteMS);

    if (v > 0) {
        str += v + 'M,';
    }

    decVal = decVal % minuteMS;
    v = parseInt(decVal / 1000);

    if (v > 0) {
        str += v + 'S';
    }

    return str;
};

Date.prototype.format = function (fmt) {
    var o = {
        'M+': this.getMonth() + 1,
        //月份
        'd+': this.getDate(),
        //日
        'h+': this.getHours(),
        //小时
        'm+': this.getMinutes(),
        //分
        's+': this.getSeconds(),
        //秒
        'q+': Math.floor((this.getMonth() + 3) / 3),
        //季度
        S: this.getMilliseconds() //毫秒
    };

    if (/(y+)/.test(fmt)) {
        fmt = fmt.replace(RegExp.$1, (this.getFullYear() + '').substr(4 - RegExp.$1.length));
    }

    for (var k in o) {
        if (new RegExp('(' + k + ')').test(fmt)) {
            fmt = fmt.replace(RegExp.$1, RegExp.$1.length == 1 ? o[k] : ('00' + o[k]).substr(('' + o[k]).length));
        }
    }

    return fmt;
}; //for safari

if (typeof Array.prototype.fill == 'undefined') {
    Array.prototype.fill = function (e) {
        for (var i = 0; i < this.length; i++) {
            this[i] = e;
        }
    };
}

if (typeof Array.prototype.join == 'undefined') {
    Array.prototype.join = function (sep) {
        var rst = '';

        for (var i = 0; i < this.length; i++) {
            rst += this[i] + sep;
        }

        if (rst.length > 0) {
            rst.substring(0, rst.length - 1);
        }

        return rst;
    };
}

if (typeof String.prototype.trim == 'undefined') {
    String.prototype.trim = function () {
        return this.replace(/^\s+|\s+$/g, '');
    };
}

if (typeof String.prototype.leftTrim == 'undefined') {
    String.prototype.leftTrim = function () {
        return this.replace(/^\s*/, '');
    };
}

if (typeof String.prototype.rightTrim == 'undefined') {
    String.prototype.rightTrim = function () {
        return this.replace(/(\s*$)/g, '');
    };
}

let _genId = 0;

export default {

	//角度转换为弧度
	toRadians: function(degree) {
		return degree * Math.PI / 180;
	},

	//计算两个坐标点之间的距离
	distanceByKm : function(long1, lat1, long2, lat2) {
		// 地球的半径（单位：公里）
		var R = 6371;
		//角度转换为弧度
		var deltaLat = this.toRadians(lat2 - lat1);
		var deltaLong = this.toRadians(long2 - long1);
		lat1 = this.toRadians(lat1);
		lat2 = this.toRadians(lat2);
		//计算过程
		var h = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLong /
			2) * Math.sin(deltaLong / 2);
		//求距离
		var d = 2 * R * Math.asin(Math.sqrt(h));
		return d;
	},
	
	readableDis:function(dis) {
		if(dis < 1) {
			return this.toFixed(dis*1000,2)+'米'
		} else {
			return this.toFixed(dis,2)+'公里'
		}
	},

	getCostTime:function(timeLong) {
		if(timeLong == 0) return "0秒"  //0表示没有剩余时间
		
		let dd = (cc) => {return cc<10 ? ('0'+cc) : cc}
		
		let v = '';
		let c = timeLong;
		
		let p = ""
		
		if(c < 0) {
			p = '已过';//负数表示 过去时间
			c = -c
		}
		
		if (c < 1000) {
			//只有毫秒值
			return v + c + '毫秒'
		}
		
		c =  parseInt(c/1000) //毫秒转为秒，忽略毫秒值
		
		if(c < 60) {
			//只有秒数
			return v + dd(c) + '秒'
		}
		v = dd(parseInt(c%60)) + '秒'
		
		c = parseInt(c/60) //转为分钟
		if(c < 60) {
			//只有分钟数
			return p + dd(c) + '分' + v
		}
		v = dd(parseInt(c%60)) + '分' + v
		
		c = parseInt(c/60)//转为小时
		if(c < 24) {
			//只有小时数
			return p + c + '小时' + v
		}
		v = dd(parseInt(c%24)) + '小时' + v
		
		c = parseInt(c/24) //转为天
		v = dd(c) + '天' + v

		return p + v;
	},
	
	toFixed(val, num) {
		if (val) {
			return parseFloat(val).toFixed(num);
		} else {
			return '0'
		}
	},
	
	getSizeVal(size) {
		let v = '';
		if (size < 1024) {
			v = size + 'B';
		} else if (size < 1024 * 1024) {
			v = this.toFixed(size / 1024, 2) + 'KB';
		} else if (size < 1024 * 1024 * 1024) {
			v = this.toFixed(size / (1024 * 1024), 2) + 'MB';
		} else {
			v = this.toFixed(size / (1024 * 1024 * 1024), 2) + 'GB';
		}
		return v;
	},
	
    isUni: function () {
        return typeof uni != 'undefined';
    },
	
	//是否是H5 plus
	isH5Plus: function () {
	    return typeof plus != 'undefined' && typeof plus.android != 'undefined'
		|| typeof plus != 'undefined' && typeof plus.ios != 'undefined';
	},
	
    goTo(url) {
        location.href = url;
    },
    getId() {
        return _genId++;
    },
    getTimeAsMills() {
        return new Date().getTime();
    },
    fnvHash1(msg) {
        let data = msg;

        if (typeof msg != 'string') {
            data = JSON.stringify(msg);
        }

        if (typeof data == 'string') {
            data = this.toUTF8Array(msg);
        }

        let FNV_32_INIT = 2166136261;
        let FNV_32_PRIME = 16777619;
        let rv = FNV_32_INIT;
        let len = data.length;

        for (let i = 0; i < len; i++) {
            rv = rv ^ data[i];
            rv = rv * FNV_32_PRIME;
        }

        return rv;
    },
    strByteLength(str) {
        let i;
        let len;
        len = 0;

        for (i = 0; i < str.length; i++) {
            if (str.charCodeAt(i) > 255) {
                len += 2;
            } else {
                len++;
            }
        }

        return len;
    },
	
	isValidPhone(str) {
	   return  /^[1][3,4,5,7,8,9][0-9]{9}$/.test(str);
	},
	
	isValidNumber(val){
		return /^[+,-]?[0-9]+.?[0-9]*$/.test(val)
	},
	
    isInteger(value) {
       return '/^(+|-)?d+$/'.test(value)
    },
	
    isFloat(value) {
       return '/^(+|-)?d+($|.d+$)/'.test(value)
    },
	
    checkUrl(value) {
        return '/^((http:[/][/])?w+([.]w+|[/]w*)*)?$/'.test(value);
    },
	
    checkEmail(value) {
        return  /^([-_A-Za-z0-9.]+)@([_A-Za-z0-9]+.)+[A-Za-z0-9]{2,3}$/.test(value);
    },
	
	checkMobile(m) {
		return /^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\d{8}$/.test(m)
	},
    
    checkIP(value) {
        let re = '/^(d+).(d+).(d+).(d+)$/';

        if (re.test(value)) {
            if (RegExp.$1 < 256 && RegExp.$2 < 256 && RegExp.$3 < 256 && RegExp.$4 < 256) {
                return true;
            }
        }

        return false;
    },
    inherits(child, parentCtor) {
        function tempCtor() {}

        tempCtor.prototype = parentCtor.prototype;
        child.superClass_ = parentCtor.prototype;
        child.prototype = new tempCtor();
        child.prototype.constructor = child;
    },
    bind(scope, funct) {
        return function () {
            return funct.apply(scope, arguments);
        };
    },
	
    appendUrlParams(url, params) {
        if (params) {
            url = url + '?';

            for (var p in params) {
                url = url + p + '=' + params[p] + '&';
            }

            url = url.substr(0, url.length - 1);
        }

        return url;
    },
	
    parseUrlParams(url1) {
        var url = url1;

        if (!url) {
            url = window.location.href;
        }

        var index = url.indexOf('?');

        if (index < 0) {
            return {};
        }

        url = decodeURI(url);
        var qs = url.substring(index + 1);
        var params = {};
        var arr = qs.split('&');

        for (var i = 0; i < arr.length; i++) {
            var kv = arr[i].split('=');
            params[kv[0]] = kv[1];
        }

        return params;
    },
	
    clone(jsObj) {
        var type = typeof jsObj;

        if (type != 'object') {
            return jsObj;
        }

        var obj = {};

        for (var i in jsObj) {
            var o = jsObj[i];
            obj[i] = this.clone(o);
        }

        return obj;
    },
	
	
    toUTF8Array(str) {
        let utf8 = [];

        for (let i = 0; i < str.length; i++) {
            let charcode = str.charCodeAt(i);

            if (charcode < 128) {
                utf8.push(charcode);
            } else {
                if (charcode < 2048) {
                    utf8.push(192 | (charcode >> 6), 128 | (charcode & 63));
                } else {
                    if (charcode < 55296 || charcode >= 57344) {
                        utf8.push(224 | (charcode >> 12), 128 | ((charcode >> 6) & 63), 128 | (charcode & 63));
                    } // surrogate pair
                    else {
                        i++;
                        charcode = ((charcode & 1023) << 10) | (str.charCodeAt(i) & 1023);
                        utf8.push(240 | (charcode >> 18), 128 | ((charcode >> 12) & 63), 128 | ((charcode >> 6) & 63), 128 | (charcode & 63));
                    }
                }
            }
        }

        return utf8;
    },
	
    fromUTF8Array(data) {
        let str = '';
        let i;
        // array of bytes
        for (i = 0; i < data.length; i++) {
            let value = data[i];

            if (value < 128) {
                str += String.fromCharCode(value);
            } else {
                if (value > 191 && value < 224) {
                    str += String.fromCharCode(((value & 31) << 6) | (data[i + 1] & 63));
                    i += 1;
                } else {
                    if (value > 223 && value < 240) {
                        str += String.fromCharCode(((value & 15) << 12) | ((data[i + 1] & 63) << 6) | (data[i + 2] & 63));
                        i += 2;
                    } else {
                        // surrogate pair
                        var charCode = (((value & 7) << 18) | ((data[i + 1] & 63) << 12) | ((data[i + 2] & 63) << 6) | (data[i + 3] & 63)) - 65536;
                        str += String.fromCharCode((charCode >> 10) | 55296, (charCode & 1023) | 56320);
                        i += 3;
                    }
                }
            }
        }

        return str;
    },
	
    utf8StringTakeLen(str) {
        //
        let le = this.toUTF8Array(str).length;

        if (le < Constants.MAX_BYTE_VALUE) {
            return le + 1;
        } else {
            if (le < Constants.MAX_SHORT_VALUE) {
                return le + 3;
            } else {
                if (le < Constants.MAX_INT_VALUE) {
                    return le + 7;
                } else {
                    throw 'String too long for:' + le;
                }
            }
        }
    },
	
    parseJson(data) {
        if (data instanceof Array) {
            let jsonStr = this.fromUTF8Array(data);
            return JSON.parse(jsonStr);
        } else {
            if (typeof data == 'string') {
                return JSON.parse(data);
            } else {
                if (data instanceof ArrayBuffer) {
                    let byteArray = [];
                    let dv = new DataView(data, data.byteLength);

                    for (let i = 0; i < data.byteLength; i++) {
                        byteArray.push(dv.getUint8(i));
                    }

                    let jsonStr = this.fromUTF8Array(byteArray);
                    return JSON.parse(jsonStr);
                } else {
                    return data;
                }
            }
        }
    },
	
    byteArr2Base64(byteArr) {
        let bstr = ''; //let self = this;

        let ch = function (c) {
            return b64map.charAt(c);
        }; //a          b         c
        //01100001 01100010 01100011
        //011000 010110 001001 100011
        //24       22    9      35
        //Y        W     J      j

        let i = 0;

        for (; i + 3 <= byteArr.length; i += 3) {
            //第一个字节高6位
            bstr += ch(byteArr[i] >>> 2); //第一个字节的低两位  连接第二个字节高4位

            bstr += ch(((byteArr[i] & 3) << 4) | (byteArr[i + 1] >>> 4)); //第二个字节的底4位  连接第3个字节高2位

            bstr += ch(((byteArr[i + 1] & 15) << 2) | (byteArr[i + 2] >> 6)); //第3个字节的底6位

            bstr += ch(byteArr[i + 2] & 63);
        }

        let mv = byteArr.length % 3;

        if (mv == 1) {
            let arr = [byteArr[byteArr.length - 1], 0, 0];
            bstr += ch(arr[0] >>> 2); //第一个字节的高两位  连接第二个字节底4位

            bstr += ch(((arr[0] & 3) << 4) | (arr[1] >>> 4));
            bstr += '==';
        }

        if (mv == 2) {
            //a          b
            //01100001 01100010
            //011000 010110 0010 00
            //24       22    9      35
            //Y        W     J      j
            let arr = [byteArr[byteArr.length - 2], byteArr[byteArr.length - 1], 0];
            bstr += ch(arr[0] >>> 2); //第一个字节的高两位  连接第二个字节底4位

            bstr += ch(((arr[0] & 3) << 4) | (arr[1] >>> 4));
            bstr += ch(((arr[1] & 15) << 2) | (arr[3] >> 6));
            bstr += '=';
        }

        return bstr;
    },
	
    base642ByteArr: function (base64) {
        let b = [];

        if (base64.length % 4 != 0) {
            throw 'Invalid base64 format!';
        } //let self = this;

        let ch = function (i) {
            return b64map.indexOf(base64.charAt(i));
        }; //a          b         c
        //01100001 01100010 01100011
        //011000 010110 001001 100011
        //24       22    9      35
        //Y        W     J      j

        let blen = base64.length;

        for (let i = 0; i <= blen; i += 4) {
            if (base64.charAt(i) == '=') {
                break;
            }

            let c0 = ch(i);
            let c1 = ch(i + 1);
            let c2 = ch(i + 2);
            let c3 = ch(i + 3); //011000 010110 001001 100011
            //01000000
            //00100011
            //01100011

            b.push((c0 << 2) | (c1 >>> 4));
            b.push(((c1 & 15) << 4) | ((c2 >>> 2) & 15));
            b.push(((c2 & 3) << 6) | c3);
        }

        return b;
    },
	
	byteArray2ArrayBuffer: function(b) {
		if(!b || b.length == 0) return new ArrayBuffer(0)
		let ab = new ArrayBuffer(b.length)
		let vw = new DataView(ab)
		for(let i = 0; i < b.length; i++) {
			vw.setInt8(i,b[i]);
		}
		return ab
	},
	
    flagIs: function (flag, mask) {
        return (flag & mask) != 0;
    },
	
    flagSet: function (isTrue, f, mask) {
        return isTrue ? (f |= mask) : (f &= ~mask);
    }
};
