package cn.jmicro.codegenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.naming.spi.ObjectFactory;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Interceptor;
import cn.jmicro.api.annotation.JMicroComponent;
import cn.jmicro.api.annotation.PostListener;
import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.annotation.Server;
import cn.jmicro.api.annotation.Service;

/*@SupportedAnnotationTypes(value= {"cn.jmicro.api.annotation.Interceptor",
		"cn.jmicro.api.annotation.JMicroComponent",
		"cn.jmicro.api.annotation.Reference",
		"cn.jmicro.api.annotation.Reference",
		"cn.jmicro.api.annotation.Server"})*/
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class JMicroComponentScanner extends AbstractProcessor {
	
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> set = new HashSet<>();
		set.add(Interceptor.class.getCanonicalName());
		//set.add(JMicroComponent.class.getCanonicalName());
		set.add(Service.class.getCanonicalName());
		set.add(Server.class.getCanonicalName());
		set.add(Component.class.getCanonicalName());
		set.add(ObjectFactory.class.getCanonicalName());
		set.add(SO.class.getCanonicalName());
		set.add(PostListener.class.getCanonicalName());
		return set;
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		
		StringBuffer sb = new StringBuffer();
		
		for(TypeElement ate : annotations) {
			for (Element element : roundEnv.getElementsAnnotatedWith(ate)) {
			     oneFileElement(sb,element);
			}
		}
		
		try {
			writeFile(sb);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	private void writeFile(StringBuffer sb) throws IOException {
		if(sb.length() < 2) return;
		sb.delete(0, 1);
		
		System.out.println("JMicroComponents: "+sb.toString());
		Filer filer = this.processingEnv.getFiler();
		FileObject file = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "jmicro.properties");
		
		String uri = file.toUri().toString();
		System.out.println("JMicroComponents src path: " + file.toUri().toString());
		
		String p = uri.substring(0, uri.indexOf("/src/main/gen"));
		if(p.startsWith("file:/")) {
			p = p.substring("file:/".length());
		}
		
		File dir = new File(p+"/target/classes/META-INF/jmicro");
		if(!dir.exists()) {
			dir.mkdirs();
		}
		
		File f = new File(dir,"jmicro.properties");
		if(!f.exists()) {
			f.createNewFile();
		}
		
		System.out.println("JMicroComponents component file: " + f.getAbsolutePath());
		
		Writer w = new OutputStreamWriter(new FileOutputStream(f));
		w.append("components=").append(sb.toString());
		w.close();
	}

	private void oneFileElement(StringBuffer sb,Element element) {
		if(element instanceof TypeElement) {
			//System.out.println(element.toString());
			TypeElement typeElement = (TypeElement)element;
			String srcTn = typeElement.getQualifiedName().toString();
			sb.append(",").append(srcTn);
		}
	}
}
