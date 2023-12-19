const ADMIN_MODEL_PER = '*';
const ADMIN_OP_PER = -1;
export default {
    /*
	 cache:{},
	 ip:process.env.ip,
	 port: process.env.port,
	 txtContext : process.env.txtContext,
	 binContext : process.env.binContext,
	 httpContext : process.env.httpContext,
	 useWs : process.env.useWs,
	 sslEnable:process.env.sslEnable,
	 */
    wsProtocol: 'ws',
    protocol: 'http',
	//ip: '192.168.3.4',
	//ip:'192.168.1.103',
	//ip:'192.168.5.129',
	//ip:'192.168.3.41',
    //ip:'apigate.jmicro.cn',
    ip:'jmicro.cn',
	//ip:'47.107.141.158',

    /*  port:'',
    sport:'',*/
    port: 9090,
    sport: 9092,
    txtContext: '_txt_',
    binContext: '_bin_',
    httpContext: '_http_',
	fsContext: '_fs_',
    useWs: false,
    sslEnable: false,
    publicKey:
        'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCt489YTxmLjNVxfFKSORyUgXjr65MQR1a/QdlriFEWXUAaLpVWP41YTlSA5ecG54xVwl2ayLytCv4CJNqYPeYNPUVXPr1tqND1aZYK9iUQQ0K36g2QZaigg+f/NJSY6w4XITQdBz3PnJOOzOK+cOew4R0XiyrR8sHG2Is4Mf9qowIDAQAB',
    privateKey: '',
    includeMethod: true,
    clientId: 1,
    //system client id
    mod: 'jmng',
    adminModelPerm: ADMIN_MODEL_PER,
    adminOpPerm: ADMIN_OP_PER,
	loginPage:"/pages/auth/login",
	
	httpUrl: function() {
		return this.gwHttpUrl() + '/' + this.httpContext
	},
	
	fsHttpUrl: function() {
		return this.gwHttpUrl() + '/' + this.fsContext+"/"
	},
	
	gwHttpUrl: function() {
		return this.protocol + '://' + this.ip + ':' + this.port
	},
	
	imgUrl : function(fileId,w,h) {
		
		if(fileId.startsWith("http")) {
			return fileId
		}
		
		if(!w) {
			return this.fsHttpUrl() + fileId
		}
		
		let subfix = ''
		let n = fileId
		let idx = n.indexOf('.')
		if(idx <= 0) {
			throw 'invalid image file id: ' + fileId
		}
		
		let ar = n.split('.')
		n = ar[0]
		subfix = ar[1]
		
		if(w > 0 ) {
			n += "@" + w
			
			if(h > 0) {
				n += "x" + h
			}
		}
	
		n = this.fsHttpUrl() + n + '.' + subfix
		return n
	}
	
};
