package cn.jmicro.api.security;

public interface ILoginStatusListener {

	public static final int STATUS_LOGIN = 1;
	public static final int STATUS_LOGOUT = 2;
	
	void statusChange(int status,ActInfoJRso ai);
}
