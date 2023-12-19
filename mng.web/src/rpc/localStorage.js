/* eslint-disable */
import util from './utils';
import auth from './auth.js';
import jcons from './constants.js';
const iswx = util.isUni();

const OBJ_PREFIX = '%%_';

const PREFIX_TO_KEY = '__J_O_K/';

export default {

	setByAct(key, value) {
		this.set(this._actk(key), value)
	},
	
	getByAct(key) {
		return this.get(this._actk(key))
	},
	
	removeByAct(key) {
		this.remove(this._actk(key))
	},
	
	//以分钟为单位的超时时间
	expireByAct(key,toWithMinutes){
		let k = PREFIX_TO_KEY + "/" + this._actk(key)
		this.expire(k,toWithMinutes)
	},
	
	getByActWithTimeout(key) {
		return this.getWithTimeOut(this._actk(key))
	},
	
	getWithTimeOut(key){
		let k = PREFIX_TO_KEY + key
		let v = this.get(k)
		if(v) {
			if(parseInt(v) > this._curMinu()) {
				return this.get(key)
			} else {
				//删除已经超时的无效数据
				this.remove(k)
				this.remove(key)
			}
		}
		return this.get(key)
	},
	
	_actk(k) {
		if(!auth.isLogin()) {
			throw '未登录'
		}
		return auth.actInfo.id + "/__wack/" + k
	},
	
	_curMinu() {
		return new Date().getTime()/jcons.MU_WITH_MSSEC
	},
	
	//以分钟为单位的超时时间
	expire(key,toWithMinutes){
		let k = PREFIX_TO_KEY + key
		//最后有效截止时间，单位是分钟
		let et = new Date().getTime()/jcons.MU_WITH_MSSEC + toWithMinutes
		this.set(k,et)
	},
	
    //this.updateBrowser_ = jm.utils.i18n.get('update_your_browser');
    set(key, value) {
        if (!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }

        if (!iswx && typeof value == 'object') {
            value = OBJ_PREFIX + JSON.stringify(value);
        }

        if (iswx) {
            uni.setStorageSync(key, value);
        } else {
            window.localStorage.setItem(key, value);
        }
    },
	
    remove(key) {
        if (!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }

        if (iswx) {
            uni.removeStorageSync(key);
        } else {
            window.localStorage.removeItem(key);
        }
    },
	
    get(key) {
        if (!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }
		
		let v = null
        if (iswx) {
            v = uni.getStorageSync(key);
        } else {
            v = localStorage.getItem(key);
        }
		
		if (v === '[object Object]') {
		    this.remove(key);
		    throw 'invalid storage val: ' + v + 'with key: ' + key;
		}
		
		if (v && typeof v ==='string' && v.startWith(OBJ_PREFIX)) {
		    v = v.substr(OBJ_PREFIX.length);
		    v = JSON.parse(v);
		}
		
		return v;
    },
	
    clear() {
        if (!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }

        if (confirm('this operation will clear all local storage data?')) {
            if (iswx) {
                uni.clearStorageSync();
            } else {
                window.localStorage.clear();
            }
        }
    },
	
    isSupport() {
        try {
            return iswx || window.localStorage !== null;
        } catch (e) {
            return false;
        }
    }
};
