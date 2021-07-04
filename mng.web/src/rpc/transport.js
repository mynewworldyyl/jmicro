import config from "@/rpc/config"
import socket from "@/rpc/socket"
import {Message,Constants} from "@/rpc/message";
import ApiResponse from "@/rpc/response"
import ps from "@/rpc/pubsub"

let httpContext = 'http://' + config.ip + ':' + config.port +'/'+ config.httpContext;

export default {

    send : function(msg,cb){
        if(config.useWs){
            socket.send(msg,cb);
        } else {
            let buff = msg.encode();
            let xhr = new XMLHttpRequest();
            xhr.responseType='arraybuffer';
            xhr.onload = function() {
                if(xhr.readyState == 4 ) {
                    if(xhr.status == 200) {
                        let respBuff = xhr.response;
                        let respMsg = new  Message();
                        respMsg.decode(respBuff);
                        if(respMsg.type == Constants.MSG_TYPE_ASYNC_RESP) {
                            if(!respMsg.success) {
                                throw 'fail:' + respMsg.payload;
                            }
                            ps.onMsg(respMsg.payload);
                        } else  {
                            if(respMsg.getDownProtocol() == Constants.PROTOCOL_BIN) {
                                let resp = new ApiResponse();
                                resp.decode(respMsg.payload, respMsg.getDownProtocol());
                                respMsg.payload = resp;
                            }
                            cb(respMsg);
                        }
                    }else {
                        cb(null,xhr.statusText);
                    }
                }
            }
            xhr.open('POST',httpContext,true);
            xhr.send(buff);
        }
    }
}