package cn.jmicro.ext.mongodb.codec;

import java.math.BigDecimal;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Decimal128;

public class BigDecimalCodec  implements Codec<BigDecimal> {

	 	@Override
	    public void encode(final BsonWriter writer, final BigDecimal value, final EncoderContext encoderContext) {
	        writer.writeDecimal128(new Decimal128(value));
	    }

	    @Override
	    public BigDecimal decode(final BsonReader reader, final DecoderContext decoderContext) {
	    	if(reader.getCurrentBsonType() == BsonType.DOUBLE) {
	    		 return new BigDecimal(reader.readDouble());
	    	}else {
	    		 return reader.readDecimal128().bigDecimalValue();
	    	}
	    }

	    @Override
	    public Class<BigDecimal> getEncoderClass() {
	        return BigDecimal.class;
	    }
}
