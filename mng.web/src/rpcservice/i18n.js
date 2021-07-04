import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";
import localStorage from "@/rpc/localStorage";

export default  {

  sn:'cn.jmicro.mng.api.II8NService',
  ns : cons.NS_MNG,
  v:'0.0.1',
  //resource name is : i18n_zh.properties
  resources_ : {},
  //take the i18n.propertis as the default language
  defaultLanguage_ : '',
  supportLangs : [],
  resPath:'cn.jmicro.mng.i18n.I18NManager',

  init : function(cb) {
    this.defaultLanguage_ = this.getLan_(this.getLocal_());
    this.getFromServer_(this.resPath,cb);
  },

  i18n : function(key,defaultVal,params) {
    return this.get(key,defaultVal,params);
  },

  get : function(key,defaultVal,params) {
    if(!key || key.length == 0) {
      throw 'i18n key cannot be null';
    }

    if(!key.trim) {
      key +='';
    }

    let v = this.resources_[key.toLowerCase().trim()];
    if(!v) {
      if(defaultVal) {
        return defaultVal
      }else {
        return key;
      }
    }

    if(!params || params.length == 0) {
      return v;
    }

    let result = [];
    let index = null;
    for (let i = 0; i < v.length; i++){
      let ch = v.charAt(i);
      if (ch == '{') {
        index = '';
      } else if (index != null && ch == '}') {
        index = parseInt(index);
        if (index >= 0 && index < params.length){
          result.push(params[index]);
        }
        index = null;
      } else if (index != null) {
        index += ch;
      } else  {
        result.push(ch);
      }
    }
    v = result.join('');
    return v;
  },

  getFromServer_ : function(resPath,cb) {
    let self = this;
    rpc.callRpcWithParams(this.sn, this.ns, this.v, 'keyValues', [resPath,this.defaultLanguage_])
      .then(resp => {
        if(resp || resp.code == 0) {
          let res = resp.data;
          for (let k in res){
            self.resources_[k] = res[k];
          }
          if(cb) {
            cb();
          }
        }
      }).catch(err => {
      cb();
      throw err;
    });
  },

  getLocal_ : function() {
    var lang = localStorage.get('language');
    if(!lang) {
      lang = navigator.browserLanguage ? navigator.browserLanguage : navigator.language;
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
