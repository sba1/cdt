/*******************************************************************************
 * Copyright (c) 2013 Sebastian Bauer
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Bauer - Initial API and implementation
 ******************************************************************************/
package org.eclipse.cdt.internal.core.doxygen;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDoxygenComment;
import org.eclipse.cdt.core.dom.ast.IASTDoxygenTag;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

/**
 * A map in which AST objects are associated to documentation.
 *
 * @author Sebastian Bauer
 */
public class DoxygenMap implements IDoxygenMap {
	/**
	 * Find all doxygen comments related with the given function.
	 *
	 * @param sfd
	 * @return
	 */
	private static IASTDoxygenComment [] findRelatedDoxygenComments(IASTNode sfd, DoxygenComments doxygenComments) {
		int offset = sfd.getNodeLocations()[0].getNodeOffset();

		List<IASTDoxygenComment> relatedDoxygenComments = new LinkedList<IASTDoxygenComment>();

		IASTDoxygenComment nearestDoxygenComment = null;

		/* Find best comment nearby */
		for (IASTDoxygenComment doxygenComment : doxygenComments) {
			if (!sfd.getContainingFilename().equals(doxygenComment.getContainingFilename())) {
				continue;
			}

			if (doxygenComment.getNodeLocations()[0].getNodeOffset() < offset) {
				nearestDoxygenComment = doxygenComment;
			}
		}

		if (nearestDoxygenComment != null) {
			relatedDoxygenComments.add(nearestDoxygenComment);
			doxygenComments.prepareForRemoval(nearestDoxygenComment);
			doxygenComments.remove();
		}
		return relatedDoxygenComments.toArray(new IASTDoxygenComment[0]);
	}

	private HashMap<IASTNode,String> map = new HashMap<IASTNode,String>();
	private HashMap<IASTNode,IASTFileLocation> doxygenLocationMap = new HashMap<IASTNode,IASTFileLocation>();

	public void put(IASTNode n, String doc) {
		map.put(n, doc);
	}

	@Override
	public String get(IASTNode n) {
		return map.get(n);
	}


	public void put(IASTNode n, IASTFileLocation doxygenLocation) {
		doxygenLocationMap.put(n, doxygenLocation);
	}

	@Override
	public IASTFileLocation getLocation(IASTNode n) {
		return doxygenLocationMap.get(n);
	}

	/**
	 * Resolves the doxygen documentation and returns a map that
	 * maps every node to a doxygen string.
	 *
	 * @param tu
	 */
	public static DoxygenMap resolveDoxygen(IASTTranslationUnit tu) {
		IASTDeclaration [] declarations = tu.getDeclarations();

		DoxygenMap doxygenMap = new DoxygenMap();
		DoxygenComments doxygenComments = new DoxygenComments(tu);
		//NodeCommentMap commentMap = ASTCommenter.getCommentedNodeMap(tu);

		for (IASTDeclaration d : declarations) {
			IASTNodeLocation [] l = d.getNodeLocations();

			if (d instanceof IASTSimpleDeclaration) {
				IASTSimpleDeclaration sd = (IASTSimpleDeclaration) d;
				IASTDeclarator [] decls = sd.getDeclarators();

				if (decls.length == 1) {
					if (!(decls[0] instanceof IASTStandardFunctionDeclarator))
					{
						System.err.println("Unexpected decl class: " + decls[0]);
						continue;
					}

					extactDoxygen((IASTStandardFunctionDeclarator)decls[0], doxygenMap, doxygenComments);
				} else if (decls.length == 0) {
					if (!(sd.getDeclSpecifier() instanceof IASTCompositeTypeSpecifier)) {
						System.err.println("Unexpected decl specifier class: " + sd.getDeclSpecifier());
						continue;
					}
					IASTCompositeTypeSpecifier cts = (IASTCompositeTypeSpecifier)sd.getDeclSpecifier();

					IASTDoxygenComment [] relatedDoxygenComments = findRelatedDoxygenComments(cts, doxygenComments);

					putDocumentationToMap(doxygenMap, cts, "", "", relatedDoxygenComments); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					System.err.println("Unsupported decl length " + decls.length);
					continue;
				}
			} else if (d instanceof IASTFunctionDefinition) {
				IASTFunctionDefinition fd = (IASTFunctionDefinition)d;
				extactDoxygen(fd, doxygenMap, doxygenComments);
			}
		}
		return doxygenMap;
	}

	/**
	 * Extract the doxygen comments for the given standard function declarator.
	 *
	 * @param sfd
	 * @param doxygenMap
	 * @param doxygenComments
	 */
	private static void extactDoxygen(IASTStandardFunctionDeclarator sfd, DoxygenMap doxygenMap, DoxygenComments doxygenComments) {
		IASTParameterDeclaration [] params = sfd.getParameters();

		IASTDoxygenComment [] relatedDoxygenComments = findRelatedDoxygenComments(sfd, doxygenComments);

		putDocumentationToMap(doxygenMap, sfd, "", "", relatedDoxygenComments); //$NON-NLS-1$ //$NON-NLS-2$
		for (int j=0; j < params.length; j++) {
			String paramName = params[j].getDeclarator().getName().getRawSignature();
			putDocumentationToMap(doxygenMap, params[j], "param", paramName, relatedDoxygenComments); //$NON-NLS-1$
		}
	}

	/**
	 * Extract the doxygen comments for the given function definition.
	 *
	 * @param sfd
	 * @param doxygenMap
	 * @param doxygenComments
	 *
	 * @todo share code with above method better.
	 */
	private static void extactDoxygen(IASTFunctionDefinition fd, DoxygenMap doxygenMap, DoxygenComments doxygenComments) {
		IASTDoxygenComment [] relatedDoxygenComments = findRelatedDoxygenComments(fd, doxygenComments);

		if (fd.getDeclarator() instanceof IASTStandardFunctionDeclarator) {
			IASTStandardFunctionDeclarator sfd = (IASTStandardFunctionDeclarator)fd.getDeclarator();
			IASTParameterDeclaration [] params = sfd.getParameters();

			putDocumentationToMap(doxygenMap, sfd, "", "", relatedDoxygenComments); //$NON-NLS-1$ //$NON-NLS-2$
			for (int j=0; j < params.length; j++) {
				String paramName = params[j].getDeclarator().getName().getRawSignature();
				putDocumentationToMap(doxygenMap, params[j], "param", paramName, relatedDoxygenComments); //$NON-NLS-1$
			}
			putDocumentationToMap(doxygenMap, fd.getDeclSpecifier(), "return", "", relatedDoxygenComments); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * A into the given doxygenMap for the given tag and prefix the documentation from the given doxygen comments for the node.
	 *
	 * @param doxygenMap
	 * @param tagName
	 * @param prefix
	 * @param node
	 * @param relatedDoxygenComments
	 */
	private static void putDocumentationToMap(DoxygenMap doxygenMap, IASTNode node, String tagName, String prefix, IASTDoxygenComment[] relatedDoxygenComments) {
		String functionDesc = buildDocumentationFromDoxygenComments(tagName, prefix, relatedDoxygenComments);
		if (functionDesc != null) {
			doxygenMap.put(node, functionDesc);
		}
		if (relatedDoxygenComments != null && relatedDoxygenComments.length > 0) {
			doxygenMap.put(node, relatedDoxygenComments[0].getFileLocation());
		}
	}

	/**
	 * Build for the given tag name and prefix a documentation String from the given doxygen comments.
	 *
	 * @param tagName
	 * @param prefix
	 * @param relatedDoxygenComments
	 * @return
	 */
	private static String buildDocumentationFromDoxygenComments(String tagName, String prefix, IASTDoxygenComment[] relatedDoxygenComments) {
		StringBuilder str = new StringBuilder();

		for (int i=0; i < relatedDoxygenComments.length; i++) {
			List<?extends IASTDoxygenTag> tags = relatedDoxygenComments[0].tags();
			for (IASTDoxygenTag tag : tags) {
				String name = tag.getName();
				String value = tag.getValue();

				if (tagName.equals(name) && value.startsWith(prefix)) {
					if (prefix.length() == 0) {
						str.append(value);
						continue;
					}
					if (value.length() > prefix.length()) {
						if (Character.isWhitespace(value.charAt(prefix.length()))) {
							str.append(value.substring(prefix.length()).trim());
						}
					}
				}
			}
		}
		return str.toString();
	}
}