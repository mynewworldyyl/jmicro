package testjava;

import org.junit.Test;

class Dog {
	  String name;
	  int age = -1; // For "unknown"
	  Dog() { name = "stray"; }
	  Dog(String nm) { name = nm; }
	  Dog(String nm, int yrs) { name = nm; age = yrs; }
	@Override
	public String toString() {
		return "Dog [name=" + name + ", age=" + age + "]";
	}
	  
	}

	interface MakeNoArgs {
	  Dog make();
	}

	interface Make1Arg {
	  Dog make(String nm);
	}

	interface Make2Args {
	  Dog make(String nm, int age);
	}

	
public class FunctionStyle {

	  //构造函数引用
	  @Test
	  public void testConstructMethod() {
	    MakeNoArgs mna = Dog::new; // [1]
	    Make1Arg m1a = Dog::new;   // [2]
	    Make2Args m2a = Dog::new;  // [3]

	    Dog dn = mna.make();
	    System.out.println(dn);
	    
	    Dog d1 = m1a.make("Comet");
	    System.out.println(d1);
	    
	    Dog d2 = m2a.make("Ralph", 4);
	    System.out.println(d2);
	    
	    
	  }
	  
	  //函数式接口
	  @Test
	  public void testFunctionalInterface() {
		  //x -> x.toString();
	  }
	  
	
	
}


class T {
	public static String a="3";
}

class T1 extends T{
	public static String a = "4";
}


