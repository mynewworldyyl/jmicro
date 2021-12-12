<template>
    <div class="JTesting" id="testing">
        <Button @click="loadServerList('threadPoolSrv','serverList','all')">threadPoolSrv.serverList</Button>
        <Button @click="loadServerList('moType','getNamedList')">moType.getNamedList</Button>
        <Button @click="loadServerList('moType','getMonitorKeyList')">moType.getMonitorKeyList</Button>
        <Button @click="loadServerList('monitor','serverList')">monitor.serverList</Button>
        <Button @click="loadServerList('conf','getChildren','/jmicro/JMICRO',true)">conf.getChildren</Button>
        <Button @click="loadServerList('comm','getDicts',['logKey2Val'])">comm.getDicts</Button>
        <Button @click="loadServerList('i18n','init',[])">i18n.init</Button>
        <Button @click="loadServerList('ISimpleRpc','hello','Test hello')">ISimplethis.$jr.rpchello</Button>
        <br/>
       <!-- <JCustTree></JCustTree>-->
    </div>
</template>

<script>

   /* import JCustTree from '../common/JCustTree.vue'*/

    export default {
        name: 'JTesting',
        components:{
          /*  JCustTree*/
        },

        data () {
            return {
                term: null,
                terminalSocket: null
            }
        },

        methods: {
            loadServerList(module,method,arg0,arg1,arg2,arg3) {
                //let self = this;
              let p =  window.jm.mng[module][method](arg0,arg1,arg2,arg3)
                    if(p) {
                        p.then((resp) => {
                            //self.$Message.info(JSON.stringify(resp));
                            console.log(resp);
                        }).catch((err) => {
                            window.console.log(err);
                        });
                    }
            }
        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
        },

        beforeDestroy () {

        }
    }
</script>

<style>
    .JTesting{

    }

</style>