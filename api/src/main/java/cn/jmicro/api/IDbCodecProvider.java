package cn.jmicro.api;

import org.bson.codecs.Codec;

public interface IDbCodecProvider {

	<V,T extends Codec<V>> void provide(Class<V> clazz, T ins);
}
