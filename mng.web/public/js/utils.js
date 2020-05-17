var jm = jm || {};

jm.goTo = function(url) {
    location.href = url;
}

String.prototype.startWith=function(str){
    var reg=new RegExp("^"+str);
    return reg.test(this);
}

String.prototype.endWith=function(str){
    var reg=new RegExp(str+"$");
    return reg.test(this);
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

if(typeof String.prototype.trim == 'undefined') {
    String.prototype.trim = function(e) {
        return str.replace(/^\s+|\s+$/g,"");
    }
}

if(typeof String.prototype.trim == 'undefined') {
    String.prototype.trim = function(e) {
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

jm.http = {

    getQueryParam : function(name) {
        if(location.href.indexOf("?")==-1 || location.href.indexOf(name+'=')==-1){
            return '';
        }
        var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
        var r = window.location.search.substr(1).match(reg);
        if (r != null)
            return decodeURI(r[2]);   //对参数进行decodeURI解码
        return null;
    },

    get: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'GET', errCb, fullpath)
    },

    post: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'POST', errCb, fullpath)
    },

    put: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'PUT', errCb, fullpath)
    },

    ajax: function (path, params, sucCb, async, method, errCb, fullpath) {
        if (!sucCb) {
            throw "Callback method cannot be null";
        }

        if (!method) {
            throw "request method cannot be null";
        }

        if (!path) {
            path = "/";
        }

        this._doAjax(path, params, sucCb, async, method, errCb, fullpath);
    }

    ,_doAjax: function(path, params, sucCb, async, method, errCb, fullpath) {
        /* if(!jm.urls.checkNetwork()) {
         throw '网络连接不可用';
         }*/
        if(fullpath) {
            path = fullpath;
        } else if (!(path.startWith('http://') &&  !path.startWith('https://'))){
            path = this.getWebResourceHttpPath(path);
        }
        if(!params) {
            params = {};
        }

        $.ajax({
            data: params,
            type: method,
            async: true,
            url: path,
            jsonp: 'json',
            success: function (data, statuCode, xhr) {
                sucCb(data, statuCode, xhr);
            },
            beforeSend: function (xhr) {
                //xhr.setRequestHeader('Access-Control-Allow-Headers','*');
                /*if (jm.zdd.uc.isLogin()) {
                    xhr.setRequestHeader("loginKey", params.loginKey);
                }*/
            },
            error: function (err, xhr) {
                if (errCb) {
                    errCb(err, xhr);
                } else {
                    sucCb(null, err, xhr);
                }
            }
        });
    }

    ,uploadBase64Picture: function (data, ptype, cb,pos) {
        var params = {
            encode: 'base64',
            imageType: ptype,
            name: 'base64_picture_file' + jm.utils.getId(),
            contentType: 'image/' + ptype + ';base64',
            base64Data: data,
            pos:pos
        }
        var url = this.getWebResourceHttpPath(jm.http.UPLOAD_BASE64);
        jm.http.post(url, params, function (data1, status, xhr) {
            console.log(data1);
            if (status !== 'success') {
                cb(status, data1);
            } else {
                cb(null, data1);
            }
        }, function (err, xhr) {
            console.log(err);
            cb(err, null);
        })
    }

    ,postHelper: function(url,params,cb){
        jm.http.post(url,params, function(data,status,xhr){
            if(data.success) {
                cb(null,data);
            } else {
                cb(data,data);
            }
        },function(err){
            cb(err,null);
        });
    }

    //get: function (path, params, cb, errCb, fullpath)
    ,getHelper: function(url,params,cb,fullpath){
        if(!params) {
            params = {};
        }
        this.get(url,params, function(data,status,xhr){
            cb(null,data);
        },function(err){
            cb(err,null);
        },fullpath);
    }

    ,getUrl: function(url) {
        return this.getWebResourceHttpPath(url);
    },

    getWebContextPath : function() {
        var pathname = location.pathname
        pathname = pathname.substring(1,pathname.length);
        pathname = pathname.substring(0,pathname.indexOf('/'));
        return '/'+pathname;
    },

    getWSApiPath : function() {
        var wp = 'ws://' + location.host+ + jm.config.wsContext;
        return wp;
    },

    getHttpApiPath : function() {
        var wp = 'http://' + location.host + jm.config.httpContext;
        return wp;
    },

    getWebResourceHttpPath : function(subPath) {
        var wp = 'http://' + location.host + jm.utils.getWebContextPath()
            + subPath;
        return wp;
    }
}

jm.utils = {
    _genId : 0,
    getId: function(){
        return this._genId ++;
    },

    getTimeAsMills: function() {
        return new Date().getTime();
    },

    strByteLength:  function(str)  {
        var i;
        var len;
        len = 0;
        for (i=0;i<str.length;i++)  {
            if (str.charCodeAt(i)>255) len+=2; else len++;
        }
        return len;
    },

    isInteger: function (value)  {
        if ('/^(\+|-)?\d+$/'.test(value )){
            return true;
        }else {
            return false;
        }
    },

    isFloat: function(value){
        if ('/^(\+|-)?\d+($|\.\d+$)/'.test(value )){
            return true;
        }else{
            return false;
        }
    } ,

    checkUrl: function (value){
        var myReg = '/^((http:[/][/])?\w+([.]\w+|[/]\w*)*)?$/';
        return myReg.test( value );
    },

    checkEmail: function (value){
        var myReg = /^([-_A-Za-z0-9\.]+)@([_A-Za-z0-9]+\.)+[A-Za-z0-9]{2,3}$/;
        return myReg.test(value);
    },

    checkIP:   function (value)   {
        var re='/^(\d+)\.(\d+)\.(\d+)\.(\d+)$/';
        if(re.test( value ))  {
            if( RegExp.$1 <256 && RegExp.$2<256 && RegExp.$3<256 && RegExp.$4<256)
                return true;
        }
        return false;
    },

    inherits : function(child, parentCtor) {
        function tempCtor() {};
        tempCtor.prototype = parentCtor.prototype;
        child.superClass_ = parentCtor.prototype;
        child.prototype = new tempCtor();
        child.prototype.constructor = child;
    },

    bind : function(scope, funct){
        return function(){
            return funct.apply(scope, arguments);
        };
    },

    appendUrlParams:function(url,params) {
        if(params) {
            url = url + '?';
            for(var p in params) {
                url = url + p + '=' + params[p]+'&';
            }
            url = url.substr(0, url.length - 1);
        }
        return url;
    },

    parseUrlParams:function(url1) {
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

    isBrowser : function(browser) {
        return jm.utils.browser[browser] != null;
    },

    version : function() {
        for(var b in jm.utils.browser) {
            return jm.utils.browser[b][1].split('.')[0];
        }
    },

    isIe : function() {
        return this.isBrowser(jm.utils.Constants.IE9) ||
            this.isBrowser(jm.utils.Constants.IE8) ||
            this.isBrowser(jm.utils.Constants.IE7) ||
            this.isBrowser(jm.utils.Constants.IE6)
    },

    toJson : function(obj) {
        if(typeof obj === 'string') {
            return obj;
        } else if(typeof obj === 'object') {
            return JSON.stringify(obj);
        }else {
            throw 'obj cannot transfor to Json'
        }
    },

    fromJson : function(jsonStr) {
        console.log(typeof jsonStr);
        if(typeof jsonStr === 'string') {
            var msg = eval('('+jsonStr+')');
            if(msg.status){
                msg.status = eval('('+msg.status+')');
            }
            return new jm.Message(msg);
        }else if(typeof jsonStr === 'object') {
            return jsonStr;
        } else  {
            throw 'fail from Json: ' + jsonStr;
        }

    },

    fromJ : function(jsonStr) {
        if(typeof jsonStr === 'string') {
            var msg = eval('('+jsonStr+')');
            if(msg.status){
                msg.status = eval('('+msg.status+')');
            }
            return msg;
        }else if(typeof jsonStr === 'object') {
            return jsonStr;
        } else  {
            throw 'fail from Json: ' + jsonStr;
        }

    },

    clone : function(jsObj) {
        var type = typeof jsObj;
        if(type != 'object') {
            return jsObj;
        }
        var obj = {};
        for(var i in jsObj) {
            var o = jsObj[i];
            obj[i] = jm.utils.clone(o);
        }
        return obj;
    },

    isLoad: function(jsUrl) {
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

    load: function(jsUrl) {
        if(this.isLoad(jsUrl)) {
            return ;
        }
        var oHead = document.getElementsByTagName('HEAD').item(0);
        var oScript= document.createElement("script");
        oScript.type = "text/javascript";
        oScript.src=jsUrl;
        oHead.appendChild( oScript);
    },

     toUTF8Array : function(str) {
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

    fromUTF8Array : function(data) { // array of bytes
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

    utf8StringTakeLen : function(str) { //
        let le = this.toUTF8Array(str).len;
        if(le < jm.rpc.Constants.MAX_BYTE_VALUE) {
            return le +1;
        }else if(le < jm.rpc.Constants.MAX_SHORT_VALUE) {
            return le +3;
        }else if(le < jm.rpc.Constants.MAX_INT_VALUE) {
            return le +7;
        }else {
            throw "String too long for:" + le;
        }
    },

    parseJson:function(data) {
        if(data instanceof Array) {
            let  jsonStr = this.fromUTF8Array(data);
            return JSON.parse(jsonStr);
        }else if(typeof data == 'string') {
            return JSON.parse(data);
        }else if(data instanceof ArrayBuffer){
            let byteArray = [];
            let dv = new DataView(data,data.byteLength);
            for(let i = 0; i <data.byteLength; i++) {
                byteArray.push(dv.getUint8());
            }
            let jsonStr = this.fromUTF8Array(byteArray);
            return JSON.parse(data);
        }else {
            return data;
        }


    }

}

jm.localStorage = new function() {
    //this.updateBrowser_ = jm.utils.i18n.get('update_your_browser');
    this.set = function(key,value) {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        localStorage.setItem(key,value);
    }

    this.remove = function(key) {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        localStorage.removeItem(key);
    }

    this.get = function(key) {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        return localStorage.getItem(key);
    }

    this.clear = function() {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        if(confirm('this operation will clear all local storage data?')) {
            localStorage.clear();
        }
    }

    this.supportLocalstorage = function() {
        try {
            return window.localStorage !== null;
        } catch (e) {
            return false;
        }
    }

    this.isSupport = function() {
        if(jm.utils.isIe() && jm.utils.version() < 8) {
            return false;
        }else {
            return true;
        }
    }

}

jm.sessionStorage = new function() {
    //this.updateBrowser_ = jm.utils.i18n.get('update_your_browser');
    this.set = function(key,value) {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        sessionStorage.setItem(key,value);
    }

    this.remove = function(key) {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        sessionStorage.removeItem(key);
    }

    this.get = function(key) {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        return sessionStorage.getItem(key);
    }

    this.clear = function() {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        sessionStorage.clear();
    }

    this.supportLocalstorage = function() {
        try {
            return window.sessionStorage !== null;
        } catch (e) {
            return false;
        }
    }
}

jm.utils.Constants={
    // debug level
    INFO:'INFO',
    DEBUG:'DEBUG',
    ERROR:'ERROR',
    FINAL:'FINAL',
    DEFAULT:'DEFAULT',
    IE:'ie',
    IE6:'ie6',
    IE7:'ie7',
    IE8:'ie8',
    IE9:'ie9',
    IE10:'ie10',
    IE10:'ie11',
    chrome:'chrome',
    firefox:'firefox',
    safari:'safari',
    opera:'opera'
}

if(!jm.utils.browser) {
    var ua = navigator.userAgent.toLowerCase();
    var s = null;
    var key = null;
    var bv = null;
    //"mozilla/5.0 (windows nt 10.0; wow64; trident/7.0; .net4.0c; .net4.0e; manmjs; rv:11.0) like gecko"
    if(s = ua.match(/msie ([\d.]+)/)) {
        key = jm.utils.Constants.IE;
        key = key + s[1].split('.')[0];
    }else if(s = ua.match(/rv\:([\d.]+)/)) {
        key = jm.utils.Constants.IE
    }else if(s = ua.match(/firefox\/([\d.]+)/)) {
        key = jm.utils.Constants.firefox
    }else if(s = ua.match(/chrome\/([\d.]+)/)) {
        key = jm.utils.Constants.chrome;
    }else if(s = ua.match(/opera.([\d.]+)/)) {
        key = jm.utils.Constants.opera;
    }else if(s = ua.match(/version\/([\d.]+).*safari/)) {
        key = jm.utils.Constants.safari;
    }
    jm.utils.browser = {};
    if(s != null) {
        jm.utils.browser[key] = [];
        jm.utils.browser[key][0] = s[0].trim();
        jm.utils.browser[key][1] = s[1].trim();
    }
}

if(jm.utils.isBrowser('ie')) {
    Array.prototype.fill = function(e) {
        for(let i = 0; i < this.length; i++) {
            this[i] = e;
        }
    }
}

jm.utils.JDataInput = function(buf) {
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
jm.utils.JDataInput.prototype.readUnsignedShort = function() {
    let firstByte = this.atByte();
    let secondByte = this.atByte();
    let anUnsignedShort  =  firstByte << 8 | secondByte;
    return anUnsignedShort;
},

jm.utils.JDataInput.prototype.readInt = function() {
    let firstByte = this.atByte();
    let secondByte = this.atByte();
    let thirdByte = this.atByte();
    let fourthByte = this.atByte();
    let anUnsignedInt  = ((firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte)) & 0xFFFFFFFF;
    return anUnsignedInt;
},

//public static long
    jm.utils.JDataInput.prototype.readUnsignedInt = function() {
        /*let firstByte = this.atByte();
        let secondByte = this.atByte();
        let thirdByte = this.atByte();
        let fourthByte = this.atByte();
        let anUnsignedInt  = ((firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte)) & 0xFFFFFFFF;
        return anUnsignedInt;*/
        let b = this.getUByte() & 0xff;
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
        return (n >>> 1) ^ -(n & 1); // back to two's-complement
    },

    jm.utils.JDataInput.prototype.getUByte = function() {
        return this.buf.getUint8(this.readPos++);
    }

jm.utils.JDataInput.prototype.atByte = function() {
    return 0xFF & this.getUByte();
}

jm.utils.JDataInput.prototype.remaining = function() {
    return this.buf.byteLength - this.readPos;
}

//public static long
jm.utils.JDataInput.prototype.readUnsignedLong = function() {
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
            fiveByte << 24 | sixByte << 16 | sevenByte << 8 | eightByte) & 0xFFFFFFFFFFFFFFFF;
    return anUnsignedLong;
},

    jm.utils.JDataInput.prototype.readUtf8String = function() {
        let len = this.getUByte();

        if(len == -1) {
            return null;
        }else if(len == 0) {
            return "";
        }

        //Byte.MAX_VALUE
        if(len == jm.rpc.Constants.MAX_BYTE_VALUE) {
            len = this.readUnsignedShort();
            //Short.MAX_VALUE
            if(len == jm.rpc.Constants.MAX_SHORT_VALUE) {
                len = this.readUnsignedInt();
            }
        }

        let arr = [];
        for(let i = 0; i < len; i++) {
            arr.push(this.getUByte());
        }

        return jm.utils.fromUTF8Array(arr);
    }


jm.utils.JDataOutput = function(buf) {
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

jm.utils.JDataOutput.prototype.getBuf = function() {
    return this._buf.slice(0,this.writePos);
}

jm.utils.JDataOutput.prototype.writeUByte = function(v) {
    this.buf.setUint8(this.writePos++,v);
}

jm.utils.JDataOutput.prototype.remaining = function() {
    return this._buf.byteLength - this.writePos;
}

jm.utils.JDataOutput.prototype.checkCapacity = function(len) {
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
jm.utils.JDataOutput.prototype.writeUnsignedShort = function(v) {
    if(v > jm.rpc.Constants.MAX_SHORT_VALUE) {
        throw "Max short value is :"+jm.rpc.Constants.MAX_SHORT_VALUE+", but value "+v;
    }
    this.checkCapacity(2);
    this.writeUByte((v >>> 8) & 0xFF);
    this.writeUByte((v >>> 0) & 0xFF);
},

//public static void
jm.utils.JDataOutput.prototype.writeUnsignedByte = function(v) {
    if(v > jm.rpc.Constants.MAX_BYTE_VALUE) {
        throw "Max byte value is :"+jm.rpc.Constants.MAX_BYTE_VALUE+", but value "+v;
    }
    this.checkCapacity(1);
    this.writeUByte(v);
},

    jm.utils.JDataOutput.prototype.writeInt = function(v) {
        if(v > jm.rpc.Constants.MAX_INT_VALUE) {
            throw "Max int value is :"+jm.rpc.Constants.MAX_INT_VALUE+", but value "+v;
        }
        this.checkCapacity(4);
        //高字节底地址
        this.writeUByte((v >>> 24)&0xFF);
        this.writeUByte((v >>> 16)&0xFF);
        this.writeUByte((v >>> 8)&0xFF);
        this.writeUByte((v >>> 0)&0xFF);

    }

//public static void
jm.utils.JDataOutput.prototype.writeUnsignedInt = function(n) {
    if(n > jm.rpc.Constants.MAX_INT_VALUE) {
        throw "Max int value is :"+jm.rpc.Constants.MAX_INT_VALUE+", but value "+v;
    }
    this.checkCapacity(4);
    n = (n << 1) ^ (n >> 31);
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
    this.writeUByte(n);

}

jm.utils.JDataOutput.prototype.writeUnsignedLong = function(v) {
    if(v > jm.rpc.Constants.MAX_INT_VALUE) {
        throw "Max int value is :"+jm.rpc.Constants.MAX_INT_VALUE+", but value "+v;
    }
    this.checkCapacity(8);
    this.writeUByte((v >>> 56)&0xFF);
    this.writeUByte((v >>> 48)&0xFF);
    this.writeUByte((v >>> 40)&0xFF);
    this.writeUByte((v >>> 32)&0xFF);
    this.writeUByte((v >>> 24)&0xFF);
    this.writeUByte((v >>> 16)&0xFF);
    this.writeUByte((v >>> 8)&0xFF);
    this.writeUByte((v >>> 0)&0xFF);
}

jm.utils.JDataOutput.prototype.writeByteArray = function(arr) {
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

jm.utils.JDataOutput.prototype.writeArrayBuffer = function(ab) {
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

jm.utils.JDataOutput.prototype.writeUtf8String = function(s) {

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
    let data = jm.utils.toUTF8Array(s);

    let le = data.length;
    let needLen = le;
    if(le < jm.rpc.Constants.MAX_BYTE_VALUE) {
        needLen = le +1;
        self.checkCapacity(needLen);
        self.writeUByte(le);
    }else if(le < jm.rpc.Constants.MAX_SHORT_VALUE) {
        needLen = le +3;
        this.checkCapacity(needLen);
        self.writeUByte(jm.rpc.Constants.MAX_BYTE_VALUE);
        self.writeUnsignedShort(le);
    }else if(le < jm.rpc.Constants.MAX_INT_VALUE) {
        needLen = le +7;
        self.checkCapacity(needLen);
        self.writeUByte(jm.rpc.Constants.MAX_BYTE_VALUE);
        self.writeUnsignedShort(jm.rpc.Constants.MAX_SHORT_VALUE);
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

jm.utils.JDataOutput.prototype.writeObject = function(obj) {
    let len = 0;
    for(let key in obj) {
        len++;
    }

    this.writeUnsignedInt(len);
    for(let key in obj) {
        //全部写为字符串
        this.writeUtf8String(key+"");
        this.writeUtf8String(obj[key]+"");
    }

}

jm.utils.JDataOutput.prototype.writeObjectArray = function(arr) {
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

