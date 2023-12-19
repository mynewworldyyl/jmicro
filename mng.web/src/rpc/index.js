import rpc from "./rpcbase.js"
//import jreq from "../utils/jreq.js"
import utils from "./utils.js"
import lc from "./localStorage.js"
import i18n from "./i18n.js"
import cons0 from "./constants.js"
import config from "./config.js"
import {Constants} from "./message.js"
import ps from "./pubsub.js"
import auth from "./auth.js"
import prf from "./profile.js"
import api from "./shop/api.js"

let cons = {
	//...cons0,
	//...Constants,
}

cons = Object.assign(cons,cons0)
cons = Object.assign(cons,Constants)

let $jr = {
	rpc,
	utils,
	lc,
	i18n,
	cons,
	config,
	ps,
	auth,
	prf,
	api,
}

let install = (Vue,vm)=>{
	Vue.prototype.$jr = $jr
}

export default {
	install
};
