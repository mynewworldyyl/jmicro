package cn.jmicro.codegenerator;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import cn.jmicro.api.annotation.SO;

/**
 * @TakeTime 注解的注解处理器
 * @SupportedSourceVersion 表示对应的版本
 * @SupportedAnnotationTypes 表示处理哪种类型的注解(这是一个集合, 其值注解的全限定名)
 * @AutoService @AutoService(Processor.class) :向javac注册我们这个自定义的注解处理器，
 * 这样，在javac编译时，才会调用到我们这个自定义的注解处理器方法。@AutoService这里主要是用来生成
 * META-INF/services/javax.annotation.processing.Processor文件的。如果不加上这个注解，那么，你需要自己进行手动配置进行注册
 * <p>
 * AbstractProcessor是注解处理器的抽象类，我们通过继承AbstractProcessor类然后实现process方法来创建我们自己的注解处理器，
 * 所有处理注解的代码放在process方法里面
 */
@SupportedSourceVersion(value = SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = {"cn.jmicro.api.annotation.SO"})
//@AutoService(Processor.class)
public class CodeDemo extends AbstractProcessor {

    /**
     * Messager接口提供注解处理器用来报告错误消息、警告和其他通知的方式
     * 它不是注解处理器开发者的日志工具，而是用来写一些信息给使用此注解器的第三方开发者的
     * 注意：我们应该对在处理过程中可能发生的异常进行捕获，通过Messager接口提供的方法通知用户（在官方文档中描述了消息的不同级别。非常重要的是Kind.ERROR）。
     * 此外，使用带有Element参数的方法连接到出错的元素，
     * 用户可以直接点击错误信息跳到出错源文件的相应行。
     * 如果你在process()中抛出一个异常，那么运行注解处理器的JVM将会崩溃（就像其他Java应用一样），
     * 这样用户会从javac中得到一个非常难懂出错信息
     */
    private Messager messager;

    /**
     * 实现Filer接口的对象，用于创建文件、类和辅助文件。
     * 使用Filer你可以创建文件
     * Filer中提供了一系列方法,可以用来创建class、java、resources文件
     * filer.createClassFile()[创建一个新的类文件，并返回一个对象以允许写入它]
     * filer.createResource() [创建一个新的源文件，并返回一个对象以允许写入它]
     * filer.createSourceFile() [创建一个用于写入操作的新辅助资源文件，并为它返回一个文件对象]
     */
    private Filer filer;

    /**
     * 用来处理Element的工具类
     * Elements接口的对象，用于操作元素的工具类。
     */
    private JavacElements elementUtils;

    /**
     * 用来处理TypeMirror的工具类
     * 实现Types接口的对象，用于操作类型的工具类。
     */
    private Types typeUtils;

    /**
     * 这个依赖需要将${JAVA_HOME}/lib/tools.jar 添加到项目的classpath,IDE默认不加载这个依赖
     */
    private JavacTrees trees;

    /**
     * 这个依赖需要将${JAVA_HOME}/lib/tools.jar 添加到项目的classpath,IDE默认不加载这个依赖
     * TreeMaker创建语法树节点的所有方法，创建时会为创建出来的JCTree设置pos字段，
     * 所以必须用上下文相关的TreeMaker对象来创建语法树节点，而不能直接new语法树节点。
     */
    private TreeMaker treeMaker;

    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        elementUtils = (JavacElements) processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        this.trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    /**
     * 该方法将一轮一轮的遍历源代码
     * 处理注解前需要先获取两个重要信息，
     * 第一是注解本身的信息，具体来说就是获取注解对象，有了注解对象以后就可以获取注解的值。
     * 第二是被注解元素的信息，具体来说就是获取被注解的字段、方法、类等元素的信息
     *
     * @param annotations 该方法需要处理的注解类型
     * @param roundEnv    关于一轮遍历中提供给我们调用的信息.
     * @return 该轮注解是否处理完成 true 下轮或者其他的注解处理器将不会接收到次类型的注解.用处不大.
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//        roundEnv.getRootElements()会返回工程中所有的Class,在实际应用中需要对各个Class先做过滤以提高效率，避免对每个Class的内容都进行扫描
        roundEnv.getRootElements();
        messager.printMessage(Diagnostic.Kind.NOTE, "TakeTimeProcessor注解处理器处理中");
        TypeElement currentAnnotation = null;
//        遍历注解集合,也即@SupportedAnnotationTypes中标注的类型
        for (TypeElement annotation : annotations) {
            messager.printMessage(Diagnostic.Kind.NOTE, "遍历本注解处理器处理的所有注解,当前遍历到的注解是：" + annotation.getSimpleName());
            currentAnnotation = annotation;
        }
//      获取所有包含 TakeTime 注解的元素(roundEnv.getElementsAnnotatedWith(TakeTime.class))返回所有被注解了@Factory的元素的列表。你可能已经注意到，我们并没有说“所有被注解了@TakeTime的方法的列表”，因为它真的是返回Element的列表。请记住：Element可以是类、方法、变量等。所以，接下来，我们必须检查这些Element是否是一个方法)
        Set<? extends Element> elementSet = roundEnv.getElementsAnnotatedWith(SO.class);
        messager.printMessage(Diagnostic.Kind.NOTE, "TakeTimeProcessor注解处理器处理@TakeTime注解");
        for (Element element : elementSet) {
                //获取注解
                SO TakeTimeAnnotation = element.getAnnotation(SO.class);
                //获取注解中配置的值
                String tag = TakeTimeAnnotation.toString();
                messager.printMessage(Diagnostic.Kind.NOTE, currentAnnotation.getSimpleName() + "注解上设置的值为：" + tag);

//                TypeSpec typeSpec = generateCodeByPoet(typeElement, null);

//                方法名(这里之所以是方法名,是因为这个注解是标注在方法上的)
                String methodName = element.getSimpleName().toString();

//                类名[全限定名]
//                element.getEnclosingElement()返回封装此元素（非严格意义上）的最里层元素,由于我们在上面判断了element是method类型,所以直接封装method的的就是类了
//                http://www.169it.com/article/3400309390285698450.html
                String className = element.getEnclosingElement().toString();

                messager.printMessage(Diagnostic.Kind.NOTE, "当前被标注注解的方法所在的类是：" + className);
                messager.printMessage(Diagnostic.Kind.NOTE, currentAnnotation.getSimpleName() + "当前被标注注解的方法是：" + methodName);

//                JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
                enhanceMethodDecl(elementUtils.getTree(element), tag, className + "." + methodName);



            if (element.getKind() == ElementKind.FIELD) {
//                当前element是字段类型
                VariableElement variableElement = (VariableElement) element;
                messager.printMessage(Diagnostic.Kind.ERROR, "字段不能使用@TakeTime注解", element);
            }

            if (element.getKind() == ElementKind.CONSTRUCTOR) {
//                当前element是构造方法类型

            }
        }


        return false;
    }


    /**
     * 方法增强
     *
     * @param jcTree
     * @param methodName 方法的全限定名
     * @param tag        标识
     * @return
     */
    private JCTree.JCMethodDecl enhanceMethodDecl(JCTree jcTree, String tag, String methodName) {
        JCTree.JCMethodDecl jcMethodDecl = (JCTree.JCMethodDecl) jcTree;

//        生成表达式System.currentTimeMillis()
        JCTree.JCExpressionStatement time = treeMaker.Exec(treeMaker.Apply(
                //参数类型(传入方法的参数的类型) 如果是无参的不能设置为null 使用 List.nil()
                List.nil(),
                memberAccess("java.lang.System.currentTimeMillis"),
                //因为不需要传递参数,所以直接设置为List.nil() 不能设置为null
                List.nil()
                //参数集合[集合中每一项的类型需要跟第一个参数对照]
//                List.of(treeMaker.Literal())
                )
        );


//        编译后该方法会存在一个startTime的变量,其值为编译时的时间
        JCTree.JCVariableDecl startTime = createVarDef(treeMaker.Modifiers(0), "startTime", memberAccess("java.lang.Long"), treeMaker.Literal(System.currentTimeMillis()));

//        耗时计算表示式
        JCTree.JCExpressionStatement timeoutStatement = treeMaker.Exec(
                treeMaker.Apply(
                        List.of(memberAccess("java.lang.Long"), memberAccess("java.lang.Long")),
                        memberAccess("java.lang.Math.subtractExact"),
                        List.of(time.expr, treeMaker.Ident(startTime.name))
                )
        );
//
        messager.printMessage(Diagnostic.Kind.NOTE, "::::::::::::::::::::");
        messager.printMessage(Diagnostic.Kind.NOTE, timeoutStatement.expr.toString());

//        生成表达式System.out.println()
        JCTree.JCExpressionStatement TakeTime = treeMaker.Exec(treeMaker.Apply(
                //参数类型(传入方法的参数的类型) 如果是无参的不能设置为null 使用 List.nil()
                List.of(memberAccess("java.lang.String"), memberAccess("java.lang.String"), memberAccess("java.lang.Long")),
//                因为这里要传多个参数,所以此处应使用printf,而不是println
                memberAccess("java.lang.System.out.printf"),
                //取到前面定义的startTime的变量
//                List.of(treeMaker.Ident(startTime.name))
//                取得结果
                List.of(treeMaker.Literal(">>>>>>>>TAG:%s -> 方法%s执行用时：%d<<<<<<<"), treeMaker.Literal(tag), treeMaker.Literal(methodName), timeoutStatement.getExpression())
                )
        );

//        catch中的代码块
        JCTree.JCBlock catchBlock = treeMaker.Block(0, List.of(
                treeMaker.Throw(
//                        e 这个字符是catch块中定义的变量
                        treeMaker.Ident(getNameFromString("e"))
                )
        ));
//      finally代码块中的代码
        JCTree.JCBlock finallyBlock = treeMaker.Block(0, List.of(TakeTime));


        List<JCTree.JCStatement> statements = jcMethodDecl.body.getStatements();
//        遍历方法体中每一行(断句符【分号/大括号】)代码
        for (JCTree.JCStatement statement : statements) {
            messager.printMessage(Diagnostic.Kind.NOTE, "遍历方法体中的statement：" + statement);
            messager.printMessage(Diagnostic.Kind.NOTE, "该statement的类型：" + statement.getKind());
            if (statement.getKind() == Tree.Kind.RETURN) {
                messager.printMessage(Diagnostic.Kind.NOTE, "该statement是Return语句");
                break;
            }
        }

//        jcMethodDecl.body即为方法体，利用treeMaker的Block方法获取到一个新方法体，将原来的替换掉
        jcMethodDecl.body = treeMaker.Block(0, List.of(
//             定义开始时间,并附上初始值 ,初始值为编译时的时间
                startTime,
                treeMaker.Exec(
//                      这一步 将startTime变量进行赋值 其值 为(表达式也即运行时时间) startTime = System.currentTimeMillis()
                        treeMaker.Assign(
                                treeMaker.Ident(getNameFromString("startTime")),
                                time.getExpression()
                        )
                ),
//              添加TryCatch
                treeMaker.Try(jcMethodDecl.body,
                        List.of(treeMaker.Catch(createVarDef(treeMaker.Modifiers(0), "e", memberAccess("java.lang.Exception"),null),
                        		catchBlock)), finallyBlock)

//                下面这段是IF代码,是我想在try catch finally后添加return代码(如果有需要的话),结果发现 如果不写下面的代码的话
//                Javac会进行判断,如果这个方法有返回值的话,那么Javac会自动在try块外定义一个变量,同时找到要上一个return的变量并赋值
//                然后返回,具体可以查看编译后的字节码的反编译文件,如果该方法没有返回值,那么什么也不做

//                根据返回值类型,判断是否在方法末尾添加 return  语句  判断返回类型的Kind是否等于TypeKind.VOID
//                treeMaker.If(treeMaker.Parens(
//                        treeMaker.Binary(
//                                JCTree.Tag.EQ,
//                                treeMaker.Literal(returnType.getKind().toString()),
//                                treeMaker.Literal(TypeKind.VOID.toString()))
//                        ),
//
//                        //符合IF判断的Statement
//                        treeMaker.Exec(treeMaker.Literal("返回类型是Void,不需要return")),
////                        不符合IF判断的Statement
//                        null
//                )
                )
        );


        return jcMethodDecl;
    }


    /**
     * 创建变量语句
     *
     * @param modifiers
     * @param name      变量名
     * @param varType   变量类型
     * @param init      变量初始化语句
     * @return
     */
    private JCTree.JCVariableDecl createVarDef(JCTree.JCModifiers modifiers, String name, JCTree.JCExpression varType, JCTree.JCExpression init) {
        return treeMaker.VarDef(
                modifiers,
                //名字
                getNameFromString(name),
                //类型
                varType,
                //初始化语句
                init
        );
    }


    /**
     * 根据字符串获取Name，（利用Names的fromString静态方法）
     *
     * @param s
     * @return
     */
    private com.sun.tools.javac.util.Name getNameFromString(String s) {
        return names.fromString(s);
    }


    /**
     * 创建 域/方法 的多级访问, 方法的标识只能是最后一个
     * . 运算符
     * @param components
     * @return
     */
    private JCTree.JCExpression memberAccess(String components) {
        String[] componentArray = components.split("\\.");
        JCTree.JCExpression expr = treeMaker.Ident(getNameFromString(componentArray[0]));
        for (int i = 1; i < componentArray.length; i++) {
            expr = treeMaker.Select(expr, getNameFromString(componentArray[i]));
        }
        return expr;
    }


}
