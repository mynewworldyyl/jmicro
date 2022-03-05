/* eslint-disable */
import Vue from 'vue'
import App from './App.vue'
import VueRouter from 'vue-router'

import iView from 'view-design'
import 'view-design/dist/styles/iview.css'

import JMicroEditor from  './components/JMicroEditor.vue'
import rpc from  '@/rpc/rpcbase'

import * as filters from './components/common/JFilters'
Object.keys(filters).forEach(key => {
    Vue.filter(key, filters[key])
})

import perm from  './components/directive/perm.js'
import role from  './components/directive/role.js'

Vue.directive('perm', perm)
Vue.directive('role', role)

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

import Element from 'element-ui'
Vue.use(Element)

Vue.use(iView)
//Vue.use(window.jm)
Vue.config.productionTip = false

import jr from "./rpc/index.js"
import './plugins/element.js'
Vue.use(jr)

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

rpc.init({
		mod: 'jmng',useWs: true,clientId: 1,
		loginPage:'/pages/auth/accountLogin/accountLogin'
	},
	(isLogin) => {
		console.log("login rst: " + isLogin)
		let vue = new Vue({
			  beforeCreate(){
				  Vue.prototype.$bus = this
				  console.log("set event bus to:",this)
				},
			  render: h => h(App),
			  router,
		 });
		 vue.$mount('#app')
	}
);



//window.vue = this;
//window.vue.jm = window.jm;







