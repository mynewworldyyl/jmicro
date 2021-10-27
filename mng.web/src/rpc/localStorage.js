/* eslint-disable */
import util from "./utils"
const iswx = util.isWx()

const OBJ_PREFIX = '%%_'

export default  {
    //this.updateBrowser_ = jm.utils.i18n.get('update_your_browser');
    set(key,value) {
        if(!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }

        if(!iswx && typeof value == 'object') {
            value =  OBJ_PREFIX + JSON.stringify(value)
        }

        iswx ? wx.setStorageSync(key,value) : window.localStorage.setItem(key,value);
    },

    remove(key) {
        if(!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }
        iswx ? wx.removeStorageSync(key) :window.localStorage.removeItem(key);
    },

    get(key) {
        if(!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }
        if(iswx) {
            return wx.getStorageSync(key)
        } else {
            let v = localStorage.getItem(key)
            if(v == '[object Object]') {
                this.remove(key)
                throw 'invalid storage val: '+v + 'with key: ' + key
            }
            if(v && v.startWith(OBJ_PREFIX)) {
                v = v.substr(OBJ_PREFIX.length)
                v = JSON.parse(v)
            }
            return v
        }
    },

    clear() {
        if(!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }
        if(confirm('this operation will clear all local storage data?')) {
          iswx ? wx.clearStorageSync() : window.localStorage.clear();
        }
    },

    isSupport() {
        try {
            return iswx || window.localStorage !== null
        } catch (e) {
            return false;
        }
    },

}