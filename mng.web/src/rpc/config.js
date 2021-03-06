const ADMIN_MODEL_PER = '*'
const ADMIN_OP_PER = -1

export default {
   /* cache:{},
    ip:process.env.ip,
    port: process.env.port,
    txtContext : process.env.txtContext,
    binContext : process.env.binContext,
    httpContext : process.env.httpContext,
    useWs : process.env.useWs,
    sslEnable:process.env.sslEnable,*/

    ip:'192.168.56.1',
    port:'9090',
    //port:'80',
    txtContext : '_txt_',
    binContext : '_bin_',
    httpContext : '/_http_',
    useWs : true,

    sslEnable:false,
    publicKey :'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCt489YTxmLjNVxfFKSORyUgXjr65MQR1a/QdlriFEWXUAaLpVWP41YTlSA5ecG54xVwl2ayLytCv4CJNqYPeYNPUVXPr1tqND1aZYK9iUQQ0K36g2QZaigg+f/NJSY6w4XITQdBz3PnJOOzOK+cOew4R0XiyrR8sHG2Is4Mf9qowIDAQAB',
    privateKey:'',

    includeMethod:true,

    clientId: 1,//system client id

    adminModelPerm : ADMIN_MODEL_PER,
    adminOpPerm: ADMIN_OP_PER,

}
