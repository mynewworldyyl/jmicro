import config from './config';
import utils from './utils';
import cons from './constants';
export default {
    data() {
        return {};
    },
    getQueryParam: function (name) {
        if (location.href.indexOf('?') == -1 || location.href.indexOf(name + '=') == -1) {
            return '';
        }

        let reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)');
        let r = window.location.search.substr(1).match(reg);

        if (r != null) {
            return decodeURI(r[2]);
        } //对参数进行decodeURI解码

        return null;
    },
    get: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'GET', errCb, fullpath);
    },
    post: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'POST', errCb, fullpath);
    },
    put: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'PUT', errCb, fullpath);
    },
    ajax: function (path, params, sucCb, async, method, errCb, fullpath) {
        if (!sucCb) {
            throw 'Callback method cannot be null';
        }

        if (!method) {
            throw 'request method cannot be null';
        }

        if (!path) {
            path = '/';
        }

        this._doAjax(path, params, sucCb, async, method, errCb, fullpath);
    },
    _doAjax: function (path, params, sucCb, async, method, errCb, fullpath) {
        /* if(!jm.urls.checkNetwork()) {
   throw '网络连接不可用';
   }*/
        if (fullpath) {
            path = fullpath;
        } else {
            if (!(path.startWith('http://') && !path.startWith('https://'))) {
                path = this.getWebResourceHttpPath(path);
            }
        }

        if (!params) {
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
    },
    uploadBase64Picture: function (data, ptype, cb, pos) {
        let params = {
            encode: 'base64',
            imageType: ptype,
            name: 'base64_picture_file' + jm.utils.getId(),
            contentType: 'image/' + ptype + ';base64',
            base64Data: data,
            pos: pos
        };
        let url = this.getWebResourceHttpPath(cons.UPLOAD_BASE64);
        this.post(
            url,
            params,
            (data1, status, xhr) => {
                console.log(data1);

                if (status !== 'success') {
                    cb(status, data1);
                } else {
                    cb(null, data1);
                }
            },
            (err, xhr) => {
                console.log(err);
                cb(err, null);
            }
        );
    },
    //get: function (path, params, cb, errCb, fullpath)
    postHelper(url, params, cb) {
        this.post(
            url,
            params,
            (data, status, xhr) => {
                if (data.success) {
                    cb(null, data);
                } else {
                    cb(data, data);
                }
            },
            (err) => {
                cb(err, null);
            }
        );
    },
    getHelper(url, params, cb, fullpath) {
        if (!params) {
            params = {};
        }

        this.get(
            url,
            params,
            (data, status, xhr) => {
                cb(null, data);
            },
            function (err) {
                cb(err, null);
            },
            fullpath
        );
    },
    getUrl: function (url) {
        return this.getWebResourceHttpPath(url);
    },
    getWebContextPath() {
        var pathname = location.pathname;
        pathname = pathname.substring(1, pathname.length);
        pathname = pathname.substring(0, pathname.indexOf('/'));
        return '/' + pathname;
    },
    getWSApiPath() {
        var wp = 'ws://' + location.host + +config.wsContext;
        return wp;
    },
    getHttpApiPath() {
        var wp = 'http://' + location.host + config.httpContext;
        return wp;
    },
    getWebResourceHttpPath: function (subPath) {
        var wp = 'http://' + location.host + utils.getWebContextPath() + subPath;
        return wp;
    }
};
