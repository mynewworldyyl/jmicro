import {Constants} from "@/rpc/message"
import JDataOutput from "@/rpc/dataoutput"

let reqId = 1;

const ApiRequest = function() {
    this.reqId = reqId++;
    this.serviceName = '';
    this.namespace = '';
    this.version = '';
    this.method = '';
    this.params = {};

    this.args = [];

}

ApiRequest.prototype = {
    encode : function(protocol) {
        if(protocol == Constants.PROTOCOL_BIN) {
            let buf =  new JDataOutput(1024);
            buf.writeUnsignedLong(reqId);
            //buf.writeUtf8String(this.serviceName);
            //buf.writeUtf8String(this.namespace);
            // buf.writeUtf8String(this.version);
            //buf.writeUtf8String(this.method);
            buf.writeObject(this.params);
            buf.writeObjectArray(this.args);
            return buf.getBuf();
        } else if(protocol == Constants.PROTOCOL_JSON)  {
            return JSON.stringify(this);
        }else {
            throw 'Invalid protocol:'+protocol;
        }
    }
}

export default ApiRequest;