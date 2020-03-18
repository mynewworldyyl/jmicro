import Vue from 'vue'
import App from './App.vue'

//import jm from '../public/js/jm.js'

import iView from 'view-design'
import 'view-design/dist/styles/iview.css'

Vue.use(iView)

Vue.config.productionTip = false

window.jm.vue = new Vue({
  render: h => h(App),
});
window.jm.vue .$mount('#app')
