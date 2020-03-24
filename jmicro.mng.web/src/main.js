import Vue from 'vue'
import App from './App.vue'
import VueRouter from 'vue-router'

import iView from 'view-design'
import 'view-design/dist/styles/iview.css'

import JConfig from './components/config/JConfig.vue'
import JService from './components/service/JService.vue'


Vue.use(iView)
Vue.use(VueRouter)
Vue.use(window.jm)

Vue.config.productionTip = false

const routes = [
    { path: '/config', component: JConfig },
    { path: '/', component: JService },

];

const router = new VueRouter({
    routes // short for `routes: routes`
})


window.jm.vue = new Vue({
    render: h => h(App),
    router,
});

//window.vue = window.jm.vue;
//window.vue.jm = window.jm;

window.jm.vue.$mount('#app')

