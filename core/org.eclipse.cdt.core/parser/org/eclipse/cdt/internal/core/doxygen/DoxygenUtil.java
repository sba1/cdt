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

import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * Some utility functions to deal with Doxygen descriptions.
 */
public class DoxygenUtil {
	/**
	 * Return the description stored in the DoxygenMap of the translation unit
	 * for the given node.
	 *
	 * @param node
	 * @return the description or null.
	 */
	public static String getDescription(IASTNode node) {
		IDoxygenMap doxygenMap = (IDoxygenMap)node.getTranslationUnit().getAdapter(IDoxygenMap.class);
		if (doxygenMap != null)
			return doxygenMap.get(node);
		return null;
	}
}
