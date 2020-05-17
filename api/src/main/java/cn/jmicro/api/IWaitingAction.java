package cn.jmicro.api;

import java.util.Collection;
import java.util.Map;

public interface IWaitingAction<R> {

	R waitAct();
	
	
	public static <R> boolean isContinue(R result,Object continueVal) {
		if(continueVal != null && result == continueVal) {
			return true;
		}
		if(result == null) {
			return true;
		}
		else if(result instanceof Collection) {
			return ((Collection)result).isEmpty();
		}
		else if(result instanceof Map) {
			return ((Map)result).isEmpty();
		}
		return false;
	}
	
	public static <R> R doAct(long timeInMill,int cnt,String msg,IWaitingAction<R> act,Object continueVal) {
		R result = act.waitAct();
		while(isContinue(result,continueVal)) {
			if(cnt <= 0) {
				break;
			}
			cnt--;
			System.out.println("waiting ["+msg+"], cnt: "+ cnt);
			try {
				Thread.sleep(timeInMill);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			result = act.waitAct();
		}
		return result;
	}
	
	public static <R> R doAct(int cnt,String msg,IWaitingAction<R> act,Object continueVal) {
		return doAct(1000,cnt,msg,act,continueVal);
	}
	
	public static <R> R doAct(String msg,IWaitingAction<R> act,Object continueVal) {
		return doAct(1000,3,msg,act,continueVal);
	}
	
	public static <R> R doAct(IWaitingAction<R> act,Object continueVal) {
		return doAct(1000,3,"",act,continueVal);
	}
	
}
