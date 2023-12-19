/* eslint-disable */
import rpc from './rpcbase';
import ls from './localStorage';
import cfg from './config.js';

import { Constants } from './message'

const MC = 93593292

export default {

    prf: {},
	
    async init() {
		//获取版本信息
		let resp = await this._getFromServer("versionData")
		console.log("versionData profile :",resp)
		
		resp = await this._getFromServer("base")
		console.log("base profile :",resp)
		
		return resp
    },
	
	getFromLocal(mod,key) {
		if(this.prf[mod]) {
			return this.prf[mod][key]
		} else {
			return ls.get(mod+'_'+key)
		}
	},
	
	async getVal(mod,key) {
		if(!this.prf[mod])  {
			await this._getFromServer(mod)
		}
		return this.prf[mod][key]
	},
	
	async _getFromServer(mod) {
		let resp = await rpc.invokeByCode(MC,[cfg.clientId,mod])
		if(resp.code == 0) {
			console.log("Load profile success: ",resp)
			this.prf[mod] = resp.data
			if(resp.data && resp.data.length > 0) {
				resp.data.forEach(e=>{
					ls.set(mod + '_'+ e.key, e.val)
				})
			}
		} else {
			console.log("Load profile fail",resp)
			//默认无值
			this.prf[mod] = {}
		}
		delete resp.data
		return resp
	}
	
};
