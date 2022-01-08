package cn.jmicro.ext.mongodb.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import cn.jmicro.api.RespJRso;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RespJRsoCodec implements Codec<RespJRso>{

	@Override
	public void encode(BsonWriter writer, RespJRso p, EncoderContext encoderContext) {
		writer.writeStartDocument();
		
		writer.writeInt32("code",p.getCode());
		writer.writeInt32("curPage",p.getCurPage());
		writer.writeInt32("pageSize",p.getPageSize());
		writer.writeInt32("total",p.getTotal());
		
		if(p.getKey() == null) {
			writer.writeString("key","");
		}else {
			writer.writeString("key",p.getKey());
		}
		
		if(p.getMsg() == null) {
			writer.writeString("msg","");
		}else {
			writer.writeString("msg",p.getMsg());
		}
		
		if(p.getData() != null) {
			writer.writeString("clazz",p.getData().getClass().getName());
			writer.writeString("data",JsonUtils.getIns().toJson(p.getData()));
		}else {
			writer.writeString("clazz","");
			writer.writeString("data","");
		}
		writer.writeEndDocument();
	
	}

	@Override
	public Class<RespJRso> getEncoderClass() {
		return RespJRso.class;
	}

	@Override
	public RespJRso decode(BsonReader r, DecoderContext decoderContext) {

		r.readStartDocument();
    	
		RespJRso p = new RespJRso();
   	 
   	 	p.setCode(r.readInt32("code"));
   	 	p.setCurPage(r.readInt32("curPage"));
   	 	p.setPageSize(r.readInt32("pageSize"));
   	 	p.setTotal(r.readInt32("total"));
   	 	
   	 	p.setKey(r.readString("key"));
   	 	p.setMsg(r.readString("msg"));
   	 	
   	 	String clazz = r.readString("clazz");
   	 	String val = r.readString("data");

        if(!Utils.isEmpty(clazz)) {
			try {
				Class<?> t = RespJRsoCodec.class.getClassLoader().loadClass(clazz);
				Object v = JsonUtils.getIns().fromJson(val, t);
				p.setData(v);
			} catch (ClassNotFoundException e) {
				log.error("",e);
			}
		}
      
        r.readEndDocument();
        
        return p;
	
	}
	
}
