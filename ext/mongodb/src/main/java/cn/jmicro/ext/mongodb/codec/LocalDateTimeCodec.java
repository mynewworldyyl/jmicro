package cn.jmicro.ext.mongodb.codec;

import static java.lang.String.format;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;

import cn.jmicro.api.utils.DateUtils;

public class LocalDateTimeCodec implements Codec<LocalDateTime>{

	@Override
	public void encode(BsonWriter writer, LocalDateTime value, EncoderContext encoderContext) {
		 try {
	            writer.writeInt64(value.toInstant(ZoneOffset.UTC).toEpochMilli());
	        } catch (ArithmeticException e) {
	            throw new CodecConfigurationException(format("Unsupported LocalDateTime value '%s' could not be converted to milliseconds: %s",
	                    value, e.getMessage()), e);
	        }
	}

	@Override
	public Class<LocalDateTime> getEncoderClass() {
		return LocalDateTime.class;
	}

	@Override
	public LocalDateTime decode(BsonReader reader, DecoderContext decoderContext) {
		BsonType currentType = reader.getCurrentBsonType();
		if(currentType.equals(BsonType.DATE_TIME)) {
			 return Instant.ofEpochMilli(reader.readDateTime()).atZone(ZoneOffset.UTC).toLocalDateTime();
		}else if(currentType.equals(BsonType.DOCUMENT)) {
			//兼容Document.toJSON()
			reader.readStartDocument();
			
			int y = -1;
			int m = 0;
			int d = 0;
			int h =0;
			int mi =0;
			int s = 0;
			int no = 0;
			
			String n = reader.readName();
			//reader.readBsonType();
			if("date".equals(n)) {
				if(reader.getCurrentBsonType() != BsonType.NULL) {
					 reader.readStartDocument();
					 y = reader.readInt32("year");
					 m = reader.readInt32("month");
					 d = reader.readInt32("day");
					 reader.readEndDocument();
				}else {
					reader.readNull();
				}
				
			}
			
			n = reader.readName();
			//reader.readBsonType();
			if("time".equals(n)) {
				if(reader.getCurrentBsonType() != BsonType.NULL) {
					 reader.readStartDocument();
					 h = reader.readInt32();
					 mi = reader.readInt32();
					 s = reader.readInt32();
					 no = reader.readInt32();
					 reader.readEndDocument();
				}else {
					reader.readNull();
				}
			}
			
			reader.readEndDocument();
			
			if(y > 0) {
				return LocalDateTime.of(y, m, d, h, mi, s, no);
			}else {
				return null;
			}
		}else if(currentType.equals(BsonType.INT64)) {
			 return Instant.ofEpochMilli(reader.readInt64()).atZone(ZoneOffset.UTC).toLocalDateTime();
		} else if(currentType.equals(BsonType.STRING)) {
			String str = reader.readString();
			Date d = DateUtils.parseDate(str, DateUtils.PATTERN_YYYY_MM_DD_HHMMSSSSST);
			return Instant.ofEpochMilli(d.getTime()).atZone(ZoneOffset.UTC).toLocalDateTime();
		} else {
            throw new CodecConfigurationException(format("Could not decode into %s, expected '%s' BsonType but got '%s'.",
                    getEncoderClass().getSimpleName(), LocalDateTime.class.getName(), currentType));
		}
	}
	
}
