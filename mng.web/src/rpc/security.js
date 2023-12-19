/* eslint-disable */
//import JSEncrypt from './jsencrypt/lib/index';
//import CryptoJS from './crypto-js/index';

let CryptoJS;
let JSEncrypt;

import utils from './utils.js';
import config from './config.js';

let cfg = {
    sslEnable: config.sslEnable,
    publicKey: config.publicKey,
    privateKey: config.privateKey,
    keySize: 128,
    pwd_table_len: 512,
    pwdTable: null,
    rsae: null,
    rsad: null,
    wpwd: null,
    pwd: null,
    lastUpdatePwdTime: new Date().getTime(),
    secretVersion: false,
    hexStr: '123456789ABCDEF',
    b64map: 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/',
    b64pad: '=',
    BI_RM: '0123456789abcdefghijklmnopqrstuvwxyz'
};

export default {
    data() {
        return {};
    },
    init: function () {
        if(!cfg.sslEnable) {
            return
        }

        CryptoJS = require('./crypto-js/index')
        JSEncrypt = require('./jsencrypt/lib/index')

        if (cfg.pwdTable) {
            return;
        }

        cfg.pwdTable = [];

        for (let i = 0; i < cfg.pwd_table_len; i++) {
            let rv = parseInt(Math.random() * 85 + 33);
            cfg.pwdTable.push(String.fromCharCode(rv));
        }

        let eopt = {
            log: true,
            default_key_size: 1024
        };

        let encrypt = new JSEncrypt(eopt);
        encrypt.setPublicKey(cfg.publicKey);
        cfg.rsae = encrypt;
        /*  let dopt = {log:true, default_key_size:1024}
        let decrypt = new JSEncrypt(dopt);
        decrypt.setPrivateKey(jm.config.privateKey);
        this.rsad = decrypt;*/
    },

    encrypt: function (msg) {
        if(!cfg.sslEnable) {
            return
        }

        if (!cfg.pwdTable) {
            this.init();
        }

        msg.setUpSsl(true);
        msg.setDownSsl(true);
        msg.setEncType(false);
        let opts = {
            mode: CryptoJS.mode.CBC,
            padding: CryptoJS.pad.Pkcs7,
            keySize: cfg.keySize,
            iv: null,
            salt: null
        };
        let iv = this.genStrPwd(16); //通过密码表生成16个字节的动态偏移量

        opts.iv = CryptoJS.enc.Utf8.parse(iv); //将偏移量转为UTF8编码，服务端使用时也要相应地使用utf8字节数组

        let salt = utils.toUTF8Array(iv); //将偏移量和数据一起发送给服务端，注意是utf8字节编码

        msg.setSaltData(salt);

        if (!cfg.pwd || new Date().getTime() - cfg.lastUpdatePwdTime > 1000 * 60 * 5) {
            //首次进来或超过5分钟更新一次密码
            //生成密码，方式和IV相同，但是功能不一样，参考前面关于密码表的说明
            if (cfg.pwd) {
                cfg.secretVersion = !cfg.secretVersion;
            }

            msg.setSecretVersion(cfg.secretVersion);
            cfg.pwd = this.genStrPwd(16); //告诉服务端，有AES密码更新

            msg.setSec(true); //对AES密码做RSA加密

            msg.setSecData(this.encryptRas(cfg.pwd));
        } else {
            if (new Date().getTime() - cfg.lastUpdatePwdTime < 2 * 1000) {
                //前5秒内的包都带上密钥，避免消息发送乱序情况下服务器解密错误
                msg.setSec(true); //msg.sec = this.encryptRas(cfg.pwd);

                msg.setSecData(this.encryptRas(cfg.pwd));
                msg.setSecretVersion(cfg.secretVersion);
            }
        } //let ab = new ArrayBuffer(msg.payload);
        //var wordArray = CryptoJS.lib.WordArray.create(ab);
        //将要发送的字节数据转为base64字符串格式，因为AES只接受字符串加密，同时方便服务器更好地处理解码

        let b64Str = utils.byteArr2Base64(msg.payload); //开始加密，密钥转为UTF8格式，保证Java服务端相同编码

        var encrypted = CryptoJS.AES.encrypt(b64Str, CryptoJS.enc.Utf8.parse(cfg.pwd), opts); //encrypted.ciphertext是一个以WordArray，也就是一个整数数组，要将此整数数据转为字节数组

        msg.payload = this.wordToByteBuffer(encrypted.ciphertext);
    },
    wordToByteBuffer: function (wordArray) {
        //var arrayOfWords = wordArray.hasOwnProperty("words") ? wordArray.words : [];
        // var length = wordArray.hasOwnProperty("sigBytes") ? wordArray.sigBytes : arrayOfWords.length * 4;
        let arrayOfWords = Object.prototype.hasOwnProperty.call(wordArray, 'words') ? wordArray.words : [];
        let length = Object.prototype.hasOwnProperty.call(wordArray, 'sigBytes') ? wordArray.sigBytes : arrayOfWords.length * 4;
        var uInt8Array = new Uint8Array(length);
        var index = 0;
        var word;
        for (let i = 0; i < length; i++) {
            word = arrayOfWords[i];
            uInt8Array[index++] = word >> 24;
            uInt8Array[index++] = (word >> 16) & 255;
            uInt8Array[index++] = (word >> 8) & 255;
            uInt8Array[index++] = word & 255;
        }

        return uInt8Array.buffer;
    },
    byteBuffer2ByteArray: function (byBuffer) {
        let byteArray = [];
        let dv = new DataView(byBuffer, 0, byBuffer.byteLength);

        for (let i = 0; i < byBuffer.byteLength; i++) {
            byteArray.push(dv.getUint8(i));
        }

        return byteArray;
    },
    hexToByteArray: function (hex) {
        let arr = [];

        for (let i = 0; i + 2 < hex.length; i += 2) {
            let hn = hex.substring(i, i + 2);
            arr.push(parseInt(hn, 16));
        }

        return arr;
    },

    checkAndDecrypt: function (msg) {
        if(!cfg.sslEnable) {
            return
        }

        if (!msg.isDownSsl()) {
            return;
        }

        if (!cfg.pwdTable) {
            this.init();
        }

        let ivStr = utils.fromUTF8Array(msg.getSaltData());
        let iv = CryptoJS.enc.Utf8.parse(ivStr);
        let opts = {
            mode: CryptoJS.mode.CBC,
            padding: CryptoJS.pad.Pkcs7,
            keySize: cfg.keySize,
            iv: iv,
            salt: null
        };
        let utf8pwd = CryptoJS.enc.Utf8.parse(cfg.pwd);
        /*var cipherParams = CryptoJS.lib.CipherParams.create({
      ciphertext: jm.utils.byteArr2Base64(msg.payload)
  });*/

        let b64str = utils.byteArr2Base64(msg.payload);
        var decrypted = CryptoJS.AES.decrypt(b64str, utf8pwd, opts);
        let dedata = this.byteBuffer2ByteArray(this.wordToByteBuffer(decrypted)); //let hex = this.b64tohex(msg.sign);

        if (!cfg.rsae.verify(utils.byteArr2Base64(dedata), msg.getSignData(), CryptoJS.MD5)) {
            throw 'Invalid sign';
        }

        msg.payload = dedata;
    },
    encryptRas: function (strContent) {
        if (!cfg.pwdTable) {
            this.init();
        } //对字符串做RSA加密，加密的结果是base64编码后的十六进制字符串

        let rst = cfg.rsae.encrypt(strContent); //return jm.utils.toUTF8Array(jm.utils.byteArr2Base64(this.hexToByteArray(rst)));
        //rst是base64编码后的十六进制字符串，此处对这个base64字符串做utf8编码转为字节数组

        return utils.toUTF8Array(rst);
    },
    decryptRas: function (strContent) {
        if (!cfg.pwdTable) {
            this.init();
        }

        return cfg.rsad.decrypt(strContent);
    },
    genStrPwd: function (len) {
        if (!cfg.pwdTable) {
            this.init();
        }

        let pwd = '';

        for (let i = 0; i < len; i++) {
            let idx = parseInt(Math.random() * 512);
            pwd += cfg.pwdTable[idx];
        }

        return pwd;
    },
    byteArray2Hex: function (byteArr) {
        let hex = '';

        for (let i = 0; i < byteArr.length; i++) {
            let e = byteArr[i];
            hex += cfg.hexStr.charAt(e >>> 4);
            hex += cfg.hexStr.charAt(e & 15);
        }

        return hex;
    },
    hex2b64: function (h) {
        var i;
        var c;
        var ret = ''; //每3个16进制数共3*4=12个bit位刚好等于2*6=12个bit，两个b64字符

        for (i = 0; i + 3 <= h.length; i += 3) {
            c = parseInt(h.substring(i, i + 3), 16);
            ret += cfg.b64map.charAt(c >> 6) + cfg.b64map.charAt(c & 63);
        }

        if (i + 1 == h.length) {
            c = parseInt(h.substring(i, i + 1), 16);
            ret += cfg.b64map.charAt(c << 2);
        } else {
            if (i + 2 == h.length) {
                c = parseInt(h.substring(i, i + 2), 16);
                ret += cfg.b64map.charAt(c >> 2) + cfg.b64map.charAt((c & 3) << 4);
            }
        }

        while ((ret.length & 3) > 0) {
            ret += cfg.b64pad;
        }

        return ret;
    },
    // convert a base64 string to hex
    b64tohex: function (s) {
        var ret = '';
        var i;
        var k = 0; // b64 state, 0-3

        var slop = 0;

        for (i = 0; i < s.length; ++i) {
            if (s.charAt(i) == cfg.b64pad) {
                //结束字符=
                break;
            }

            var v = cfg.b64map.indexOf(s.charAt(i));

            if (v < 0) {
                //无效b64字符
                continue;
            }

            if (k == 0) {
                ret += this.int2char(v >> 2);
                slop = v & 3;
                k = 1;
            } else {
                if (k == 1) {
                    ret += this.int2char((slop << 2) | (v >> 4));
                    slop = v & 15;
                    k = 2;
                } else {
                    if (k == 2) {
                        ret += this.int2char(slop);
                        ret += this.int2char(v >> 2);
                        slop = v & 3;
                        k = 3;
                    } else {
                        ret += this.int2char((slop << 2) | (v >> 4));
                        ret += this.int2char(v & 15);
                        k = 0;
                    }
                }
            }
        }

        if (k == 1) {
            ret += this.int2char(slop << 2);
        }

        return ret;
    },
    int2char: function (n) {
        return cfg.BI_RM.charAt(n);
    }
};
