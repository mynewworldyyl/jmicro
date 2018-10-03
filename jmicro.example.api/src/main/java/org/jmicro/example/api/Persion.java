package org.jmicro.example.api;

import java.io.Serializable;

public class Persion implements Serializable{

	private static final long serialVersionUID = 23424242331L;
	
	private String username ="";
	private int id = 222;
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	@Override
	public String toString() {
		return "ID: " + this.id+", username: " + this.username;
	}
	
}
