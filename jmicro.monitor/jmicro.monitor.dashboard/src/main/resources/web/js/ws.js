var jmicro = jmicro || {};

jmicro.config ={
  ip:"192.168.3.3",
  port:'9992',
  context:'ws',
}

jmicro.socket = {
    listeners : {}
    ,logData:null
    ,init : function() {
            var url = 'ws://' + jmicro.config.ip + ':' + jmicro.config.port +'/'+ jmicro.config.context;
            var self = this;
            if(window.WebSocket){
              self.wsk = new WebSocket(url);  //获得WebSocket对象

             //当有消息过来的时候触发
             self.wsk.onmessage = function(event){
               console.log(event.data);
               var msg = JSON.parse(event.data);
               self.listeners[msg.type].onMessage(msg);
             }

             //连接关闭的时候触发
              self.wsk.onclose = function(event){
               console.log("connection close");
            }

            //连接打开的时候触发
              self.wsk.onopen = function(event){
              console.log("connect successfully");
            }
        }else{
          alert("浏览器不支持WebSocket");
        }
    }

    ,emit : function(data) {
      this.wsk.send(JSON.stringify(data));
    }

}

