import i18n0 from "@/rpc/i18n"

const FORMAT_DATE = 'yyyy/MM/dd';  //1
const FORMAT_DATE_TIME = 'yyyy/MM/dd hh:mm:ss'; //2
const FORMAT_DATE_TIME_MS = 'yyyy/MM/dd hh:mm:ss S'; //3
const FORMAT_TIME = 'hh:mm:ss'; //4
const FORMAT_TIME_MS = 'hh:mm:ss S'; //5

let formatDate = function(time,t) {
    let f = FORMAT_DATE_TIME_MS;

    switch (t) {
        case 1:
            f = FORMAT_DATE;
            break;
        case 2:
            f = FORMAT_DATE_TIME;
            break;
        case 3:
            f = FORMAT_DATE_TIME_MS;
            break;
        case 4:
            f = FORMAT_TIME;
            break;
        case 5:
            f = FORMAT_TIME_MS;
            break;
    }

    return new Date(time).format(f);
}

let i18n = function(key,defaultVal,params) {
    return i18n0.get(key,defaultVal,params);
}

export {
    formatDate,
    i18n
}