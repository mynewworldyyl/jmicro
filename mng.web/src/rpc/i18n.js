/* eslint-disable */
import rpc from './rpcbase';
import cfg from './config';
import utils from './utils';
import ls from './localStorage';

import { Constants } from './message'

const I18N_KEY = "_j_i18nKey"
const I18N_KEY_UT = "_j_i18nKey_UT"//最后一次更新时间
const I18N_KEY_VER = Constants.VER_DATA_KEY //当前缓存版本码

const MU_WITH_MSSEC = 1*60*1000

function __actreq(args) {
    let req = rpc.cmreq(-1011749358, args);
    req.clientId = 1;
    return req;
}

export default {
    //resource name is : i18n_zh.properties
    resources_: {},
    //take the i18n.propertis as the default language
    defaultLanguage_: '',
	
    init: async function () {
		
		this.defaultLanguage_ = this.getLan_(this.getLocal_());
		let ver = ls.get(I18N_KEY_VER+'_i18n')//服务器版本
		let ever = ls.get(I18N_KEY_VER)//本地版本
		
		if(ver) {
			ls.set(I18N_KEY_VER,ver)
		}
		
		if(!ver || ver == ever) {
			//兼容服务器没有配置版本的情况
			console.log("i18n load from local and then server ver= " + ver + ",ever="+ever)
			if(!this._loadFromLocal() ) {
				return await this.getFromServer_(cfg.mod)
			} else {
				return {code:0}
			}
		} else {
			console.log("load from server")
			//有版本更新，直接服务器取
			return await this.getFromServer_(cfg.mod)
		}	
    },
	
	_loadFromLocal() {
		let lres = ls.get(I18N_KEY)
		if(lres == 'undefined') {
			lres = null
			ls.remove(I18N_KEY)
		}
		
		if(lres) {
			let lt = ls.get(I18N_KEY_UT)
			let ct = parseInt(new Date().getTime()/MU_WITH_MSSEC) -lt//距离上次加载成功的分钟数
			if(ct < 3) {
				//小于24小时，不更新
				
				this._parseData(JSON.parse(lres))
				console.log("_loadFromLocal success ")
				return true
			}
		}
		console.log("_loadFromLocal fail ")
		return false
	},
	
    i18n: function (key, defaultVal, params) {
        return this.get(key, defaultVal, params);
    },
	
    get: function (key, defaultVal, params) {
        if (!key || key.length == 0) {
            return ""
        }

        if (!key.trim) {
            key += '';
        }

        let v = this.resources_[key.toLowerCase().trim()];

        if (!v) {
            if (defaultVal) {
                return defaultVal;
            } else {
                return key;
            }
        }

        if (!params || params.length == 0) {
            return v;
        }

        let result = [];
        let index = null;

        for (let i = 0; i < v.length; i++) {
            let ch = v.charAt(i);

            if (ch == '{') {
                index = '';
            } else {
                if (index != null && ch == '}') {
                    index = parseInt(index);

                    if (index >= 0 && index < params.length) {
                        result.push(params[index]);
                    }

                    index = null;
                } else {
                    if (index != null) {
                        index += ch;
                    } else {
                        result.push(ch);
                    }
                }
            }
        }

        v = result.join('');
        return v;
    },
	
	 getFromServer_: async function (resPath) {
        let that = this;
        let req = __actreq([cfg.mod, this.defaultLanguage_, cfg.clientId]);
        let resp = await rpc.callRpc(req)  
		if (resp || resp.code == 0) {
			console.log("getFromServer_ success")
			let res = resp.data;
			if(res) {
				ls.set(I18N_KEY, JSON.stringify(res))
				let ut = parseInt(new Date().getTime() / MU_WITH_MSSEC)
				ls.set(I18N_KEY_UT, ut)//当前时间小时数
				this._parseData(res)
			}
			return resp
		}
		console.log("getFromServer_ fail: ",resp)
		return resp
    },
	
	_parseData(res) {
		console.log(res)
		for (let k in res) {
		    this.resources_[k] = res[k];
		}
	},
	
    getLocal_: function () {
        let lang = ls.get('language');

        if (!lang) {
            if (utils.isUni()) {
                lang = 'zh-cn';
            } else {
                if (navigator.browserLanguage) {
                    lang = navigator.browserLanguage;
                } else {
                    lang = navigator.language;
                }
            }
        }

        return lang;
    },
	
    getLan_: function (lan) {
        if (!lan) {
            return '';
        }

        return lan.toLowerCase();
    }
};
