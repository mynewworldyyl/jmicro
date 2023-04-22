/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.jmicro.api.codec;

import cn.jmicro.common.CommonException;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:01:07
 */
public class DecoderConstant {
	
	public static byte PREFIX_TYPE_ID = -128;
	
	//空值编码
	public static final byte PREFIX_TYPE_NULL = PREFIX_TYPE_ID++; //-128
	
	//FINAL
	public static final byte PREFIX_TYPE_FINAL = PREFIX_TYPE_ID++; //-127

	//类型编码写入编码中
	public static final byte PREFIX_TYPE_SHORT = PREFIX_TYPE_ID++;//-126
	//全限定类名作为前缀串写入编码中
	public static final byte PREFIX_TYPE_STRING = PREFIX_TYPE_ID++;//-125
	
	//以下对高使用频率非final类做快捷编码
	
	//列表类型编码，指示接下业读取一个列表，取列表编码器直接解码
	public static final byte PREFIX_TYPE_LIST = PREFIX_TYPE_ID++;//-124
	//集合类型编码，指示接下来读取一个集合，取SET编码器直接解码
	public static final byte PREFIX_TYPE_SET = PREFIX_TYPE_ID++;//-123
	//Map类型编码，指示接下来读取一个Map，取Map编码器直接解码
	public static final byte PREFIX_TYPE_MAP = PREFIX_TYPE_ID++;//-122
	
	public static final byte PREFIX_TYPE_BYTE = PREFIX_TYPE_ID++;//-121
	public static final byte PREFIX_TYPE_SHORTT = PREFIX_TYPE_ID++;//-120
	public static final byte PREFIX_TYPE_INT = PREFIX_TYPE_ID++;//-119
	public static final byte PREFIX_TYPE_LONG = PREFIX_TYPE_ID++;//-118
	public static final byte PREFIX_TYPE_FLOAT = PREFIX_TYPE_ID++;//-117
	public static final byte PREFIX_TYPE_DOUBLE = PREFIX_TYPE_ID++;//-116
	public static final byte PREFIX_TYPE_CHAR = PREFIX_TYPE_ID++;//-115
	public static final byte PREFIX_TYPE_BOOLEAN = PREFIX_TYPE_ID++;//-114
	public static final byte PREFIX_TYPE_STRINGG = PREFIX_TYPE_ID++;//-113
	public static final byte PREFIX_TYPE_DATE = PREFIX_TYPE_ID++;//-112
	public static final byte PREFIX_TYPE_BYTEBUFFER = PREFIX_TYPE_ID++;//-111
	public static final byte PREFIX_TYPE_REQUEST = PREFIX_TYPE_ID++;//-110
	public static final byte PREFIX_TYPE_RESPONSE = PREFIX_TYPE_ID++;//-109
	public static final byte PREFIX_TYPE_PROXY = PREFIX_TYPE_ID++;//-108
	
	@SuppressWarnings("unchecked")
	public static final <T> T getValFromString(Short type, String strVal) {
		if(PREFIX_TYPE_BYTE == type) {
			return (T)new Byte(Byte.parseByte(strVal));
		}else if(PREFIX_TYPE_SHORTT == type) {
			return (T)new Short(Short.parseShort(strVal));
		}else if(PREFIX_TYPE_INT == type) {
			return (T)new Integer(Integer.parseInt(strVal));
		}else if(PREFIX_TYPE_LONG == type) {
			return (T)new Long(Long.parseLong(strVal));
		}else if(PREFIX_TYPE_FLOAT == type) {
			return (T)new Float(Float.parseFloat(strVal));
		}else if(PREFIX_TYPE_DOUBLE == type) {
			return (T)new Double(Double.parseDouble(strVal));
		}else if(PREFIX_TYPE_CHAR == type) {
			return (T)new Character(strVal.length() == 0 ? 0 : strVal.charAt(0)); 
		}else if(PREFIX_TYPE_BOOLEAN == type) {
			return (T)new Boolean(Boolean.parseBoolean(strVal));
		}else if(PREFIX_TYPE_STRINGG == type) {
			return (T)strVal;
		}else {
			throw new CommonException("Not support type code: " + type + " with value: " + strVal);
		}
	}
	
}
