package cn.jmicro.ext.mongodb.codec;

import static java.lang.String.format;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;

public class LocalDateCodec implements Codec<LocalDate>{

	@Override
	public void encode(BsonWriter writer, LocalDate value, EncoderContext encoderContext) {
		 try {
			 writer.writeDateTime(value.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
	        } catch (ArithmeticException e) {
	            throw new CodecConfigurationException(format("Unsupported LocalDateTime value '%s' could not be converted to milliseconds: %s",
	                    value, e.getMessage()), e);
	        }
	}

	@Override
	public Class<LocalDate> getEncoderClass() {
		return LocalDate.class;
	}

	@Override
	public LocalDate decode(BsonReader reader, DecoderContext decoderContext) {
		BsonType currentType = reader.getCurrentBsonType();
		if(currentType.equals(BsonType.DATE_TIME)) {
			 return Instant.ofEpochMilli(reader.readDateTime()).atZone(ZoneOffset.UTC).toLocalDate();
		}else if(currentType.equals(BsonType.DOCUMENT)) {
			//兼容Document.toJSON()
			reader.readStartDocument();
			
			int y = 0;
			int m = 0;
			int d = 0;
			
			String n = reader.readName();
			//reader.readBsonType();
			if("date".equals(n)) {
				reader.readStartDocument();
				 y = reader.readInt32("year");
				 m = reader.readInt32("month");
				 d = reader.readInt32("day");
				 reader.readEndDocument();
			}
			reader.readEndDocument();
			return LocalDate.of(y, m, d);
			
		}else {
            throw new CodecConfigurationException(format("Could not decode into %s, expected '%s' BsonType but got '%s'.",
                    getEncoderClass().getSimpleName(), LocalDateTime.class.getName(), currentType));
        
		}
	}
	
}
