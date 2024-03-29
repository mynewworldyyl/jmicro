import { Constants } from './message';
import JDataInput from './datainput';
import utils from './utils';

const ApiResponse = function () {
    this.id = -1;
    this.msg = null;
    this.result = null;
    this.success = true;
};

ApiResponse.prototype = {
    decode: function (arrayBuf, protocol) {
        if (protocol == Constants.PROTOCOL_BIN) {
            let dataInput = new JDataInput(arrayBuf);
            this.id = dataInput.readLong();
            this.success = dataInput.readUByte() > 0;
            this.result = [];
            let len = dataInput.remaining();

            for (let i = 0; i < len; i++) {
                this.result.push(dataInput.readUByte());
            }
        } else {
            if (protocol == Constants.PROTOCOL_JSON) {
                if (arrayBuf instanceof Array || arrayBuf instanceof ArrayBuffer) {
                    let dataInput = new JDataInput(arrayBuf);
                    let byteArray = [];
                    let len = dataInput.remaining();

                    for (let i = 0; i < len; i++) {
                        byteArray.push(dataInput.readUByte());
                    }

                    let jsonStr = utils.fromUTF8Array(byteArray);
                    let o = JSON.parse(jsonStr);

                    if (o) {
                        this.id = o.id;
                        this.success = o.success;
                        this.result = o.result;
                    }
                }
            } else {
                throw 'Invalid protocol:' + protocol;
            }
        }
    }
};
export default ApiResponse;
