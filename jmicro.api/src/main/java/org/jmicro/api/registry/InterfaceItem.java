package org.jmicro.api.registry;

import java.util.HashSet;
import java.util.Set;

public class InterfaceItem {

	private String interfaceName;
	
	private Set<ServiceMethod> methods = new HashSet<>();
	
	public ServiceMethod getMethod(String methodName,String paramTypesStr){
		for(ServiceMethod sm : this.methods){
			if(methodName.equals(sm.getMethodName()) && paramTypesStr.equals(sm.getMethodParamTypes())){
				return sm;
			}
		}
		return null;
	}
	
	public ServiceMethod getMethod(String methodName,Object[] args){
		String mk = ServiceMethod.methodParamsKey(args);
		for(ServiceMethod sm : this.methods){
			if(methodName.equals(sm.getMethodName()) && mk.equals(sm.getMethodParamTypes())){
				return sm;
			}
		}
		return null;
	}
	
	public void addMethod(ServiceMethod sm){
		methods.add(sm);
	}
	
	public Set<ServiceMethod> getMethods(){
		return methods;
	}

	public String getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	public void setMethods(Set<ServiceMethod> methods) {
		this.methods = methods;
	}
	
	
}
