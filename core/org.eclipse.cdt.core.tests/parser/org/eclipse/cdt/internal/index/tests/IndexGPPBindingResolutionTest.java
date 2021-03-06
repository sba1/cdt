/*******************************************************************************
 * Copyright (c) 2013 Nathan Ridge.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nathan Ridge - Initial implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.index.tests;

import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.testplugin.TestScannerProvider;
import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.CPPVisitor;

import junit.framework.TestSuite;

/**
 * For testing resolution of bindings in C++ code with GNU extensions.
 */
public abstract class IndexGPPBindingResolutionTest extends IndexBindingResolutionTestBase {

	private static void gnuSetUp() {
		TestScannerProvider.sDefinedSymbols.put("__GNUC__", Integer.toString(GPPLanguage.GNU_LATEST_VERSION_MAJOR));
		TestScannerProvider.sDefinedSymbols.put("__GNUC_MINOR__", Integer.toString(GPPLanguage.GNU_LATEST_VERSION_MINOR));
	}
	
	private static void gnuTearDown() {
		TestScannerProvider.clear();
	}
	
	public class GPPReferencedProject extends ReferencedProject {
		public GPPReferencedProject() {
			super(true /* cpp */);
		}
		
		@Override
		public void setUp() throws Exception {
			gnuSetUp();
			super.setUp();
		}
		
		@Override
		public void tearDown() throws Exception {
			super.tearDown();
			gnuTearDown();
		}
	}

	public class GPPSinglePDOMTestStrategy extends SinglePDOMTestStrategy {
		public GPPSinglePDOMTestStrategy() {
			super(true /* cpp */);
		}
		
		@Override
		public void setUp() throws Exception {
			gnuSetUp();
			super.setUp();
		}
		
		@Override
		public void tearDown() throws Exception {
			super.tearDown();
			gnuTearDown();
		}
	}

	public static class SingleProject extends IndexGPPBindingResolutionTest {
		public SingleProject() { setStrategy(new GPPSinglePDOMTestStrategy()); }
		public static TestSuite suite() { return suite(SingleProject.class); }
	}
	
	public static class ProjectWithDepProj extends IndexGPPBindingResolutionTest {
		public ProjectWithDepProj() { setStrategy(new GPPReferencedProject()); }
		public static TestSuite suite() { return suite(ProjectWithDepProj.class); }
	}
	
	public static void addTests(TestSuite suite) {
		suite.addTest(SingleProject.suite());
		suite.addTest(ProjectWithDepProj.suite());
	}
	
	//	template <typename T>
	//	struct underlying_type {
	//	    typedef __underlying_type(T) type;
	//	};
	//
	//	enum class e_fixed_short1 : short;
	//	enum class e_fixed_short2 : short { a = 1, b = 2 };
	//
	//	enum class e_scoped { a = 1, b = 2 };
	//
	//	enum e_unsigned { a1 = 1, b1 = 2 };
	//	enum e_int { a2 = -1, b2 = 1 };
	//	enum e_ulong { a3 = 5000000000, b3 };
	//	enum e_long { a4 = -5000000000, b4 = 5000000000 };
	
	//	typedef underlying_type<e_fixed_short1>::type short1_type;
	//	typedef underlying_type<e_fixed_short2>::type short2_type;
	//
	//	typedef underlying_type<e_scoped>::type scoped_type;
	//
	//	typedef underlying_type<e_unsigned>::type unsigned_type;
	//	typedef underlying_type<e_int>::type int_type;
	//	typedef underlying_type<e_ulong>::type ulong_type;
	//	typedef underlying_type<e_long>::type loong_type;
	public void testUnderlyingTypeBuiltin_bug411196() throws Exception {
		assertSameType((ITypedef) getBindingFromASTName("short1_type", 0), CPPVisitor.SHORT_TYPE);
		assertSameType((ITypedef) getBindingFromASTName("short2_type", 0), CPPVisitor.SHORT_TYPE);

		assertSameType((ITypedef) getBindingFromASTName("scoped_type", 0), CPPVisitor.INT_TYPE);
		
		assertSameType((ITypedef) getBindingFromASTName("unsigned_type", 0), CPPVisitor.UNSIGNED_INT);
		assertSameType((ITypedef) getBindingFromASTName("int_type", 0), CPPVisitor.INT_TYPE);
		assertSameType((ITypedef) getBindingFromASTName("ulong_type", 0), CPPVisitor.UNSIGNED_LONG);
		assertSameType((ITypedef) getBindingFromASTName("loong_type", 0), CPPVisitor.LONG_TYPE);
	}
}
