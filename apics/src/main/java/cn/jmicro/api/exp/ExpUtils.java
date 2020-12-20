package cn.jmicro.api.exp;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;

public class ExpUtils {
	
	public static final Map<String,Integer> OPS = new HashMap<>();
	
	static {
		
		OPS.put("||", 1);
		OPS.put("&&", 5);
		
		OPS.put("==", 8);
		OPS.put("!=", 8);
		
		OPS.put("<", 10);
		OPS.put("<=", 10);
		
		OPS.put(">", 10);
		OPS.put(">=", 10);
		
		OPS.put("+", 15);
		OPS.put("-", 15);
		
		OPS.put("%", 20);
		OPS.put("/", 20);
		OPS.put("*", 20);
		
		OPS.put("|", 23);
		OPS.put("&", 25);
		
		OPS.put("!", 30);
	}
	
	public static void main(String[] args) {
		/*
		Map<String,Object> cxt = new HashMap<>();
		cxt.put("a", 2);
		cxt.put("b", 3);
		Exp ex = new Exp();
		ex.setOriEx("b*3/2");
		
		Integer val = compute(ex,cxt,Integer.TYPE);
		System.out.println(val);
		*/
		
		//System.out.println(isValid("3*3"));
		//System.out.println(isValid("a+(\"3\" + \"3\")"));
		
		Map<String,Object> cxt = new HashMap<>();
		/*cxt.put("a", 0x0F);
		cxt.put("b", 0x00);*/
		
		cxt.put("var1", 2);
		cxt.put("var2", 3);
		
		Exp ex = new Exp();
		//ex.setOriEx("a==b");
		//ex.setOriEx("a<b");
		//ex.setOriEx("a<=a && a!=b");
		//ex.setOriEx("b % a");
		//ex.setOriEx("\"\" <= \"\"");
		
		//ex.setOriEx("a&b");
		ex.setOriEx("var1+var2");
		
		String val = compute(ex, cxt, String.class);
		
		System.out.println(val);
		
	}
	
	public static boolean isValid(List<String> subfix) {
		
		if(subfix == null || subfix.isEmpty()) {
			return false;
		}

        Stack<Object> stack = new Stack<>();
        for(int i=0; i< subfix.size(); i++){
            String item = subfix.get(i);
            if(OPS.containsKey(item)){
            	 if(!"!".equals(item)) {
            		 //双操作数操作符，出一个操作数，另一个操作数作为结果重新入栈
            		 stack.pop();
            	 }
            } else {
            	 //是操作数
            	 stack.push(item);
            }
        }
        
        Object v = null;
        try {
        	 v = stack.pop();
        }catch(EmptyStackException e) {
        	return false;
        }
        
        return v != null && stack.size() == 0;
	
	}
	
	public static boolean isValid(String exp) {
		if(Utils.isEmpty(exp)) {
			return false;
		}
		List<String> subfix = toSuffix(exp);
		return isValid(subfix);
	}
	
	public static <T> T compute(Exp ex, Map<String,Object> cxt, Class<T> resultClazz) {
		
		List<String> subfix = ex.getSuffix();
		
		if(subfix == null || subfix.isEmpty()) {
			if(Utils.isEmpty(ex.getOriEx())) {
				throw new CommonException("Null express");
			} else {
				subfix = toSuffix(ex.getOriEx());
				if(subfix == null || subfix.isEmpty()) {
					throw new CommonException("Invalid exp: " + ex.getOriEx());
				}
				ex.setSuffix(subfix);
			}
		}
		
        Stack<Object> stack = new Stack<>();
        for(int i=0; i< subfix.size(); i++){
            String item = subfix.get(i);
            
            if(OPS.containsKey(item)){
            	 //是一个操作符
            	 Object rst = null;
            	 switch(item) {
            	 case "+":
            		 rst = doAdd(cxt,stack);
            		 break;
            		 
            	 case "-":
            		 rst = doDec(cxt,stack);
            		 break;
            		 
            	 case "*":
            		 rst = doMul(cxt,stack);
            		 break;
            		 
            	 case "/":
            		 rst = doDiv(cxt,stack);
            		 break;
            		 
            	 case "%":
            		 rst = doMode(cxt,stack);
            		 break;
            		 
            	 case "&&":
            		 rst = doAnd(cxt,stack);
            		 break;
            		 
            	 case "||":
            		 rst = doOr(cxt,stack);
            		 break;
            		 
            	 case ">":
            		 rst = doGt(cxt,stack);
            		 break;
            		 
            	 case ">=":
            		 rst = doGte(cxt,stack);
            		 break;
            		 
            	 case "<":
            		 rst = doLt(cxt,stack);
            		 break;
            		 
            	 case "<=":
            		 rst = doLte(cxt,stack);
            		 break;
            		 
            	 case "==":
            		 rst = doEq(cxt,stack);
            		 break;
            		 
            	 case "!=":
            		 rst = !Boolean.parseBoolean(doEq(cxt,stack).toString());
            		 break;
            		 
            	 case "|":
            		 rst = doOrByBit(cxt,stack);
            		 break;
            		 
            	 case "&":
            		 rst = doAndByBit(cxt,stack);
            		 break;
            		 
            	 case "!":
            		 String v1 = getVal(cxt, stack.pop().toString());
            		 rst = !Boolean.parseBoolean(v1);
            		 break;
            	 }
            	 
            	 if(rst != null) {
            		 stack.push(rst);
            	 }else {
            		 throw new CommonException("Null result for: " + ex.getOriEx());
            	 }
            } else {
            	 //是操作数
            	 stack.push(item);
            }
        }
        
        Object v = stack.pop();
        if(v == null || stack.size() > 0) {
        	 throw new CommonException("Invalid exp: " + ex.getOriEx());
        }
        return toResult(v.toString(),resultClazz);
	}
	
	private static Object doAndByBit(Map<String, Object> cxt, Stack<Object> stack) {
		 String v2 = getVal(cxt, stack.pop().toString());//左操作数
		 String v1 = getVal(cxt, stack.pop().toString());//右操作数
		 return Integer.parseInt(v1) & Integer.parseInt(v2);
	}
	
	private static Object doOrByBit(Map<String, Object> cxt, Stack<Object> stack) {
		 String v2 = getVal(cxt, stack.pop().toString());//左操作数
		 String v1 = getVal(cxt, stack.pop().toString());//右操作数
		 return Integer.parseInt(v1) | Integer.parseInt(v2);
	}

	private static Object doEq(Map<String, Object> cxt, Stack<Object> stack) {
		 Object rst = null;
		
		 String v2 = getVal(cxt, stack.pop().toString());//左操作数
		 String v1 = getVal(cxt, stack.pop().toString());//右操作数
		 
		 if((v1.startsWith("\"") && v1.endsWith("\"")) ||
			(v2.startsWith("\"") && v2.endsWith("\"")) ) {
			//只要其中一个是字符串，那么就把另外一个也当字符串处理
			 if(v1.startsWith("\"")) {
				 v1 = v1.substring(1, v1.length()-1);
			 }
			 if(v2.startsWith("\"")) {
				 v2 = v2.substring(1, v2.length()-1);
			 }
			rst = v1.compareTo(v2) == 0; //字符串字典序比较
		 }else if(isNumber(v1.charAt(0)) && isNumber(v2.charAt(0))) {
			 //首字符是数字，必然是数字
			 rst = Double.parseDouble(v1) == Double.parseDouble(v2);
		 } else {
			 throw new CommonException("不支持操作类型：V1:" + v2 + ",V2:" + v1);
		 }
		 return rst;
	}
	
	
	private static Object doLte(Map<String, Object> cxt, Stack<Object> stack) {
		 Object rst = null;
		
		 String v2 = getVal(cxt, stack.pop().toString());//左操作数
		 String v1 = getVal(cxt, stack.pop().toString());//右操作数
		 
		 if(v1.startsWith("\"") && v1.endsWith("\"") &&
			v2.startsWith("\"") && v2.endsWith("\"")) {
			//只要其中一个是字符串，那么就把另外一个也当字符串处理
			rst = v1.compareTo(v2) <= 0; //字符串字典序比较
		 }else if(isNumber(v1.charAt(0)) && isNumber(v2.charAt(0))) {
			 //首字符是数字，必然是数字
			 rst = Double.parseDouble(v1) <= Double.parseDouble(v2);
		 } else {
			 throw new CommonException("不支持操作类型：V1:" + v2 + ",V2:" + v1);
		 }
		 return rst;
	}
   
	private static Object doLt(Map<String, Object> cxt, Stack<Object> stack) {
		 Object rst = null;
		
		 String v2 = getVal(cxt, stack.pop().toString());//左操作数
		 String v1 = getVal(cxt, stack.pop().toString());//右操作数
		 
		 if(v1.startsWith("\"") && v1.endsWith("\"") &&
			v2.startsWith("\"") && v2.endsWith("\"")) {
			//只要其中一个是字符串，那么就把另外一个也当字符串处理
			rst = v1.compareTo(v2) < 0; //字符串字典序比较
		 }else if(isNumber(v1.charAt(0)) && isNumber(v2.charAt(0))) {
			 //首字符是数字，必然是数字
			 rst = Double.parseDouble(v1) < Double.parseDouble(v2);
		 } else {
			 throw new CommonException("不支持操作类型：V1:" + v2 + ",V2:" + v1);
		 }
		 return rst;
	}
	
	
	private static Object doGte(Map<String, Object> cxt, Stack<Object> stack) {
		 Object rst = null;
		
		 String v2 = getVal(cxt, stack.pop().toString());//左操作数
		 String v1 = getVal(cxt, stack.pop().toString());//右操作数
		 
		 if(v1.startsWith("\"") && v1.endsWith("\"") &&
			v2.startsWith("\"") && v2.endsWith("\"")) {
			//只要其中一个是字符串，那么就把另外一个也当字符串处理
			rst = v1.compareTo(v2) >= 0; //字符串字典序比较
		 }else if(isNumber(v1.charAt(0)) && isNumber(v2.charAt(0))) {
			 //首字符是数字，必然是数字
			 rst = Double.parseDouble(v1) >= Double.parseDouble(v2);
		 } else {
			 throw new CommonException("不支持操作类型：V1:" + v2 + ",V2:" + v1);
		 }
		 return rst;
	}
    
	private static Object doGt(Map<String, Object> cxt, Stack<Object> stack) {
		 Object rst = null;
		
		 String v2 = getVal(cxt, stack.pop().toString());//左操作数
		 String v1 = getVal(cxt, stack.pop().toString());//右操作数
		 
		 if(v1.startsWith("\"") && v1.endsWith("\"") &&
			v2.startsWith("\"") && v2.endsWith("\"")) {
			//只要其中一个是字符串，那么就把另外一个也当字符串处理
			rst = v1.compareTo(v2) > 0; //字符串字典序比较
		 }else if(isNumber(v1) && isNumber(v2)) {
			 //首字符是数字，必然是数字
			 rst = Double.parseDouble(v1) > Double.parseDouble(v2);
		 } else {
			 throw new CommonException("不支持操作类型：V1:" + v2 + ",V2:" + v1);
		 }
		 return rst;
	}

	private static Object doOr(Map<String, Object> cxt, Stack<Object> stack) {
		
		 String v1 = getVal(cxt, stack.pop().toString());
		 String v2 = getVal(cxt, stack.pop().toString());
		 
		 Object rst = null;
		 rst = Boolean.parseBoolean(v2) || Boolean.parseBoolean(v1);
		 
		 return rst;
	}

	private static Object doAnd(Map<String, Object> cxt, Stack<Object> stack) {
		
		 String v1 = getVal(cxt, stack.pop().toString());
		 String v2 = getVal(cxt, stack.pop().toString());
		 
		 Object rst = null;
		 rst = Boolean.parseBoolean(v2) && Boolean.parseBoolean(v1);
		 
		 return rst;
	}

	private static Object doMode(Map<String, Object> cxt, Stack<Object> stack) {
		 String v1 = getVal(cxt, stack.pop().toString());
		 String v2 = getVal(cxt, stack.pop().toString());
		 Object rst = null;
		 rst = Integer.parseInt(v2) % Integer.parseInt(v1);
		 
		return rst;
	}

	private static Object doDiv(Map<String, Object> cxt, Stack<Object> stack) {
		 String v1 = getVal(cxt, stack.pop().toString());
		 String v2 = getVal(cxt, stack.pop().toString());
		 Object rst = null;
		 //首字符是数字，必然是数字
		 if(v1.indexOf('.') >= 0 || v2.indexOf('.') >= 0) {
			 rst = Double.parseDouble(v2) / Double.parseDouble(v1);
		 } else {
			 rst = Integer.parseInt(v2) / Integer.parseInt(v1);
		 }
		 
		return rst;
	}

	private static Object doMul(Map<String, Object> cxt, Stack<Object> stack) {
		 String v1 = getVal(cxt, stack.pop().toString());
		 String v2 = getVal(cxt, stack.pop().toString());
		 Object rst = null;
		 //首字符是数字，必然是数字
		 if(v1.indexOf('.') >= 0 || v2.indexOf('.') >= 0) {
			 rst = Double.parseDouble(v2) * Double.parseDouble(v1);
		 } else {
			 rst = Integer.parseInt(v2) * Integer.parseInt(v1);
		 }
		 
		return rst;
	}

	private static Object doDec(Map<String, Object> cxt, Stack<Object> stack) {
		 String v1 = getVal(cxt, stack.pop().toString());
		 String v2 = getVal(cxt, stack.pop().toString());
		 Object rst = null;
		 //首字符是数字，必然是数字
		 if(v1.indexOf('.') >= 0 || v2.indexOf('.') >= 0) {
			 rst = Double.parseDouble(v2) - Double.parseDouble(v1);
		 } else {
			 rst = Integer.parseInt(v2) - Integer.parseInt(v1);
		 }
		return rst;
	}

	private static Object doAdd(Map<String,Object> cxt,Stack<Object> stack) {
		 Object rst = null;
		 //支持数字及数符串相加
		 String v1 = getVal(cxt, stack.pop().toString());
		 String v2 = getVal(cxt, stack.pop().toString());
		 
		 if(v1.startsWith("\"") && v1.endsWith("\"") ||
			 v2.startsWith("\"") && v2.endsWith("\"")) {
			 //只要其中一个是字符串，那么就把另外一个也当字符串处理
			 if(v2.startsWith("\"") && v2.endsWith("\"")) {
				 rst = v2.substring(0,v2.length()-1);
			 } else {
				 rst = "\"" + v2;
			 }
			 
			 if(v1.startsWith("\"") && v1.endsWith("\"")) {
				 rst += v1.substring(1);
			 }else {
				 rst =  v1 +"\"";
			 }
		 }else if(isNumber(v1.charAt(0)) && isNumber(v2.charAt(0))) {
			 //首字符是数字，必然是数字
			 if(v1.indexOf('.') >= 0 || v2.indexOf('.') >= 0) {
				 rst = Double.parseDouble(v1) + Double.parseDouble(v2);
			 } else {
				 rst = Integer.parseInt(v1) + Integer.parseInt(v2);
			 }
		 } else {
			 throw new CommonException("不支持操作类型：V1:" + v2 + ",V2:" + v1);
		 }
		 return rst;
	}

	private static String getVal(Map<String, Object> cxt, String v1) {
		 if(v1.charAt(0) <= 'z' && v1.charAt(0) >= 'a' || v1.charAt(0) <= 'Z' && v1.charAt(0) >= 'A') {
			 //变量
			 if(cxt.containsKey(v1)) {
				 return cxt.get(v1).toString();
			 } else if(v1.equals("false") || v1.equals("true")) {
				 return v1;
			 } else {
				 throw new CommonException("变量不存在：" + v1);
			 }
		 } else {
			 return v1;
		 }
	}

	public static boolean isNumber(char charAt) {
		return charAt >= '0' && charAt <= '9';
	}

	private static <T> T toResult(String v, Class<T> resultClazz) {
		if(resultClazz == null) {
			throw new NullPointerException("Target class is null");
		}
		return JsonUtils.getIns().fromJson(v,resultClazz);
	}

	/**
     * 将表达式转为String list
     * @param expression
     * @return
     */
    private static List<String> str2ListExp(String strExp) {
        int index = 0;
        List<String> list = new ArrayList<>();
        do{
            char ch = strExp.charAt(index);
            if(ch == 13 || ch == 10 || ch == 32) {
            	//空格
            	index++;
            	continue;
            }
            
            if(ch > 47 && ch < 58){
                //是数字,判断多位数的情况
                String str = "";
                while(index < strExp.length() && (strExp.charAt(index) > 47 && strExp.charAt(index) < 58
                		|| strExp.charAt(index) == '.')){
                    str += strExp.charAt(index);
                    index ++;
                }
                list.add(str);
            } else if(ch == '(' || ch == ')'|| ch == '+'|| ch == '-'
            		|| ch == '*'|| ch == '/' || ch == '%'  ) {
            	list.add(ch+"");
            	index++;
            } else if(ch == '|' || ch == '&') {
            	if(index < strExp.length() && ch == strExp.charAt(index+1)) {
            		list.add(ch+""+ch);
            		index++;
            	} else {
            		list.add(ch+"");
            	}
            	index++;
            } else if(ch == '!') {
            	if(index < strExp.length() && '=' == strExp.charAt(index+1)) {
            		list.add("!=");
            		index++;
            	} else {
            		list.add(ch+"");
            	}
            	index++;
            }else if(ch == '>' || ch == '<') {
            	if(index < strExp.length() && '=' == strExp.charAt(index+1)) {
            		index++;
            		list.add(ch+"=");
            	} else {
            		list.add(ch+"");
            	}
            	index++;
            }else if(ch == '=') {
            	if(index < strExp.length() && '=' == strExp.charAt(index+1)) {
            		index++;
            		index++;
            		list.add("==");
            	} else {
            		throw new CommonException("无效字符【" + index +"】附近");
            	}
            }else if(ch >='A' && ch <= 'Z' || ch >='a' && ch <= 'z') {
            	//变量
                String str = "";
                while(index < strExp.length()){
                	ch = strExp.charAt(index);
                	if(ch > 47 && ch <  58 || ch >='A' && ch <= 'Z' || ch >='a' && ch <= 'z' || '_'== ch) {
                		 str += ch;
                		 index++;
                	} else {
                		break;
                	}
                }
                list.add(str);
            }else if(ch == '"') {
            	 //字符串常量
            	 index++;
            	 String str = "\"";
                 while(index < strExp.length()){
                 	ch = strExp.charAt(index);
                 	if(ch != '"') {
                 		 str += ch;
                 		 index++;
                 	} else {
                 		break;
                 	}
                 }
                 
                 if(/*str.length() == 1 || */strExp.charAt(index) != '"') {
                	 //单个引用,或者没有结束引号，肯定是无效表式
                	 throw new CommonException("无效字符【" + index +"】附近");
                 }
                 
                 str += '"';
                 index++;
                 
                 list.add(str);
            }
        }while (index < strExp.length());
        
        return list;
    }
	
	public static List<String> toSuffix(String strExp) {
		List<String> expressionList = str2ListExp(strExp);
        //创建一个栈用于保存操作符
        Stack<String> opStack = new Stack<>();
        //创建一个list用于保存后缀表达式
        List<String> suffixList = new ArrayList<>();
        for(String item : expressionList){
            //得到数或操作符
            if(OPS.containsKey(item)){
                //是操作符 判断操作符栈是否为空
                if(opStack.isEmpty() || "(".equals(opStack.peek()) || OPS.get(item) > OPS.get(opStack.peek())){
                    //为空或者栈顶元素为左括号或者当前操作符大于栈顶操作符直接压栈
                    opStack.push(item);
                }else {
                    //否则将栈中元素出栈入队，直到遇到大于当前操作符或者遇到左括号时
                    while (!opStack.isEmpty() && !"(".equals(opStack.peek())){
                        if(OPS.get(item) <= OPS.get(opStack.peek())){
                            suffixList.add(opStack.pop());
                        }
                    }
                    //当前操作符压栈
                    opStack.push(item);
                }
            }else if(isConstant(item) || isVar(item)){
                //是数字则直接入队
                suffixList.add(item);
            }else if("(".equals(item)){
                //是左括号，压栈
                opStack.push(item);
            }else if(")".equals(item)){
                //是右括号 ，将栈中元素弹出入队，直到遇到左括号，左括号出栈，但不入队
                while (!opStack.isEmpty()){
                    if("(".equals(opStack.peek())){
                        opStack.pop();
                        break;
                    }else {
                        suffixList.add(opStack.pop());
                    }
                }
            }else {
                throw new CommonException("有非法字符！: " + item);
            }
        }
        //循环完毕，如果操作符栈中元素不为空，将栈中元素出栈入队
        while (!opStack.isEmpty()){
            suffixList.add(opStack.pop());
        }
        return suffixList;
    }

    private static boolean isVar(String item) {
    	char pch = item.charAt(0);
		if((pch <= 'Z' && pch >= 'A') || (pch <= 'z' && pch >= 'a')) {
			return true;
		}
		return false;
	}

	private static boolean isConstant(String item) {
		if(item.startsWith("\"") && item.endsWith("\"") || isNumber(item)) {
			return true;
		}
		return false;
	}

	/**
     * 判断是否为数字
     * @param num
     * @return
     */
    private static boolean isNumber(String num){
        return num.matches("^[\\-\\+]?\\d+[\\.]?\\d*$");
    }

}
