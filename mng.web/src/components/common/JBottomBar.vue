<template>
    <div id="bottomBar" class="JBottomBar" ref="bottomBar" :style="{top:btnTop}">
        <a target="_blank" class="icp" href="http://beian.miit.gov.cn"><pre>粤ICP备2020078757号</pre></a>
        <a target="_blank" href="http://www.beian.gov.cn/portal/registerSystemInfo?recordcode=44030602004897"
           class="ba">
            <img src="ba.png" style="float:left;"/>
            <pre>粤公网安备 44030602004897号</pre>
        </a>
        <p class="jCopyRight">© CopyRight 2016-{{year()}}, JMICRO.CN, Inc.All Rights Reserved</p>
        <!--<p><a target="_blank" href="https://github.com/mynewworldyyl/jmicro">Source Code</a></p>-->
    </div>
</template>

<script>

export default {
    name: 'JBottomBar',

    components: {

    },

    data() {
        return {
            bb:null,
            btnTop:0,
        };
    },

    updated() {

    },

    mounted(){
        //给页面绑定滑轮滚动事件
        /* if (window.document.addEventListener) {
             window.document.addEventListener('DOMMouseScroll', this.scrollFunc, false);
         }*/
        //滚动滑轮触发scrollFunc方法
        //window.onmousewheel = document.onmousewheel = this.scrollFunc;
        this.bb = document.getElementById("bottomBar");
        //this.bb.previousElementSibling.style.minHeight=(document.body.clientHeight-67)+'px';
        //this.bb.parentElement.firstChild.style.minHeight=(document.body.clientHeight-67)+'px';
        // this.bb.style.position='relative';
        //this.bb.style.width=document.body.clientWidth+'px';
        if(document.body.scrollHeight < document.body.clientHeight) {
            this.btnTop = (document.body.clientHeight-67)+'px';
        }else {
            this.btnTop = (document.body.scrollHeight-document.body.clientHeight)+'px';
        }
    },

    methods: {
        year() {
            return new Date().format('yyyy');
        },

        scrollFunc(e0) {

            let timer = null;

            let val = 0;
            let e = e0 || window.event;
            if (e.wheelDelta) {  //判断浏览器IE，谷歌滑轮事件
                val = e.wheelDelta;
            } else if (e.detail) {  //Firefox滑轮事件
                val = e.detail;
            }
            //console.log(-val);
            if(val > 0) {
                //bb.style.bottom='-80px';
                //console.log(-val);
                let p = parseInt(this.bb.style.bottom);
                if(!timer && p != -80) {
                    timer = setInterval(function(){
                        p = parseInt(this.bb.style.bottom);
                        if(p >= -80) {
                            p = p-5;
                            this.bb.style.bottom=p+"px";
                            //console.log(p);
                        }else {
                            this.bb.style.bottom="-80px";
                            clearTimeout(timer);
                            timer  = null;
                        }
                    },10);
                }
            } else {
                let p = parseInt(this.bb.style.bottom);
                if(!timer && p != 0) {
                    timer = setInterval(function(){
                        p = parseInt(this.bb.style.bottom);
                        if(p<=0) {
                            p = p+5;
                            this.bb.style.bottom=p+"px";
                            //console.log(p);
                        }else {
                            this.bb.style.bottom="0px";
                            clearTimeout(timer);
                            timer  = null;
                        }
                    },10);
                }
            }
        },
    },
}

</script>

<style scoped>

    .icp , .ba{
        display:inline-block;text-decoration:none;height:20px;line-height:20px;
        color: black;
    }

    .jCopyRight{
        color: black;
    }

    .JBottomBar {
        width: 100%;
        margin: 0 auto;
        position: relative;
        text-align: center;
        height:auto;
        padding:10px;
        line-height: 25px;
        background-color: lightgray;
    }

</style>
