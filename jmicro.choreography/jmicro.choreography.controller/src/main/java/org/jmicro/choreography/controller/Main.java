package org.jmicro.choreography.controller;

import org.jmicro.api.JMicro;

public class Main {
	public static void main(String[] args) {
		JMicro.getObjectFactoryAndStart(new String[]{"-DinstanceName=ServiceSchedulerImpl"});
	}
}
