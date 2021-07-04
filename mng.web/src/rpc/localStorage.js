import utils from "@/rpc/utils"

export default  {
    //this.updateBrowser_ = jm.utils.i18n.get('update_your_browser');
    set(key,value) {
        if(!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }
        window.localStorage.setItem(key,value);
    },

    remove(key) {
        if(!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }
        window.localStorage.removeItem(key);
    },

    get(key) {
        if(!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }
        return window.localStorage.getItem(key);
    },

    clear() {
        if(!this.isSupport()) {
            //alert(this.updateBrowser_);
            return;
        }
        if(confirm('this operation will clear all local storage data?')) {
            window.localStorage.clear();
        }
    },

    supportLocalstorage() {
        try {
            return window.localStorage !== null;
        } catch (e) {
            return false;
        }
    },

    isSupport() {
        if(utils.isIe() && utils.version() < 8) {
            return false;
        }else {
            return true;
        }
    }

}