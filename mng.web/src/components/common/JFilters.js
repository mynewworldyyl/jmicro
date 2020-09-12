let formatDate = function(time,f) {
    if(!f) {
        f = "yyyy/MM/dd hh:mm:ss S";
    }
    return new Date(time).format(f);
}

let i18n = function(key,defaultVal,params) {
    return window.jm.mng.i18n.get(key,defaultVal,params);
}

export {
    formatDate,
    i18n
}