package cn.jmicro.codegenerator;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("cn.jmicro.api.annotation.ServiceGen")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ClientServiceProxyGenerator extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		
		 processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "****************found @Log at ClientServiceProxyGenerator");
		for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "****************found @Log at ClientServiceProxyGenerator" + element);
            }
        }
		
		//System.out.println("************************ClientServiceProxyGenerator************************************");
		/*StringBuilder builder = new StringBuilder().append("package com.zhangjian.annotationprocessor.generated;\n\n")
				.append("public class GeneratedClass {\n\n").append("\tpublic String getMessage() {\n")
				.append("\t\treturn \"");

		for (Element element : roundEnv.getElementsAnnotatedWith(ServiceGen.class)) {
			String objectType = element.getSimpleName().toString();
			builder.append(objectType).append(" exists!\\n");
		}
		builder.append("\";\n").append("\t}\n").append("}\n");
		try {
			JavaFileObject source = processingEnv.getFiler()
					.createSourceFile("cn.jmicro.example.GeneratedClass");
			System.out.println(builder.toString());
			processingEnv.getMessager().printMessage(Kind.ERROR, builder.toString());
			Writer writer = source.openWriter();
			writer.write(builder.toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
*/
		return true;
	}

}
