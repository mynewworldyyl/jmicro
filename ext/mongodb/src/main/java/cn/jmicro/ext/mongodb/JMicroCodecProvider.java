package cn.jmicro.ext.mongodb;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import cn.jmicro.ext.mongodb.codec.BigDecimalCodec;
import cn.jmicro.ext.mongodb.codec.LocalDateCodec;
import cn.jmicro.ext.mongodb.codec.LocalDateTimeCodec;
import cn.jmicro.ext.mongodb.codec.LocalTimeCodec;

public class JMicroCodecProvider implements CodecProvider{

	private final Map<Class<?>, Codec<?>> codecs = new HashMap<Class<?>, Codec<?>>();
	
	public JMicroCodecProvider(){
		codecs.put(LocalDateTime.class, new LocalDateTimeCodec());
		codecs.put(LocalTime.class, new LocalTimeCodec());
		codecs.put(LocalDate.class, new LocalDateCodec());
		codecs.put(BigDecimal.class, new BigDecimalCodec());
	}
	
	@Override
	public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
		return (Codec<T>)codecs.get(clazz);
	}

}
