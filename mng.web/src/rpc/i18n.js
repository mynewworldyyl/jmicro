import rpc from "./rpcbase";
import cfg from "./config";
import utils from "./utils";
import ls from "./localStorage"

function __actreq(args){
    let req = rpc.cmreq(-1011749358, args)
    req.clientId = 1;
    return req;
}

export default  {
  //resource name is : i18n_zh.properties
  resources_ : {},
  //take the i18n.propertis as the default language
  defaultLanguage_ : '',
  supportLangs : [],

  init : function(cb) {
    if(this.supportLangs.length > 0) return
    this.defaultLanguage_ = this.getLan_(this.getLocal_());
    this.getFromServer_( cfg.mod,cb)
  },

  i18n : function(key,defaultVal,params) {
    return this.get(key,defaultVal,params)
  },

  get : function(key,defaultVal,params) {
    if(!key || key.length == 0) {
      throw 'i18n key cannot be null'
    }

    if(!key.trim) {
      key +=''
    }

    let v = this.resources_[key.toLowerCase().trim()];
    if(!v) {
      if(defaultVal) {
        return defaultVal
      }else {
        return key
      }
    }

    if(!params || params.length == 0) {
      return v;
    }

    let result = [];
    let index = null;
    for (let i = 0; i < v.length; i++){
      let ch = v.charAt(i)
      if (ch == '{') {
        index = ''
      } else if (index != null && ch == '}') {
        index = parseInt(index)
        if (index >= 0 && index < params.length){
          result.push(params[index])
        }
        index = null
      } else if (index != null) {
        index += ch
      } else  {
        result.push(ch)
      }
    }
    v = result.join('');
    return v;
  },

  getFromServer_ : function(resPath,cb) {
    let self = this;
    let req = __actreq([cfg.mod,this.defaultLanguage_,cfg.clientId])
    rpc.callRpc(req)
      .then(resp => {
        if(resp || resp.code == 0) {
          let res = resp.data
          for (let k in res){
            self.resources_[k] = res[k]
          }
          if(cb) {
            cb();
          }
        }
      }).catch(err => {
      cb(err);
      throw err;
    });
  },

  getLocal_ : function() {
    let lang = ls.get('language')
    if(!lang) {
      if(utils.isWx()) {
          lang='zh-cn'
      }else {
          lang = navigator.browserLanguage ? navigator.browserLanguage : navigator.language
      }
    }
    return lang;
  },

  getLan_ : function(lan) {
    if(!lan) {
      return '';
    }
    return lan.toLowerCase();
  },

}
