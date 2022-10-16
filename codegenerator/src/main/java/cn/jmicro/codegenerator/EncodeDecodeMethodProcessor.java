package cn.jmicro.codegenerator;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import cn.jmicro.api.annotation.SO;

//@SupportedAnnotationTypes("com.imooc.mylombok.Getter")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class EncodeDecodeMethodProcessor extends AbstractProcessor {

	// 打印log
	private Messager messager;
	// 抽象语法树
	private JavacTrees trees;
	// 封装了创建AST节点的一些方法
	private TreeMaker treeMaker;
	// 提供了创建标识符的一些方法
	private Names names;
	
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> set = new HashSet<>();
		set.add(SO.class.getCanonicalName());
		return set;
	}

	//初始化方法
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		this.messager = processingEnv.getMessager();
		this.trees = JavacTrees.instance(processingEnv);
		Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
		this.treeMaker = TreeMaker.instance(context);
		this.names = Names.instance(context);
	}

	//真正处理注解的方法
	@Override
	public synchronized boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		//获取所有包含SO注解的类
		Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(SO.class);
		set.forEach(element -> {
			JCTree jcTree = trees.getTree(element);
			jcTree.accept(new TreeTranslator() {
				@Override
				public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
					List<JCTree.JCVariableDecl> jclist = List.nil();
					
					//获取所有属性
					for(JCTree tree : jcClassDecl.defs) {
						if(!tree.getKind().equals(Tree.Kind.VARIABLE)) {
							continue;
						}
						
						JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl)tree;
						JCTree.JCModifiers modifiers = jcVariableDecl.getModifiers();
						
						Set<Modifier> ms = modifiers.getFlags();
						boolean s = false;
						boolean t = false;
						
						for(Modifier m : ms) {
							if(m == Modifier.STATIC) {
								s = true;
							}
							if(m == Modifier.TRANSIENT) {
								t = true;
							}
						}
						
						if(!(s || t)) {
							jclist = jclist.append(jcVariableDecl);
						}
					}
					
					jclist.sort((s1,s2)->{
						return s1.getName().compareTo(s2.getName());
					});
					
					//实现接口cn.jmicro.api.codec.ISerializeObject
					/*
					 JCTree.JCClassDecl cd = treeMaker.ClassDef(
							treeMaker.Modifiers(Flags.PUBLIC), 
							getNameFromString("cn.jmicro.api.codec.ISerializeObject"), 
							List.nil(), null, List.nil(), List.nil());
					*/
					
					//导入接口
					importClass(jcClassDecl,"cn.jmicro.api.codec","ISerializeObject");
					importClass(jcClassDecl,"java.io","DataOutput");
					importClass(jcClassDecl,"java.io","IOException");
					importClass(jcClassDecl,"cn.jmicro.api.codec","JDataOutput");
					
					//实现接口
					jcClassDecl.implementing = jcClassDecl.implementing.append(
							memberTypeExpression("cn.jmicro.api.codec.ISerializeObject"));
					
					//实现编码方法
					jcClassDecl.defs = jcClassDecl.defs.prepend(makeEncodeMethodMethodDecl(jcClassDecl,jclist));
					//实现解码方法
					jcClassDecl.defs = jcClassDecl.defs.prepend(makeDecodeMethodMethodDecl(jcClassDecl,jclist));
					
					/*//为每一个属性创建get方法
					jcVariableDeclList.forEach(jcVariableDecl -> {
						messager.printMessage(Diagnostic.Kind.NOTE, jcVariableDecl.getName() + " has been processed");
					});*/
					super.visitClassDef(jcClassDecl);
				}
			});
		});

		return true;
	}

	/**
		 private static String getEncodeMethod(CtClass cls) throws NotFoundException, CannotCompileException {
			StringBuffer sb = new StringBuffer("public void encode(java.io.DataOutput __buffer) throws java.io.IOException { \n");
			
			CtClass supcls = cls.getSuperclass();
			if(supcls.getName().endsWith("JRso")) {
				sb.append("super.encode(__buffer); ");
			}
			
			sb.append(cls.getName()).append(" __obj =  this;\n ");
			sb.append(" cn.jmicro.api.codec.JDataOutput out = (cn.jmicro.api.codec.JDataOutput)__buffer;\n");
			sb.append(" cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder(); \n");
			
			//ClassPool cp = ClassPool.getDefault();
			
			//boolean d = cls.getName().contains("ActInfo");
			
			CtField[] fields = cls.getDeclaredFields();
			
			//CtField[] fields = cls.;
			
			for(int i = 0; i < fields.length; i++) {
				CtField f = fields[i];
				if(Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers())
					|| Modifier.isFinal(f.getModifiers()) ) {
					//transient字段不序列化,final不可赋值，static一般为常量，所以也不做序列化
					continue;
				}
				
				CtClass fieldDeclareType = f.getType();
				sb.append(" ").append(fieldDeclareType.getName()).append(" __val"+i).append("= __obj.").append(f.getName()).append(";\n");
				 // 字数型空值序列化后变为0值，字符串和字符型NULL值变为""空串,布尔值NULL值变为false
				 
				if(fieldDeclareType == CtClass.intType ) {
					sb.append(" out.writeInt(").append(" __val"+i).append("); \n");
				}else if(fieldDeclareType.getName().equals(Integer.class.getName()) ) {
					sb.append(" out.writeInt(").append(" __val"+i).append(" == null ? new java.lang.Integer(0) : __val"+i+"); \n");
				}else if(fieldDeclareType.getName().equals(String.class.getName())) {
					sb.append(" out.writeUTF(").append(" __val"+i).append(" == null ? \"\" : __val"+i+"); \n");
				}else if(fieldDeclareType == CtClass.longType ) {
					sb.append(" out.writeLong(").append(" __val"+i).append("); \n");
				}else if(fieldDeclareType.getName().equals(Long.class.getName()) ) {
					sb.append(" out.writeLong(").append(" __val"+i).append(" == null ? new java.lang.Long((long)0) : __val"+i+"); \n");
				}else if(fieldDeclareType == CtClass.byteType) {
					sb.append(" out.writeByte(").append(" __val").append(i).append("); \n");
				}else if(fieldDeclareType.getName().equals(Byte.class.getName()) ) {
					sb.append(" out.writeByte(").append(" __val"+i).append(" == null ? new java.lang.Byte((byte)0) : __val"+i+"); \n");
				}else if(fieldDeclareType.getName().equals(Short.class.getName()) ) {
					sb.append(" out.writeShort(").append(" __val"+i).append(" == null ? new java.lang.Short((short)0) : __val"+i+"); \n");
				}else if(fieldDeclareType == CtClass.shortType) {
					sb.append(" out.writeShort(").append(" __val").append(i).append("); \n");
				}else  if(fieldDeclareType == CtClass.floatType) {
					sb.append(" out.writeFloat(").append(" __val").append(i).append("); \n");
				}else  if(fieldDeclareType.getName().equals(Float.class.getName()) ) {
					sb.append(" out.writeFloat(").append(" __val"+i).append(" == null ? new java.lang.Float((double)0) : __val"+i+"); \n");
				}else if(fieldDeclareType == CtClass.doubleType) {
					sb.append(" out.writeDouble(").append(" __val").append(i).append("); \n");
				}else if(fieldDeclareType.getName().equals(Double.class.getName()) ) {
					sb.append(" out.writeDouble(").append(" __val"+i).append(" == null ?  (double)0: __val"+i+".doubleValue()); \n");
				}else if(fieldDeclareType == CtClass.booleanType ) {
					sb.append(" out.writeBoolean(").append(" __val").append(i).append("); \n");
				}else if(fieldDeclareType.getName().equals(Boolean.class.getName()) ) {
					sb.append(" out.writeBoolean(").append(" __val"+i).append(" == null ? false : __val"+i+".booleanValue()); \n");
				}else if(fieldDeclareType.getName().equals(Character.class.getName()) ) {
					sb.append(" out.writeChar(").append(" __val").append(" == null ? '' : __val"+i+"); \n");
				}else if(fieldDeclareType == CtClass.charType) {
					sb.append(" out.writeChar(").append(" __val").append(i).append("); \n");
				}else  if(fieldDeclareType.getName().equals(Date.class.getName())) {
					sb.append("if(__val"+i+" == null)  __buffer.writeLong(0L) ;") ;
					sb.append(" else out.writeLong(").append(" __val").append(i).append(".getTime()); \n");
				}else {
					sb.append("if(__val"+i+" == null){  out.write(cn.jmicro.api.codec.DecoderConstant.PREFIX_TYPE_NULL); \n} \n") ;
					sb.append(" else { out.write(cn.jmicro.api.codec.DecoderConstant.PREFIX_TYPE_PROXY); \n");
					if(isSOClass(fieldDeclareType.getName())) {
						sb.append("java.lang.Object __o"+i).append("=__val"+i).append("; \n");
						sb.append(" ((cn.jmicro.api.codec.ISerializeObject)__o"+i+").encode(__buffer);\n }");
					} else {
						sb.append(" __coder.encode(__buffer,__val").append(i).append(",").append(fieldDeclareType.getName()).append(".class,").append(" null ); \n }");
					}
				}
				//fieldDeclareType.detach();
				
				sb.append("\n\n");
			}
		
			sb.append("}");
		
			//System.out.println("\n\n");

			return sb.toString();
		}
	 * @param jcVariableDeclList 实体类的全部字段
	 * @return
	 */
	private JCTree makeEncodeMethodMethodDecl(JCTree.JCClassDecl jcClassDecl,List<JCVariableDecl> jcVariableDeclList) {
		
		
		//方法体语句
		ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();	
		
		/*
		    CtClass supcls = cls.getSuperclass();
			if(supcls.getName().endsWith("JRso")) {
				sb.append("super.encode(__buffer); ");
			}
		 */
		
		JCTree.JCIdent buffer = treeMaker.Ident(this.getNameFromString("__buffer"));
		
		JCTree.JCExpression jdataOutputType = memberTypeExpression("cn.jmicro.api.codec.JDataOutput");
		JCTree.JCExpression dataOutputType = memberTypeExpression("java.io.DataOutput");
		JCTree.JCExpression typeCorderFactoryType = memberTypeExpression("cn.jmicro.api.codec.TypeCoderFactory");
		JCTree.JCExpression typeCoderType = memberTypeExpression("cn.jmicro.api.codec.typecoder.TypeCoder");
		
		JCTree.JCExpression supperClazz = jcClassDecl.getExtendsClause();
		if(supperClazz != null) {
			//对父类字段做编码
			JCTree.JCIdent superCt = (JCTree.JCIdent)supperClazz;
			if(superCt.getName().toString().endsWith("JRso")) {
				JCTree.JCExpressionStatement callsuper = treeMaker.Exec(
			        treeMaker.Apply(
	                        List.of(dataOutputType),
	                        memberTypeExpression("super.encode"),
	                        List.of(buffer)
	                )
				);
				statements = statements.append(callsuper);
			}
		}
		
		//(cn.jmicro.api.codec.JDataOutput) __buffer
		JCTree.JCTypeCast cast = treeMaker.TypeCast(jdataOutputType, buffer);
		
		//cn.jmicro.api.codec.JDataOutput out = ...,初始值为(cn.jmicro.api.codec.JDataOutput) __buffer
		JCTree.JCVariableDecl out = createVarDef(treeMaker.Modifiers(0), "out", jdataOutputType, cast);
		statements = statements.append(out);
		
		//cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder();
		
		//cn.jmicro.api.codec.TypeCoderFactory.getIns()
		JCTree.JCExpressionStatement getIns = treeMaker.Exec(treeMaker.Apply(
                //参数类型(传入方法的参数的类型) 如果是无参的不能设置为null 使用 List.nil()
                List.nil(),
                memberTypeExpression("cn.jmicro.api.codec.TypeCoderFactory.getIns"),
                //因为不需要传递参数,所以直接设置为List.nil() 不能设置为null
                List.nil()
                //参数集合[集合中每一项的类型需要跟第一个参数对照]
//              List.of(treeMaker.Literal())
                )
        );
		JCTree.JCVariableDecl getInsVar = createVarDef(treeMaker.Modifiers(0), "getInsVar", typeCorderFactoryType, getIns.getExpression());
		
		//.getDefaultCoder()
		JCTree.JCVariableDecl __coder = createVarDef(
				treeMaker.Modifiers(0), "__coder", typeCoderType, 
					treeMaker.Exec(treeMaker.Apply(List.nil(),
							memberTypeExpression("getInsVar.getDefaultCoder"),List.nil()
		                )
					).getExpression()
		);
		
		
		JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
		
		List<JCTree.JCVariableDecl> paramList = List.nil();
		//方法参数 DataOutput __buffer
		paramList = paramList.append(this.methodParamVariableDecl("__buffer", "DataOutput"));
		
		List<JCTree.JCExpression> exceptionsList = List.nil();
		//throws IOException
		exceptionsList = exceptionsList.append(treeMaker.Ident(names.fromString("IOException")));
		
		JCTree.JCMethodDecl m = treeMaker.MethodDef(
             treeMaker.Modifiers(Flags.PUBLIC), // 访问标识
             names.fromString("encode"), 		// 方法名字
             treeMaker.TypeIdent(TypeTag.VOID),// 返回类型
             List.nil(),                        // 泛型形参列表
             paramList,                        // 参数列表
             exceptionsList,                   // 异常列表
             body, 								// block
             null                   			// 默认方法
         );
		
		 return m;
	}
	
	private JCTree makeDecodeMethodMethodDecl(JCTree.JCClassDecl jcClassDecl,List<JCVariableDecl> jcVariableDeclList) {
		
		return null;
	}
	
	// 创建getter方法
	private JCTree.JCMethodDecl makeGetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
		ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
		statements.append(treeMaker
				.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName())));
		JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
		return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), getNewMethodName(jcVariableDecl.getName()),
				jcVariableDecl.vartype, List.nil(), List.nil(), List.nil(), body, null);
	}

	// 获取方法名
	private Name getNewMethodName(Name name) {
		String s = name.toString();
		return names.fromString("get" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
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
     * 创建 域/方法 的多级访问, 方法的标识只能是最后一个
     * . 运算符
     * @param components
     * @return
     */
    private JCTree.JCExpression memberTypeExpression(String components) {
        String[] componentArray = components.split("\\.");
        JCTree.JCExpression expr = treeMaker.Ident(getNameFromString(componentArray[0]));
        for (int i = 1; i < componentArray.length; i++) {
            expr = treeMaker.Select(expr, getNameFromString(componentArray[i]));
        }
        return expr;
    }
    
    /**
     * 
     * @param fieldName 变量名称
     * @param fieldType 变量类型
     * @param modifier 修饰符
     * @return
     */
    private JCTree.JCVariableDecl classFieldvariableDecl(String fieldName, String fieldType, Modifier modifier ) {
    	//private AstTreeClass variable = new AstTreeClass()
        // new AstTreeClass()
        JCTree.JCNewClass newClass = treeMaker.NewClass(null, null, treeMaker.Ident(names.fromString(fieldType)), List.nil(), null);
        // private AstTreeClass variable = ...
        JCTree.JCModifiers ms = treeMaker.Modifiers(modifier.ordinal());
        
        JCTree.JCVariableDecl variableDecl = treeMaker.VarDef(ms,
                names.fromString(fieldName), treeMaker.Ident(names.fromString(fieldType)), newClass);
        return variableDecl; 
    }
    
    /**
     * 
     * @param fieldName 变量名称
     * @param fieldType 变量类型
     * @return
     */
    private JCTree.JCVariableDecl methodParamVariableDecl(String fieldName, String fieldType) {
        JCTree.JCVariableDecl variableDecl = treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER),
                names.fromString(fieldName), treeMaker.Ident(names.fromString(fieldType)), null);
        return variableDecl; 
    }
    
    private void importClass(JCTree.JCClassDecl jcClassDecl, String pckName,String clazzName) {
    	// 获取标识符
    	JCTree.JCIdent jcIdent = treeMaker.Ident(names.fromString(pckName));
    	
    	// 获取标识符
    	Name className = names.fromString(clazzName);
    	
    	// 定义访问
    	JCTree.JCFieldAccess jcFieldAccess = treeMaker.Select(jcIdent, className);
    	
    	// 添加import节点
    	JCTree.JCImport anImport = treeMaker.Import(jcFieldAccess, false);
    	
    	jcClassDecl.defs = jcClassDecl.defs.prepend(anImport);
    	//return anImport;
    }
}