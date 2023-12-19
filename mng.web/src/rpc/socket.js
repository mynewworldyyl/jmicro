/* eslint-disable */
import { Constants, Message } from './message';
import ApiResponse from './response';
import ps from './pubsub';
import config from './config';
import rpc from './rpcbase';
import auth from './auth';
import utils from './utils';

let listeners = {};

let waiting = []; 
//let logData = null;
//let idCallback = {};
//0：初始状态，未连接
//1：连接建立中
//2：连接已经建立，可正常发送数据
let isInit = 0

let timer = null;
let sending = false

const blockSize = 32768;

let wsk = null
let iswx = utils.isUni()

export default {
    reconnect() {
		if(iswx) {
			uni.closeSocket({})
		} else {
			if (wsk) {
			    wsk.close();
			} //isInit = false
			wsk = null
		}
		isInit = 0
    },
	
    __onClose(event) {
		for(let k in listeners) {
			listeners[k]('connection error');
			delete listeners[k]
		}
		this.reconnect()
        console.log('connection close');
        console.log(event); //isInit = false;
    },
	
	__readMessage(buf) {
		let msg = new Message();
		msg.decode(buf);
		//console.log("readMessage: ",msg)
		if (msg.type == Constants.MSG_TYPE_ASYNC_RESP) {
			//异步消息
		    ps.onMsg(msg);
		} else {
		    if (listeners[msg.msgId]) {
		        listeners[msg.msgId](msg);
		        delete listeners[msg.msgId];
		    }
		}
	},
	
    __onMessage(event) {
		if(iswx) {
			this.__readMessage(event.data)
		}else {
			event.data.arrayBuffer().then( (buf)=> {
			    this.__readMessage(buf)
			});
		}
    },
	
    __init(onopen) {
        isInit = 1;
        let url = config.wsProtocol + '://' + config.ip + ':' + config.port + '/' + config.binContext;
		console.log(url)
         if (!iswx) {
            wsk = new WebSocket(url); //获得WebSocket对象
            //当有消息过来的时候触发

            wsk.onmessage = (err)=>{ this.__onMessage(err) }; //连接关闭的时候触发

            wsk.onclose = (err)=>{ this.__onClose(err) }//连接打开的时候触发

            wsk.onopen = (event) => {
				 isInit = 2;
                console.log('connect successfully');
                console.log(event);
                if (onopen) {
                    onopen();
                }
            };
        } else  {
			let that = this
			uni.onSocketOpen( (res)=>{
				isInit = 2;
				console.log('WebSocket连接已打开！');
				if (onopen) {
					onopen();
				}
				 //this.__onClose();
			});
			
			uni.onSocketError((res)=>{
			   console.log('WebSocket连接打开失败，请检查！');
			   that.__onClose(res);
			});
			
			uni.onSocketMessage((res)=>{
			   that.__onMessage(res);
			});
			
			uni.onSocketClose( (res)=>{
			  console.log('WebSocket 已关闭！');
			  that.__onClose(res);
			});
		   
			wsk = uni.connectSocket({
			    url: url,
				method:"GET",
				binaryType: 'arraybuffer',
				success : s =>{
					console.log("WebSocket connect successfully",s)
				},
				fail:err=>{
					 that.__onClose(err);
				}
			}); //连接打开的时候触发
        }
    },
	
    send(msg) {
		return new Promise((reso,reje)=>{
			//console.log("send",msg)
			if(msg.isNeedResponse()) {
				//注册回调为消息监听
				listeners[msg.msgId] = (m)=>{
					if(typeof m == 'object') {
						reso(m)
					} else {
						reje(m)
					}
				}
			}
			this.__doSend(msg)
			if(!msg.isNeedResponse()) {
				reso({})
			}
		})
    },
	
	//大数据分块发送
	__doSend0(buffe) {
		//大数据分块发送
		let len = buffe.byteLength;
		if(len < blockSize) {
			this.__doSend1(buffe)
		} else {
			let bn = parseInt(len / blockSize)
			let idx = 0
			for(; idx < bn; idx++) {
				let st = idx*blockSize
				let b = buffe.slice(st, st+blockSize)
				this.__doSend1(b)
			}
			this.__doSend1(buffe.slice(idx*blockSize, len))
		}
	},
	
	__doSend1(buffe) {
		//更新访问时间
		auth.lastat = new Date().getTime()
		if(iswx){
			//console.log(buffe)
			uni.sendSocketMessage({data:buffe})
		}
		else {
			//console.log(buffe)
			wsk.send(buffe)
		}
		
	},
	
	__checkLoop() {
		if(sending) {
			return
		}
		
		sending = true
		while(waiting.length > 0) {
			try{
				let b = waiting.pop()
				this.__doSend0(b)
			}catch(err) {
				console.log(err)
				console.log(b)
			}
		}
		sending = false
	},
	
	__timerCheck() {
		if(waiting.length > 0 && !sending) {
			this.__checkLoop()
		}
	},
	
	__doSend(msg) {
		
		let buffe = msg.encode();
		waiting.unshift(buffe);
		
		if(isInit == 0) {
			this.__init(()=>{
				if(timer == null) {
					timer = setInterval(()=>{
						this.__timerCheck();
					},1000)
				}
				this.__checkLoop();
			});
		} else if(isInit == 2) {
			this.__checkLoop();
		}
	},
	
    registListener(type, lis) {
        if (!listeners[type]) {
            listeners[type] = lis;
        } else {
            throw 'type:' + type + ' have been exists';
        }
    }
};
