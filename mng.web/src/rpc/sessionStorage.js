modules.exports = {
    //this.updateBrowser_ = jm.utils.i18n.get('update_your_browser');
    set(key, value) {
        if (!this.supportLocalstorage()) {
            //alert(this.updateBrowser_);
            return;
        }

        this.setItem(key, value);
    },

    remove(key) {
        if (!this.supportLocalstorage()) {
            //alert(this.updateBrowser_);
            return;
        }

        this.removeItem(key);
    },

    get(key) {
        if (!this.supportLocalstorage()) {
            //alert(this.updateBrowser_);
            return;
        }

        return this.getItem(key);
    },

    clear() {
        if (!this.supportLocalstorage()) {
            //alert(this.updateBrowser_);
            return;
        }

        this.clear();
    },

    supportLocalstorage() {
        try {
            return window.sessionStorage !== null;
        } catch (e) {
            return false;
        }
    }
};
