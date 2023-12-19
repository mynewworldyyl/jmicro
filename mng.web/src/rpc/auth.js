/* eslint-disabled */
import cons from './constants';
import config from './config';
import utils from './utils';
import lc from './localStorage';
import rpc from './rpcbase.js';

import { Message, Constants } from './message';
import transport from './transport'; //import socket from "./socket";

let actListeners = [];
let loginPage = "/pages/auth/login"

const PROFILE_KEY = "_profileKey"
const LOGIN_TYPE = "_loginType"
const LOGIN_TYPE_PWD = "pwd"
const LOGIN_TYPE_WX = "wx"

const SC_WAIT_ACTIVE = 1 //待激活
const SC_ACTIVED = 2 //已经激活
const SC_FREEZE = 4 //冻结

const st2Desc = {1:'待激活',2:'已激活',4:'已冻结'}

const HB_INTERVAL = 3 * 1*60*1000 //1分钟一次

let lastSetTime = 0

export default {
	PROFILE_KEY,
	LOGIN_TYPE,
	LOGIN_TYPE_PWD,
	LOGIN_TYPE_WX,
	SC_WAIT_ACTIVE,
	SC_ACTIVED,
	SC_FREEZE,
	st2Desc,
	
	_clients:[],
	
	lastat : new Date().getTime(),
	
    actInfo: null,
	toLoginCb:null,

	getClients() {
		let cs = []
		cs.push(...this._clients)
		return cs
	},
	
	updateAuth(createdBy) {
		if(!this.isLogin()) return false
		if(typeof createdBy == 'undefined') return false
		return  createdBy == this.actInfo.id || this.isAdmin()
	},
	
	async loginHearBeat() {
		
		if(!this.isLogin()) {
			console.log("Not login status")
			return
		}
		
		let inte = new Date().getTime() - this.lastat
		if(inte < HB_INTERVAL) {
			return
		}

		//心跳
		let resp = await  rpc.invokeByCode(1452926548, [""]);
		console.log(resp)
		if(resp.code != 0 || !resp.data) {
			this.actInfo = null
			console.log(resp)
			return
		}
		
	},

    addActListener(l,l2) {
		if(l && typeof l != 'function') {
			l = l2
		}
		
		if(!l) {
			throw 'invalid listener: ' + l
		}
		
		if(this.getListenerIndex(l) >= 0) {
			return
		}
		if(this.actInfo && this.actInfo != '') {
			l(Constants.LOGIN,this.actInfo);
		}
		actListeners.push(l);
    },
	
	getListenerIndex(l) {
		for(let i = 0; i < actListeners.length; i++) {
			if(actListeners[i]===l) {
				i
			}
		}
		return -1;
	},
	
	getActId() {
	   if(this.isLogin()) {
		   return this.actInfo.pUserId ? this.actInfo.pUserId : this.actInfo.id
	   }
	   return null
	},
	
	isMy(actId) {
		console.log("isMy", actId)
	   return this.getActId() == actId
	},
	
    async getCode(type,vcode,codeId,mobile) {
        let resp = await  rpc.invokeByCode(-1494008743, [type,vcode,codeId,mobile]); //__actreq('getCode',[type]);
        return resp;
    },
	
    removeActListener(l) {
		let idx = this.getListenerIndex(l)
        if ( idx < 0) {
            return
        }
        actListeners.splice(idx,1)
    },
	
	setValue(key,value) {
		if(this.actInfo[key] == value) return
		this.actInfo[key] = value
		lc.set(Constants.USER_INFO, this.actInfo);
	},
	
	hasRole(roldId) {
		if(this.isAdmin()) return true
		if(!roldId || !this.isLogin() || !this.actInfo.roles || this.actInfo.roles.length == 0) {
			return false;
		}
		return this.actInfo.roles.indexOf(roldId) >= 0
	},
	
	hasTag(tag) {
		if(this.isAdmin()) return true
		if(!tag || !this.isLogin() || !this.actInfo.tags || this.actInfo.tags.length == 0) {
			return false;
		}
		return this.actInfo.tags.indexOf(tag) >= 0
	},
	
	hasPerm(perId) {
		if(this.isAdmin()) return true
		if(!perId || !this.isLogin() || !this.actInfo.pers || this.actInfo.pers.length == 0) {
			return false;
		}
		return this.actInfo.pers.indexOf(perId) >= 0
	},
	
	async loadTags() {
		if(!this.isLogin()) {
			return {code:1,msg:'无效登录'}
		}
		let r = await  rpc.invokeByCode(174222680, [])
		console.log(r)
		
		if(r.code != 0) {
			console.log(r)
			return r
		}
		
		if(r.data) {
			this.actInfo.tags = r.data
		}else {
			this.actInfo.tags = []
		}
		lc.set(Constants.USER_INFO, this.actInfo);
		return r
	},
	
	async loadPerms() {
		if(!this.isLogin()) {
			return {code:1,msg:'无效登录'}
		}
		
		let r = await  rpc.invokeByCode(-1167657536, [])
		if(r.code != 0) {
			console.log(r)
			return r
		}
		
		if(r.data) {
			this.actInfo.roles = r.data.roles
			this.actInfo.pers = r.data.perms
		} else {
			this.actInfo.roles = []
			if(this.actInfo.pers.findIndex(e=>{return "*" == e})) {
				this.actInfo.pers = []
			}else {
				this.actInfo.pers = ["*"]
			}
		}
		lc.set(Constants.USER_INFO, this.actInfo);
		return r
	},
	
    isLogin() {
        if (!this.actInfo) {
            let ac = lc.get(Constants.USER_INFO);
			if(ac && ac != '') {
				this.setActInfo(ac)
				this.loadPerms()
				this.loadTags()
			}
        }
        return this.actInfo != null && this.actInfo != '';
    },
	
    isAdmin() {
        return this.isLogin() && this.actInfo.admin;
    },
	
    setActInfo(actInfo) {
        if (!actInfo) {
            throw 'invalid act info';
        }
		lastSetTime = new Date().getTime()/1*60*1000//转为分钟
        this.actInfo = actInfo;
        lc.set(Constants.USER_INFO, actInfo);
		
		if(this._clients.findIndex(e=>actInfo.defClientId == e) < 0) {
			this._clients.push(actInfo.defClientId)
		}
		
		if(this.isAdmin()) {
			if(this._clients.findIndex(e=> -1 == e) < 0) {
				this._clients.push(-1); //通用ClientId
			}
		} else {
			let idx = this._clients.findIndex(e=> -1 == e);
			if(idx >= 0) {
				this._clients.splice(idx,1)
			}
		}
        this._notify(Constants.LOGIN);
    },
	
    unsetActInfo() {
		console.log('删除登录信息')
        this.actInfo = null;
        lc.remove(Constants.USER_INFO);
		//禁止自动登录
		lc.remove(Constants.ACT_AUTO_LOGIN_KEY) 
        this._notify(Constants.LOGOUT);
    },
	
    login(actName, pwd, vcode, vcodeId,toLoginPageIfNeed) {     
		return new Promise((reso, reje)=>{
			if(!actName) {
				actName = lc.get(Constants.ACT_NAME_KEY)
			}
			
			if(!actName || actName == 'undefined') {
				if(toLoginPageIfNeed) {
					this.__toLoginPage()
					reso({code:0, msg:""})
				} else {
					reso({code:1, msg:"账号名不能为空"})
				}
				return
			}
			
			if(!pwd) {
				pwd = lc.get(Constants.ACT_PWD_KEY)
			}
			
			if(!pwd ) {
				if(actName.startsWith('guest_')) {
					pwd = ""
				} else {
					reso({code:1,msg:'密码不能为空'})
					return
				}
			}
			
			lc.remove("vcodeId")
			lc.remove("vcode")
			
			 let that = this
			 rpc.invokeByCode("login", [config.clientId, actName, pwd, vcode, vcodeId])
			 .then(resp=>{
				 //console.log(resp)
				 if (resp.code == 0) {
					  lc.set(Constants.ACT_NAME_KEY, actName);
					  lc.set(Constants.ACT_PWD_KEY, pwd);
					  let sai = resp.data
					  that.setActInfo(sai)
					  resp.data = that.actInfo
					  reso(resp)
				 } else if(resp.code == 4) {
					  let arr = resp.msg.split("$@$")
					  if(toLoginPageIfNeed) {
						this.__toLoginPage(arr[0],arr[1])
						reso({code:0,msg:""})
					  } else {
						  reso({code: resp.code, vcodeId:arr[1], vcode:arr[0]})
					  }
				 } else {
					  if(toLoginPageIfNeed) {
						this.__toLoginPage()
					  }
					  reso(resp);
				 }
			 })
			 .catch(err=>{
				 console.log(err)
				 reso({code:1,msg:err})
			 })
		})
    },
	
	__toLoginPage(vcode,vcodeId) {
		if(vcodeId) {
			lc.set("vcodeId",vcodeId)
			lc.set("vcode",vcode)
		}
		
		if(this.toLoginCb) {
			this.toLoginCb(vcode,vcodeId)
		}else if(utils.isUni() && loginPage) {
			uni.navigateTo({url:loginPage,fail:(err)=>{console.log(err)}})
		} else {
			throw "未注册登录页面或回调"
		}
	},
	
    async logout(cb){

		console.log(this.actInfo)

        if (!this.actInfo) {
			console.log("Not Login")
            return true
        }

         let that = this
		 try {
			  let resp = await rpc.invokeByCode(1105614483, [])
			  console.log(resp)
			  if (resp.code == 0) {
				  that.unsetActInfo();
			      return true
			  } else {
				  return false
			  }
		 } catch(err) {
			console.log(err)
			return false
		 }	
    },
	
    checkLogin() {
        let self = this;
        return new Promise((reso, reje) => {
            if (!this.isLogin()) {
				console.log('Auto login')
				let autoLogin = lc.get(Constants.ACT_AUTO_LOGIN_KEY)
				if(autoLogin && (autoLogin==true || autoLogin == 'true')) {
					//自动登录
					this.login()
					.then((res) => {
						reso(res)
					}).catch((err) => {
						reso({code:1,msg:err})
					});
				} else {
					reso({code:1,msg:"未登录"})
				}
            } else {
                let req = rpc.cmreq(-515329030, [self.actInfo.loginKey]);
                rpc.callRpc(req)
				.then((res) => {
					if(res.data) {
						reso(res);
					} else {
						console.log('invalid login key : ' + self.actInfo.loginKey)
						self.actInfo = null;
						lc.remove(Constants.USER_INFO);
						this.login()
						.then((res) => {
							reso(res)
						}).catch((err) => {
							reso({code:1,msg:err})
						});
					}
				}).catch((err) => {
					console.log(err);
					self.actInfo = null;
					lc.remove(Constants.USER_INFO);
					reso({code:1,msg:err});
				});
            }
        });
    },
	
    init(opts) {
		if(opts.loginPage) {
			loginPage = opts.loginPage
		}else if(config.loginPage) {
			loginPage = config.loginPage
		}
		return this.checkLogin()
    },

  _notify: function (type) {
        for (let key = 0; key < actListeners.length; key++) {
            if (actListeners[key]) {
                actListeners[key](type, this.actInfo);
            }
        }
    },
};
