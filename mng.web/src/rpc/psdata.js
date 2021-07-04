import utils from "@/rpc/utils"


let PSData = function(topic,data) {

    //消息ID,唯一标识一个消息
    this.id = 0;
    this.flag = Constants.FLAG_DEFALUT;
    this.topic = topic;
    this.srcClientId = -1;
    this.data = data;
    //消息发送结果回调的RPC方法，用于消息服务器给发送者回调
    this.callback = null;
    this.context = null;

}

let Constants = {
    FLAG_DEFALUT : 0,
    FLAG_QUEUE : 1<<0,
    FLAG_PUBSUB : 0<<0,
    //1右移1位，异步方法，决定回调方法的参数类型为消息通知的返回值
    FLAG_ASYNC_METHOD : 1<<1,
    //1右移两位，消息回调通知，决定回调方法的参数类型为消息通知的返回值分别为 消息发送状态码，消息ID，消息上下文透传
    FLAG_MESSAGE_CALLBACK : 1<<2,
    FLAG_PERSIST : 1<<3,
    FLAG_CALLBACK_TOPIC : 1<<4,
    FLAG_CALLBACK_METHOD : 0<<4,

    RESULT_SUCCCESS : 0,

    PUB_OK : 0,
    //无消息服务可用,需要启动消息服务
    PUB_SERVER_NOT_AVAILABALE : -1,
    //消息队列已经满了,客户端可以重发,或等待一会再重发
    PUB_SERVER_DISCARD : -2,
    //消息服务线程队列已满,客户端可以重发,或等待一会再重发,可以考虑增加消息服务线程池大小,或增加消息服务
    PUB_SERVER_BUSUY : -3,

    PUB_TOPIC_INVALID: -4,

    //消息服务器不可用
    RESULT_FAIL_SERVER_DISABLE : -5,

    //发送给消息订阅者失败
    RESULT_FAIL_DISPATCH : -6,

    //回调结果通知失败
    RESULT_FAIL_CALLBACK : -7,

}

PSData.prototype.isPersist = function() {
    return utils.flagIs(this.flag,Constants.FLAG_PERSIST);
}

PSData.prototype.setPersist = function(f) {
    this.flag = utils.flagSet(f,this.flag,Constants.FLAG_PERSIST);
}

PSData.prototype.queue = function() {
    this.flag =utils .flagSet(true,this.flag,Constants.FLAG_QUEUE);
}

PSData.prototype.pubsub = function() {
    this.flag = utils.flagSet(false,this.flag,Constants.FLAG_PUBSUB);
}

PSData.prototype.callbackTopic = function() {
    this.flag = utils.flagSet(true,this.flag,Constants.FLAG_CALLBACK_TOPIC);
}

PSData.prototype.callbackMethod = function() {
    this.flag = utils.flagSet(false,this.flag,Constants.FLAG_CALLBACK_METHOD);
}

PSData.prototype.isCallbackTopic = function() {
    return utils.flagIs(this.flag,Constants.FLAG_CALLBACK_TOPIC);
}

PSData.prototype.isCallbackMethod = function() {
    return !utils.flagIs(this.flag,Constants.FLAG_CALLBACK_TOPIC);
}

PSData.prototype.isQueue = function() {
    return utils.flagIs(this.flag,Constants.FLAG_QUEUE);
}

PSData.prototype.isPubsub = function() {
    return !utils.flagIs(this.flag,Constants.FLAG_QUEUE);
}

export default PSData;