package cn.jmicro.api.async;

public interface IFinish<T> {

	void onResult(ISuccess<T> suc,IFailure fail);
}
