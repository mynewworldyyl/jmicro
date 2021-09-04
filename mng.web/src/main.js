import Vue from 'vue'
import App from './App.vue'
import VueRouter from 'vue-router'

import iView from 'view-design'
import 'view-design/dist/styles/iview.css'

import JMicroEditor from  './components/JMicroEditor.vue'
import i18n from  '@/rpc/i18n'

import * as filters from './components/common/JFilters'
Object.keys(filters).forEach(key => {
    Vue.filter(key, filters[key])
})

/*
import JService from './components/service/JService.vue'
import JConfig from './components/config/JConfig.vue'
import JRouter from './components/route/JRouter.vue'
import JShell from './components/shell/JShell.vue'
import JLog from './components/log/JLog.vue'
import JWarning from './components/warning/JWarning.vue'
import JStatis from './components/statis/JStatis.vue'
import JMonitor from './components/monitor/JMonitor.vue'
*/

Vue.use(iView)
//Vue.use(window.jm)
Vue.config.productionTip = false

Vue.use(VueRouter)
const routes = [
   /* { path: '/config', component: JConfig },
    { path: '/router', component: JRouter },
    { path: '/shell', component: JShell },
    { path: '/log', component: JLog },
    { path: '/statisService', component: JStatis },
    { path: '/warning', component: JWarning },
    { path: '/monitors', component: JMonitor },*/
    { path: '/', component: JMicroEditor },
];

const router = new VueRouter({
    routes // short for `routes: routes`
})

//window.vue = window.jm.vue;
//window.vue.jm = window.jm;
i18n.init(()=>{
    window.jm={};
    window.jm.vue = new Vue({
        render: h => h(App),
        router,
    });
    window.jm.vue.$mount('#app')
});






