/*******************************************************************************
 * Copyright (c) 2013 Sebastian Bauer
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Bauer - Initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.internal.core.parser.scanner;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

/**
 * Helper class for parsing Doxygen comments.
 */
public class DoxygenParser {
	private static class DoxygenParserContext {
		int offset;
		int endOffset;
		AbstractCharArray input;

		char getCurrentChar() {
			return input.get(offset);
		}

		boolean finished() {
			if (offset >= endOffset) return true;
			return false;
		}
		boolean next() {
			if (finished()) return false;
			offset++;
			return true;
		}
	}

	/**
	 * Parses and return the tagname that starts under the current offset.
	 *
	 * @param pc
	 * @return
	 */
	private static String parseDoxygenTagName(DoxygenParserContext pc) {
		if (pc.getCurrentChar() == '@')
		{
			pc.next();
			if (pc.finished())
				return ""; //$NON-NLS-1$
		}

		int start = pc.offset;
		do
		{
			if (Character.isWhitespace(pc.getCurrentChar()))
				break;
		} while (pc.next());
		char [] tagName = new char[pc.offset - start];
		pc.input.arraycopy(start, tagName, 0, pc.offset - start);
		return new String(tagName);
	}

	/**
	 * Parses and skips the starting part of a comment line.
	 *
	 * @param pc
	 * @return
	 */
	private static boolean parseDoxygenStartingComment(DoxygenParserContext pc) {
		do {
			char currentChar = pc.getCurrentChar();
			if (currentChar != '*' && currentChar != '/' && !Character.isWhitespace(currentChar)) {
				return true;
			}
		} while (pc.next());
		return true;
	}

	/**
	 * Parses and skips the ending part of a comment line.
	 *
	 * @param pc
	 * @return
	 */
	private static boolean parseDoxygenEndingComment(DoxygenParserContext pc) {
		int enterOffset = pc.offset;
		boolean rc = true;

		do {
			char currentChar = pc.getCurrentChar();

			if (currentChar == '\n')
				break;

			if (currentChar != '*' && currentChar != ' ' && currentChar != '/') {
				pc.offset = enterOffset;
				rc = false;
				break;
			}
		} while (pc.next());
		return rc;
	}

	/**
	 * Parse the doxygen tag value that starts under the current offset.
	 *
	 * @param pc
	 * @return
	 */
	private static String parseDoygenTagValue(DoxygenParserContext pc) {
		StringBuilder str = new StringBuilder(100);

		outerloop:
		while (!pc.finished())
		{
			innerloop:
			while (!pc.finished())
			{
				parseDoxygenStartingComment(pc);

				do {
					if (parseDoxygenEndingComment(pc))
					{
						str.append(' ');
						break innerloop;
					}

					char currentChar = pc.getCurrentChar();
					if (currentChar == '@')
						break outerloop;

					str.append(currentChar);
				} while (pc.next());
			}
		}
		return new String(str.toString()).trim();
	}

	/**
	 * Parse the given offset as a Doxygen comment. This includes also the beginning of the comment marks.
	 *
	 * @param translationUnit the translation unit where this comment belongs to
	 * @param filePath the name of the file in which this comment is located
	 * @param offset the start offset of the comment
	 * @param endOffset the ned offset of the comment
	 * @param isBlockComment
	 * @param input
	 *
	 * @return a parsed Doxygen comment
	 */
	public static ASTDoxygenComment parseDoxygenComment(IASTTranslationUnit translationUnit, String filePath, int offset, int endOffset, boolean isBlockComment, AbstractCharArray input) {
		DoxygenParserContext pc = new DoxygenParserContext();
		pc.offset = offset;
		pc.endOffset = endOffset;
		pc.input = input;

		ASTDoxygenComment comment = new ASTDoxygenComment(translationUnit, filePath, offset, endOffset, isBlockComment);

		String currentDoxygenTagName = ""; //$NON-NLS-1$
		String currentDoxygenTagValue = ""; //$NON-NLS-1$

		do
		{
			currentDoxygenTagValue = parseDoygenTagValue(pc);

			ASTDoxygenTag doxygenTag = new ASTDoxygenTag(currentDoxygenTagName, currentDoxygenTagValue);
			comment.addTag(doxygenTag);

			currentDoxygenTagName = parseDoxygenTagName(pc);
			currentDoxygenTagValue = ""; //$NON-NLS-1$
		} while (pc.next());

		if (currentDoxygenTagValue.length() > 0) {
			ASTDoxygenTag doxygenTag = new ASTDoxygenTag(currentDoxygenTagName, currentDoxygenTagValue);
			comment.addTag(doxygenTag);
		}
		return comment;
	}
}
