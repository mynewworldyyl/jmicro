import auth from "@/rpc/auth"

export default{
  inserted(el, binding, vnode) {
	if (!auth.hasPerm(binding.value)) {
        el.parentNode && el.parentNode.removeChild(el)
      }
  }
}