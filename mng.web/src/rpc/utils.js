/* eslint-disable */
//import cons from "./constants";
import {Constants} from "./message"

const b64map = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

let wxs = null

if(typeof wx == 'object') wxs = wx

String.prototype.startWith=function(str){
    var reg=new RegExp("^"+str);
    return reg.test(this);
}

String.prototype.endWith=function(str){
    var reg=new RegExp(str+"$");
    return reg.test(this);
}

Date.prototype.toDecDay = function() {
    let curDate = new Date();
    let decVal = curDate.getTime() - this.getTime();
    if(decVal <= 0) {
        return '0';
    }

    let minuteMS = 60*1000;
    let hourMS = 60*minuteMS;
    let dateMS = 24*hourMS;

    let str = '';

    let v = parseInt(decVal/dateMS);
    if(v > 0) {
        str += v + 'D,';
    }

    decVal = decVal % dateMS;
    v = parseInt(decVal/hourMS);
    if(v > 0) {
        str += v + 'H,';
    }

    decVal = decVal % hourMS;
    v = parseInt(decVal/minuteMS);
    if(v > 0) {
        str += v + 'M,';
    }

    decVal = decVal % minuteMS;
    v = parseInt(decVal/1000);
    if(v > 0) {
        str += v + 'S';
    }

    return str;

}

Date.prototype.format = function(fmt) {
    var o = {
        "M+" : this.getMonth()+1,                 //月份
        "d+" : this.getDate(),                    //日
        "h+" : this.getHours(),                   //小时
        "m+" : this.getMinutes(),                 //分
        "s+" : this.getSeconds(),                 //秒
        "q+" : Math.floor((this.getMonth()+3)/3), //季度
        "S"  : this.getMilliseconds()             //毫秒
    };
    if(/(y+)/.test(fmt)) {
        fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));
    }
    for(var k in o) {
        if(new RegExp("("+ k +")").test(fmt)){
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));
        }
    }
    return fmt;
}

//for safari
if(typeof Array.prototype.fill == 'undefined') {
    Array.prototype.fill = function(e) {
        for(var i = 0; i < this.length; i++) {
            this[i] = e;
        }
    }
}

if(typeof Array.prototype.join == 'undefined') {
    Array.prototype.join = function(sep) {
        var rst = '';
        for(var i = 0; i < this.length; i++) {
            rst += this[i] + sep;
        }
        if(rst.length > 0) {
            rst.substring(0, rst.length-1);
        }
        return rst;
    }
}

if(typeof String.prototype.trim == 'undefined') {
    String.prototype.trim = function() {
        return this.replace(/^\s+|\s+$/g,"");
    }
}


if(typeof String.prototype.leftTrim == 'undefined') {
    String.prototype.leftTrim = function() {
        return this.replace( /^\s*/, '');
    }
}

if(typeof String.prototype.rightTrim == 'undefined') {
    String.prototype.rightTrim = function() {
        return this.replace(/(\s*$)/g, "");
    }
}

let _genId = 0;

export  default  {

    isWx : function(){
        return wxs != null
    },

    goTo(url) {
        location.href = url;
    },

    getId(){
        return _genId ++;
    },

    getTimeAsMills() {
        return new Date().getTime();
    },

    fnvHash1(msg) {
        let data = msg;
        if(typeof msg != 'string') {
            data = JSON.stringify(msg);
        }

        if(typeof data == 'string' ) {
            data = this.toUTF8Array(msg);
        }

        let FNV_32_INIT = 0x811c9dc5;
        let FNV_32_PRIME = 0x01000193;

        let rv = FNV_32_INIT;
        let len = data.length;
        for (let i = 0; i < len; i++) {
            rv = (rv ^ data[i]);
            rv = (rv * FNV_32_PRIME);
        }
        return rv;
    },

    strByteLength(str)  {
        let i;
        let len;
        len = 0;
        for (i=0;i<str.length;i++)  {
            if (str.charCodeAt(i)>255) len+=2; else len++;
        }
        return len;
    },

    isInteger (value)  {
        if ('/^(+|-)?d+$/'.test(value )){
            return true;
        }else {
            return false;
        }
    },

    isFloat(value){
        if ('/^(+|-)?d+($|.d+$)/'.test(value )){
            return true;
        }else{
            return false;
        }
    } ,

    checkUrl (value){
        let myReg = '/^((http:[/][/])?w+([.]w+|[/]w*)*)?$/';
        return myReg.test( value );
    },

    checkEmail (value){
        let myReg = /^([-_A-Za-z0-9.]+)@([_A-Za-z0-9]+.)+[A-Za-z0-9]{2,3}$/;
        return myReg.test(value);
    },

    checkMobile (mobile){
        return /^1(3|4|5|6|7|8|9)d{9}$/.test(mobile);
    },

    checkIP (value)   {
        let re='/^(d+).(d+).(d+).(d+)$/';
        if(re.test( value ))  {
            if( RegExp.$1 <256 && RegExp.$2<256 && RegExp.$3<256 && RegExp.$4<256)
                return true;
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

    bind(scope, funct){
        return function(){
            return funct.apply(scope, arguments);
        };
    },

    appendUrlParams(url,params) {
        if(params) {
            url = url + '?';
            for(var p in params) {
                url = url + p + '=' + params[p]+'&';
            }
            url = url.substr(0, url.length - 1);
        }
        return url;
    },

    parseUrlParams(url1) {
        var url = url1;
        if(!url) {
            url = window.location.href;
        }
        var index = url.indexOf('?');

        if(index < 0) {
            return {};
        }

        url = decodeURI(url);
        var qs = url.substring(index+1);

        var params = {};
        var arr = qs.split('&');
        for(var i =0; i < arr.length; i++) {
            var kv = arr[i].split('=');
            params[kv[0]]=kv[1];
        }

        return params;
    },

    clone(jsObj) {
        var type = typeof jsObj;
        if(type != 'object') {
            return jsObj;
        }
        var obj = {};
        for(var i in jsObj) {
            var o = jsObj[i];
            obj[i] = this.clone(o);
        }
        return obj;
    },

    isLoad(jsUrl) {
        var scripts = document.getElementsByTagName('script');
        if(!scripts || scripts.length < 1) {
            return false;
        }
        for(var i = 0; i < scripts.length; i++) {
            if(jsUrl == scripts[i].src ) {
                return true;
            }
        }
        return false;
    },

    load(jsUrl) {
        if(this.isLoad(jsUrl)) {
            return ;
        }
        var oHead = document.getElementsByTagName('HEAD').item(0);
        var oScript= document.createElement("script");
        oScript.type = "text/javascript";
        oScript.src=jsUrl;
        oHead.appendChild( oScript);
    },

    toUTF8Array(str) {
        let utf8 = [];
        for (let i=0; i < str.length; i++) {
            let charcode = str.charCodeAt(i);
            if (charcode < 0x80) utf8.push(charcode);
            else if (charcode < 0x800) {
                utf8.push(0xc0 | (charcode >> 6),
                    0x80 | (charcode & 0x3f));
            }
            else if (charcode < 0xd800 || charcode >= 0xe000) {
                utf8.push(0xe0 | (charcode >> 12),
                    0x80 | ((charcode>>6) & 0x3f),
                    0x80 | (charcode & 0x3f));
            }
            // surrogate pair
            else {
                i++;
                charcode = ((charcode&0x3ff)<<10)|(str.charCodeAt(i)&0x3ff)
                utf8.push(0xf0 | (charcode >>18),
                    0x80 | ((charcode>>12) & 0x3f),
                    0x80 | ((charcode>>6) & 0x3f),
                    0x80 | (charcode & 0x3f));
            }
        }
        return utf8;
    },

    fromUTF8Array(data) { // array of bytes
        let str = '',
            i;

        for (i = 0; i < data.length; i++) {
            let value = data[i];

            if (value < 0x80) {
                str += String.fromCharCode(value);
            } else if (value > 0xBF && value < 0xE0) {
                str += String.fromCharCode((value & 0x1F) << 6 | data[i + 1] & 0x3F);
                i += 1;
            } else if (value > 0xDF && value < 0xF0) {
                str += String.fromCharCode((value & 0x0F) << 12 | (data[i + 1] & 0x3F) << 6 | data[i + 2] & 0x3F);
                i += 2;
            } else {
                // surrogate pair
                var charCode = ((value & 0x07) << 18 | (data[i + 1] & 0x3F) << 12 | (data[i + 2] & 0x3F) << 6 | data[i + 3] & 0x3F) - 0x010000;

                str += String.fromCharCode(charCode >> 10 | 0xD800, charCode & 0x03FF | 0xDC00);
                i += 3;
            }
        }

        return str;
    },

    utf8StringTakeLen(str) { //
        let le = this.toUTF8Array(str).length;
        if(le < Constants.MAX_BYTE_VALUE) {
            return le +1;
        }else if(le < Constants.MAX_SHORT_VALUE) {
            return le +3;
        }else if(le < Constants.MAX_INT_VALUE) {
            return le +7;
        }else {
            throw "String too long for:" + le;
        }
    },

    parseJson(data) {
        if(data instanceof Array) {
            let  jsonStr = this.fromUTF8Array(data);
            return JSON.parse(jsonStr);
        }else if(typeof data == 'string') {
            return JSON.parse(data);
        }else if(data instanceof ArrayBuffer){
            let byteArray = [];
            let dv = new DataView(data,data.byteLength);
            for(let i = 0; i <data.byteLength; i++) {
                byteArray.push(dv.getUint8(i));
            }
            let jsonStr = this.fromUTF8Array(byteArray);
            return JSON.parse(jsonStr);
        }else {
            return data;
        }
    },



    byteArr2Base64(byteArr) {
        let bstr = '';
        //let self = this;
        let ch = function (c) {
            return b64map.charAt(c);
        }

        //a          b         c
        //01100001 01100010 01100011
        //011000 010110 001001 100011
        //24       22    9      35
        //Y        W     J      j
        let i = 0;
        for(; i+3 <= byteArr.length; i+=3 ) {
            //第一个字节高6位
            bstr += ch(byteArr[i]>>>2);
            //第一个字节的低两位  连接第二个字节高4位
            bstr += ch(((byteArr[i] & 0x03) << 4) | (byteArr[i+1] >>> 4));
            //第二个字节的底4位  连接第3个字节高2位
            bstr += ch(((byteArr[i+1] & 0x0F)<<2) | (byteArr[i+2] >>6));
            //第3个字节的底6位
            bstr += ch(byteArr[i+2] & 0X3F);
        }

        let mv = byteArr.length % 3;
        if(mv == 1) {
            let arr = [byteArr[byteArr.length-1],0,0];
            bstr += ch(arr[0] >>>2 );
            //第一个字节的高两位  连接第二个字节底4位
            bstr += ch(((arr[0] & 0x03) << 4) | (arr[1] >>> 4));
            bstr += '==';
        }

        if(mv == 2) {
            //a          b
            //01100001 01100010
            //011000 010110 0010 00
            //24       22    9      35
            //Y        W     J      j

            let arr = [byteArr[byteArr.length-2],byteArr[byteArr.length-1],0];
            bstr += ch(arr[0] >>>2 );
            //第一个字节的高两位  连接第二个字节底4位
            bstr += ch(((arr[0] & 0x03) << 4) | (arr[1] >>> 4));
            bstr += ch(((arr[1] & 0x0F)<<2) | (arr[3] >>6));

            bstr += '=';
        }
        return bstr;
    },

    base642ByteArr: function(base64) {
        let b = [];
        if(base64.length % 4 != 0) {
            throw 'Invalid base64 format!';
        }

        //let self = this;
        let ch = function (i) {
            return b64map.indexOf(base64.charAt(i));
        }

        //a          b         c
        //01100001 01100010 01100011
        //011000 010110 001001 100011
        //24       22    9      35
        //Y        W     J      j

        let blen = base64.length;
        for(let i = 0; i <= blen; i += 4 ) {

            if(base64.charAt(i) == '=') {
                break;
            }

            let c0 = ch(i);
            let c1 = ch(i+1);
            let c2 = ch(i+2);
            let c3 = ch(i+3);

            //011000 010110 001001 100011
            //01000000
            //00100011
            //01100011
            b.push(c0<<2 | c1 >>> 4);
            b.push((c1 & 0x0F) << 4 | (c2 >>> 2) & 0x0F);
            b.push(((c2 & 0x03) << 6) | c3);

        }
        return b;
    },

    flagIs : function( flag,  mask) {
        return (flag & mask) != 0;
    },

    flagSet : function( isTrue, f, mask) {
        return isTrue ?(f |= mask):(f &= ~mask);
    }


}