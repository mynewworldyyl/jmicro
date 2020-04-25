print('Hello World!');

var fun1 = function(name) {
    print('Hi there from Javascript, ' + name);
    return "greetings from javascript";
};

var fun2 = function (object) {
    print("JS Class Definition: " + Object.prototype.toString.call());
    
    print("JS Class Definition: " + object.toString());
};

var MyJavaClass = Java.type('org.jmicro.example.test.js.JavaObject');

var result = MyJavaClass.fun1('John Doe');
print(result);

var o = new MyJavaClass();
o.fun2("Hello from javascript invoke");

MyJavaClass.fun3(123);
//class java.lang.Integer

MyJavaClass.fun3(49.99);
//class java.lang.Double

MyJavaClass.fun3(true);
//class java.lang.Boolean

MyJavaClass.fun3("hi there")
//class java.lang.String

MyJavaClass.fun3(new Number(23));
//class jdk.nashorn.internal.objects.NativeNumber

MyJavaClass.fun3(new Date());
//class jdk.nashorn.internal.objects.NativeDate

MyJavaClass.fun3(new RegExp());
//class jdk.nashorn.internal.objects.NativeRegExp

MyJavaClass.fun3({foo: 'bar'});
//class jdk.nashorn.internal.scripts.JO4