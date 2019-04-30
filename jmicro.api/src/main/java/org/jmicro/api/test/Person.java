package org.jmicro.api.test;

import java.io.Serializable;

import org.jmicro.api.annotation.SO;

@SO
public final class Person implements Serializable{
	
	private String username ="Yeu";
	private int id = 222;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	@Override
	public String toString() {
		return "ID: " + this.id+", username: " + this.username;
	}
	
}
