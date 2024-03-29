import { Constants } from './message';
import JDataOutput from './dataoutput';

const ApiRequest = function () {
    this.serviceName = '';
    this.namespace = '';
    this.version = '';
    this.method = '';
    this.params = {};
    this.args = [];
    this.upProtocol = Constants.PROTOCOL_JSON;
    this.doProtocol = Constants.PROTOCOL_JSON;
    this.type = Constants.MSG_TYPE_REQ_JRPC;
};

ApiRequest.prototype = {
    encode: function (protocol) {
        if (protocol == Constants.PROTOCOL_BIN) {
            let buf = new JDataOutput(1024);
			//buf.writeUtf8String(this.serviceName);
            //buf.writeUtf8String(this.namespace);
            //buf.writeUtf8String(this.version);
            //buf.writeUtf8String(this.method);

            buf.writeObject(this.params);
            buf.writeObjectArray(this.args);
            return buf.getBuf();
        } else {
            if (protocol == Constants.PROTOCOL_JSON) {
                return JSON.stringify(this);
            } else {
                throw 'Invalid protocol:' + protocol;
            }
        }
    }
};
export default ApiRequest;
