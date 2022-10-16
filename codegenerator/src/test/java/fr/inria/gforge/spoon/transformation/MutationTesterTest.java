package fr.inria.gforge.spoon.transformation;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import cn.jmicro.codegenerator.spoon.EncodeDecodeMethodProcessor;
import fr.inria.gforge.spoon.transformation.mutation.MutantNotKilledException;
import fr.inria.gforge.spoon.transformation.mutation.MutationTester;
import fr.inria.gforge.spoon.transformation.mutation.TestDriver;
import spoon.reflect.declaration.CtClass;

public class MutationTesterTest {

	@Test
	public void testMutationTester() throws Exception {
		// mutation testing requires three things
		// 1. the code to be mutated
		// 2. the test driver to kill the mutants 
		// 3. the mutation operator
		String codeToBeMutated = "D:\\opensource\\github\\jmicro\\api\\src\\main\\java\\cn\\jmicro\\api\\ds\\ApiReqJRso.java";
		
		TestDriver<IFoo> testDriverForIFooObjects = new TestDriver<IFoo>() {
			@Override
			public void test(IFoo t) {
				assertEquals(2, t.m());
				assertEquals(6, t.n());		
			}
		};
		
		EncodeDecodeMethodProcessor mutationOperator = new EncodeDecodeMethodProcessor();

		// we instantiate the mutation tester
		MutationTester<IFoo> mutationTester = new MutationTester<>(codeToBeMutated, testDriverForIFooObjects, mutationOperator);
		
		// generating the mutants
		mutationTester.generateMutants();
		List<CtClass> mutants = mutationTester.getMutants();
		assertEquals(2, mutants.size());
		
		// killing the mutants, no exception should be thrown
		try {
			mutationTester.killMutants();				
		} catch (MutantNotKilledException e) {
			Assert.fail();
		}
		
		// another couple of assertions for testing that mutants are actually mutants
		// testing the first mutant
		// 1-1 = 0
		assertEquals(0, mutationTester.mutantInstances.get(0).m());
		assertEquals(6, mutationTester.mutantInstances.get(0).n());
		
		// testing the second mutant
		assertEquals(2, mutationTester.mutantInstances.get(1).m());
		// 2-3 = -1
		assertEquals(-1, mutationTester.mutantInstances.get(1).n());
	}
}
