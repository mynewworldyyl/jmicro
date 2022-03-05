import auth from "@/rpc/auth"

export default{
  inserted(el, binding, vnode) {
	  
	if (!auth.hasRole(binding.value)) {
        el.parentNode && el.parentNode.removeChild(el)
      }
  }
}