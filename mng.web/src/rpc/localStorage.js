/* eslint-disable */
import util from "./utils"
const iswx = util.isWx()

export default  {
    //this.updateBrowser_ = jm.utils.i18n.get('update_your_browser');
    set(key,value) {
        if(!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
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
        return  iswx ? wx.getStorageSync(key) : window.localStorage.getItem(key);
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