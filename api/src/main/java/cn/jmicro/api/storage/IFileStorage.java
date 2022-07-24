package cn.jmicro.api.storage;

/**
 * 将文件存储到专门的储存系统
 * 如果当前可以直接存储，则直接存，如果不行，则调用远程服务存储
 *
 * @author Yulei Ye
 * @date 2022年7月16日 下午12:21:21
 */
public interface IFileStorage {

	public static final int TYPE_INIT = 1;
	public static final int TYPE_NEXT = 2;
	public static final int TYPE_FINISH = 3;
	
	/**
	 * 返回文件ID
	 * 客户端将File转为字节流，传输给服务端，服务端再将字节流保存为文件，并以File文件参数调用目标方法
	 * 
	 * @param attr 文件关联属性
	 * @param file 要保存的文件
	 * @return 返回文件的ID
	 */
	void save(FileJRso file, IProgress p);
	
}
