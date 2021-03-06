package org.eclipse.cdt.core.parser.tests.doxygen;

import static org.eclipse.cdt.core.parser.ParserLanguage.C;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDoxygenComment;
import org.eclipse.cdt.core.dom.ast.IASTDoxygenTag;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.parser.IScanner;
import org.eclipse.cdt.core.parser.tests.ast2.AST2TestBase;
import org.eclipse.cdt.internal.core.doxygen.DoxygenMap;

public class DoxygenMapTest extends AST2TestBase {
	/**
	 * Configure the scanner. Here, we enable the scanner to look for
	 * doxygen tags.
	 */
	protected void configureScanner(IScanner scanner) {
		super.configureScanner(scanner);
		scanner.setParseDoxygen(true);
	}

	//	/**
	//	 * A doxygen comment.
	//	 * Second line.
	//	 *
	//	 * @param arg1
	//	 * @param arg2
	//	 *  description
	//	 * @return return value
	//	 */
	public void testDoxygenComments() throws Exception {
		IASTTranslationUnit tu = parseAndCheckBindings(getAboveComment(), C);

		IASTComment[] comments = tu.getComments();
		assertEquals(1, comments.length);
		assertTrue(comments[0] instanceof IASTDoxygenComment);

		IASTDoxygenComment doxygenComment = (IASTDoxygenComment)comments[0];
		List<?extends IASTDoxygenTag> tags = doxygenComment.tags();
		assertEquals(4, tags.size());

		assertEquals("", tags.get(0).getName());
		assertEquals("A doxygen comment. Second line.", tags.get(0).getValue());
		assertEquals("param", tags.get(1).getName());
		assertEquals("arg1", tags.get(1).getValue());
		assertEquals("param", tags.get(2).getName());
		assertEquals("arg2 description", tags.get(2).getValue());
		assertEquals("return", tags.get(3).getName());
		assertEquals("return value", tags.get(3).getValue());
	}

	//	/**
	//	 * A test function.
	//	 *
	//	 * @param arg1 first argument
	//	 * @return return value
	//	 */
	//	int test(int arg1);
	public void testDoxygenCommentsBeforeFunction() throws Exception {
		IASTTranslationUnit tu = parseAndCheckBindings(getAboveComment(), C);
		assertEquals(1, tu.getDeclarations().length);

		IASTSimpleDeclaration decl = getDeclaration(tu, 0);
		IASTDeclarator [] decls = decl.getDeclarators();
		IASTFunctionDeclarator fd = (IASTFunctionDeclarator)decls[0];
		assertTrue(fd instanceof IASTStandardFunctionDeclarator);
		IASTStandardFunctionDeclarator sfd = (IASTStandardFunctionDeclarator) fd;
		assertEquals(1, sfd.getParameters().length);

		IASTComment[] comments = tu.getComments();
		assertEquals(1, comments.length);
		assertTrue(comments[0] instanceof IASTDoxygenComment);

		IASTDoxygenComment doxygenComment = (IASTDoxygenComment)comments[0];
		List<?extends IASTDoxygenTag> tags = doxygenComment.tags();
		assertEquals(3, tags.size());

		assertEquals("", tags.get(0).getName());
		assertEquals("A test function.", tags.get(0).getValue());
		assertEquals("param", tags.get(1).getName());
		assertEquals("arg1 first argument", tags.get(1).getValue());
		assertEquals("return", tags.get(2).getName());
		assertEquals("return value", tags.get(2).getValue());

		DoxygenMap doxygenMap = DoxygenMap.resolveDoxygen(tu);
		assertEquals("A test function.", doxygenMap.get(sfd));

		IASTParameterDeclaration [] params = sfd.getParameters();
		assertEquals("first argument", doxygenMap.get(params[0]));
	}

	//	/**
	//	 * A test function.
	//	 *
	//	 * @param arg1 first argument
	//	 * @return return value
	//	 */
	//	int test(int arg1)
	//	{
	//	    return 0;
	//	}
	public void testDoxygenCommentBeforeFunctionDefinition() throws Exception {
		IASTTranslationUnit tu = parseAndCheckBindings(getAboveComment(), C);
		assertEquals(1, tu.getDeclarations().length);

		IASTComment[] comments = tu.getComments();
		assertEquals(1, comments.length);

		IASTFunctionDefinition fd = getDeclaration(tu, 0);
		DoxygenMap doxygenMap = DoxygenMap.resolveDoxygen(tu);

		IASTStandardFunctionDeclarator sfd = (IASTStandardFunctionDeclarator)fd.getDeclarator();
		IASTParameterDeclaration [] params = sfd.getParameters();

		assertEquals("A test function.", doxygenMap.get(sfd));
		assertEquals("first argument", doxygenMap.get(params[0]));
		assertEquals("return value", doxygenMap.get(fd.getDeclSpecifier()));

		assertNotNull(doxygenMap.getLocation(sfd));
		assertEquals(1, doxygenMap.getLocation(sfd).getNodeOffset());
		assertEquals(89, doxygenMap.getLocation(sfd).getNodeLength());
	}
}
