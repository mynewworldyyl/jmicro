/* eslint-disable */
import config from "./config"
import socket from "./socket"
import {Message,Constants} from "./message";
import ApiResponse from "./response"
import ps from "./pubsub"
import rpc from "./rpcbase"
import utils from "./utils"

let url = config.protocol + '://'+ config.ip + ':' + config.port +'/'+ config.httpContext;

let iswx = utils.isWx();

function decodeMessage(respBuff,cb) {
    let respMsg = new  Message();
    respMsg.decode(respBuff);
    if(respMsg.type == Constants.MSG_TYPE_ASYNC_RESP) {
        if(!respMsg.success) {
            throw 'fail:' + respMsg.payload
        }
        ps.onMsg(respMsg.payload)
    } else  {
        if(respMsg.getDownProtocol() == Constants.PROTOCOL_BIN) {
            let resp = new ApiResponse()
            resp.decode(respMsg.payload, respMsg.getDownProtocol())
            respMsg.payload = resp
        }
        cb(respMsg);
    }
}

export default {

    send : function(msg,cb){
        if(config.useWs && !utils.isWx()){
            socket.send(msg,cb);
        } else {
            let buff = msg.encode();
            if(iswx) {
                //微信平台

                let h = {'Content-Type': 'application/json'}
                if(rpc.actInfo) {
                    h[Constants.TOKEN] = rpc.actInfo.loginKey;
                }

                wx.request({
                    url: url,
                    data: buff,
                    method: 'POST',
                    responseType:'arraybuffer',
                    dataType:'其他',
                    header:h,
                    timeout:10000,
                    success: function(res) {
                        if (res.statusCode == 200) {
                            decodeMessage(res.data,cb)
                        } else {
                            console.log(res)
                            cb(null,res.errMsg);
                        }
                    },
                    fail: function(err) {
                        console.log(err)
                        cb(null,err);
                    }
                })

            } else {
                //非微信平台
                let xhr = new XMLHttpRequest();
                xhr.responseType='arraybuffer';
                xhr.onload = function() {
                    if(xhr.readyState == 4 ) {
                        if(xhr.status == 200) {
                            decodeMessage(xhr.response,cb)
                        }else {
                            cb(null,xhr.statusText);
                        }
                    }
                }
                xhr.open('POST',url,true);
                xhr.send(buff);
            }
        }
    }
}