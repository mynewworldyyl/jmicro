package cn.jmicro.codegenerator.spoon;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

public class ClassProcessor extends AbstractProcessor<CtClass<?>> {
	
	@Override
	public void process(CtClass<?> ctClass) {
		// Creates field.
		
		final CtTypeReference<Date> dateRef = getFactory().Code().createCtTypeReference(Date.class);
		
		final CtTypeReference<List<Date>> listRef = getFactory().Code().createCtTypeReference(List.class);
		listRef.addActualTypeArgument(dateRef);
		
		final CtField<List<Date>> listOfDates = getFactory().Core().<List<Date>>createField();
		listOfDates.<CtField<List<Date>>>setType(listRef);
		listOfDates.<CtField<List<Date>>>addModifier(ModifierKind.PRIVATE);
		listOfDates.setSimpleName("dates");
		
		//Creates constructor.
		final CtCodeSnippetStatement statementInConstructor = getFactory().Code().createCodeSnippetStatement("this.dates = dates");
		final CtBlock<?> ctBlockOfConstructor = getFactory().Code().createCtBlock(statementInConstructor);
		
		final CtParameter<List<Date>> parameter = getFactory().Core().<List<Date>>createParameter();
		parameter.<CtParameter>setType(listRef);
		parameter.setSimpleName("dates");
		
		final CtConstructor constructor = getFactory().Core().createConstructor();
		constructor.setBody(ctBlockOfConstructor);
		constructor.setParameters(Collections.<CtParameter<?>>singletonList(parameter));
		constructor.addModifier(ModifierKind.PUBLIC);

		// Apply transformation.
		ctClass.addField(listOfDates);
		ctClass.addConstructor(constructor);
		
		System.out.println(ctClass.toString());
	}
}
