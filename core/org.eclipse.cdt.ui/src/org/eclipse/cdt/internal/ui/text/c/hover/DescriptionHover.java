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

package org.eclipse.cdt.internal.ui.text.c.hover;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorInput;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IDescription;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IParameter;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.ui.CUIPlugin;

import org.eclipse.cdt.internal.core.CCoreInternals;

/**
 * Class implementing a text hover using description stored in the IBinding.
 */
public class DescriptionHover extends AbstractCEditorTextHover {

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (getEditor() == null) return null;

		IEditorInput editorInput = getEditor().getEditorInput();
		ITranslationUnit itu = CUIPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editorInput);

		try {
			IASTName hoveredName = itu.getAST().getNodeSelector(null).findEnclosingName(hoverRegion.getOffset(), 1);

			return getDescription(itu, hoveredName);
		} catch (Exception e) {
			/* Ignore */
		}
		return null;
	}

	/**
	 * Return the description string for the given name in the given translation unit.
	 *
	 * @param itu
	 * @param name
	 * @return
	 * @throws CoreException
	 */
	private String getDescription(ITranslationUnit itu, IASTName name) throws CoreException {
		ASTVisitor visit = new ASTVisitor(true) {
			@Override
			public int visit(IASTName name) {
				name.resolveBinding();
				return PROCESS_CONTINUE;
			}
		};
		IASTTranslationUnit ast = itu.getAST();
		ast.accept(visit);

		IBinding binding = name.resolveBinding();
		String description = null;
		IDescription desc = null;
		if (binding != null) {
			desc = (IDescription)binding.getAdapter(IDescription.class);
			if (desc != null)
				description = desc.getDescription();
		}

		if (description == null || description.length() == 0) {
			/* Attempt to find the description via the pdom/index */
			binding = CCoreInternals.getPDOMManager().getPDOM(itu.getCProject()).findBinding(name);
			if (binding != null)
				desc = (IDescription)binding.getAdapter(IDescription.class);
		}

		/* Parameter description */
		StringBuilder parameterDescription = new StringBuilder();
		if (binding instanceof IFunction) {
			IFunction function = (IFunction)binding;
			for (IParameter p : function.getParameters()) {
				IDescription pDesc = (IDescription)p.getAdapter(IDescription.class);

				parameterDescription.append("<b>"); //$NON-NLS-1$
				parameterDescription.append(p.getName());
				parameterDescription.append(":</b> "); //$NON-NLS-1$
				if (pDesc != null) {
					parameterDescription.append(pDesc.getDescription());
				}
				parameterDescription.append("<br>"); //$NON-NLS-1$
			}
		}

		if (desc != null) {
			description = desc.getDescription();
			if (description == null || description.length() == 0) {
				description = parameterDescription.toString();
			} else {
				description = description + "<br>" + parameterDescription.toString(); //$NON-NLS-1$
			}

			if (description != null && description.length() > 0)
				return description;
		}
		return null;
	}
}
