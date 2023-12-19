/* eslint-disable */
import config from './config';
import socket from './socket';
import { Message, Constants } from './message';
import ApiResponse from './response';
import ps from './pubsub';
import rpc from './rpcbase';
import utils from './utils';
import auth from './auth';
let url = config.httpUrl();
let iswx = utils.isUni();

function decodeMessage(respBuff, reso,reje) {
    let respMsg = new Message();
    respMsg.decode(respBuff);
    if(respMsg.type == Constants.MSG_TYPE_ASYNC_RESP) {
        if (!respMsg.success) {
			if(reje) {
				reje(respMsg.payload)
			}
        } else {
			ps.onMsg(respMsg.payload);
		}
    } else {
        if (respMsg.getDownProtocol() == Constants.PROTOCOL_BIN) {
            let resp = new ApiResponse();
            resp.decode(respMsg.payload, respMsg.getDownProtocol());
            respMsg.payload = resp;
        }
		if(reso) {
			reso(respMsg);
		}
    }
}

export default {
    send(msg) {
        if (config.useWs) {
            return socket.send(msg);
        } else {
			return new Promise((reso,reje)=>{
				let buff = msg.encode();
				if (iswx) {
				    //微信平台
				    let h = {
				        'Content-Type': 'application/json'
				    };
				
				    if (auth.actInfo) {
				        h[Constants.TOKEN] = auth.actInfo.loginKey;
				    }
				
				    uni.request({
				        url: url,
				        data: buff,
				        method: 'POST',
				        responseType: 'arraybuffer',
				        dataType: '其他',
				        header: h,
				        timeout: 10000,
				        success: function (res) {
				            if (res.statusCode == 200) {
				                decodeMessage(res.data, reso,reje);
				            } else {
				                console.log(res);
				                reje(res)
				            }
				        },
				        fail: function (err) {
				            console.log(err);
				             reje(err)
				        }
				    });
				} else {
				    //非微信平台
				    let xhr = new XMLHttpRequest();
				    xhr.responseType = 'arraybuffer';
				
				    xhr.onload = function () {
				        if (xhr.readyState == 4) {
				            if (xhr.status == 200) {
				                decodeMessage(xhr.response, reso,reje);
				            } else {
				                //cb(null, xhr.statusText);
								reje(xhr.statusText)
				            }
				        }
				    };
				
				    xhr.open('POST', url, true);
				    xhr.send(buff);
				}
			})
        }
    }
};
