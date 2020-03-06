package org.jmicro.pubsub;

import org.jmicro.api.annotation.PostListener;
import org.jmicro.api.codec.TypeCoderFactory;
import org.jmicro.api.objectfactory.PostFactoryAdapter;
import org.jmicro.api.objectfactory.IObjectFactory;

@PostListener
public class InitPubsubServer extends PostFactoryAdapter {

	@Override
	public void preInit(IObjectFactory of) {
		TypeCoderFactory.registClass(SendItem.class);
		
	}
}
