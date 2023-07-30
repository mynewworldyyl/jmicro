package cn.jmicro.api.iot;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.codec.ISerializeObject;
import cn.jmicro.api.codec.JDataInput;
import cn.jmicro.api.codec.JDataOutput;
import lombok.Data;

@IDStrategy(100)
@Data
public class IotDeviceVoJRso  implements Serializable, ISerializeObject{
	
	private int  id;
	
	private String deviceId;
	
	private int srcClientId;
	
	//设备所属账号ID
	private int srcActId;
	
	private long lastActiveTime;
	
	private Boolean isMaster;
	
	@Override
	public void encode(DataOutput buffer) throws IOException {
		JDataOutput out = (JDataOutput)buffer;
		out.writeInt(id);
		out.writeInt(this.srcActId);
		out.writeInt(this.srcClientId);
		out.writeUTF(deviceId);
		out.writeLong(this.lastActiveTime);
		out.writeBoolean(isMaster);
	}

	@Override
	public void decode(DataInput buffer) throws IOException {
		JDataInput in = (JDataInput)buffer;
		this.id = in.readInt();
		this.srcActId = in.readInt();
		this.srcClientId = in.readInt();
		this.deviceId = in.readUTF();
		this.lastActiveTime = in.readLong();
		this.isMaster = in.readBoolean0();
	}
	
}
