import rpc from "../rpcbase.js"

const map = {}

export default {
	
	//取通用数据类型
	async getDataType(key) {
		if(map[key]) return map[key]
		let resp = await rpc.invokeByCode(834707949, [key,{}])
		if (resp.code != 0) {
			console.log("getDataType:" + key,resp)
			return resp
		}
		map[key] = resp
		return resp
	}
	
}