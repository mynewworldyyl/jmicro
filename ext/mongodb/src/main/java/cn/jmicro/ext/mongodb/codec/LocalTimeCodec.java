package cn.jmicro.ext.mongodb.codec;

import static java.lang.String.format;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;

public class LocalTimeCodec implements Codec<LocalTime>{

	@Override
	public void encode(BsonWriter writer, LocalTime value, EncoderContext encoderContext) {
		 try {
			 writer.writeDateTime(value.atDate(LocalDate.ofEpochDay(0L)).toInstant(ZoneOffset.UTC).toEpochMilli());
	        } catch (ArithmeticException e) {
	            throw new CodecConfigurationException(format("Unsupported LocalDateTime value '%s' could not be converted to milliseconds: %s",
	                    value, e.getMessage()), e);
	        }
	}

	@Override
	public Class<LocalTime> getEncoderClass() {
		return LocalTime.class;
	}

	@Override
	public LocalTime decode(BsonReader reader, DecoderContext decoderContext) {
		BsonType currentType = reader.getCurrentBsonType();
		if(currentType.equals(BsonType.DATE_TIME)) {
			return Instant.ofEpochMilli(reader.readDateTime()).atOffset(ZoneOffset.UTC).toLocalTime();
		}else if(currentType.equals(BsonType.DOCUMENT)) {
			//兼容Document.toJSON()
			reader.readStartDocument();
			int h =0;
			int mi =0;
			int s = 0;
			int no = 0;
			
			String n = reader.readName();
			//reader.readBsonType();
			if("time".equals(n)) {
				 reader.readStartDocument();
				 h = reader.readInt32();
				 mi = reader.readInt32();
				 s = reader.readInt32();
				 no = reader.readInt32();
				 reader.readEndDocument();
			}
			
			reader.readEndDocument();
			
			return LocalTime.of(h, mi, s, no);
			
		}else {
            throw new CodecConfigurationException(format("Could not decode into %s, expected '%s' BsonType but got '%s'.",
                    getEncoderClass().getSimpleName(), LocalDateTime.class.getName(), currentType));
        
		}
	}
	
}
