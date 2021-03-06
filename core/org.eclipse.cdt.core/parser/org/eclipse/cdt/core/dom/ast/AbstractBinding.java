package org.eclipse.cdt.core.dom.ast;

import org.eclipse.cdt.internal.core.doxygen.DoxygenUtil;
import org.eclipse.core.runtime.PlatformObject;

public abstract class AbstractBinding extends PlatformObject implements IBinding {
	/** The description of this binding */
	private String description;

	/** The location of an associated Doxygen comment */
	private IASTFileLocation doxygenCommentLocation;

	private IDescription descriptionAdapter;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.isAssignableFrom(IDescription.class)) {
			if (descriptionAdapter == null) {
				descriptionAdapter = new IDescription() {
					@Override
					public String getDescription() {
						return description;
					}

					@Override
					public IASTFileLocation getDoxygenCommentLocation() {
						return doxygenCommentLocation;
					}
				};
			}
			return descriptionAdapter;
		}
		return super.getAdapter(adapter);
	}

	protected void updateDescription(IASTNode node) {
		/* TODO: Accumulate somehow if multiple description exists */
		if (description == null || description.length() == 0) {
			description = DoxygenUtil.getDescription(node);
			if (description != null) {
				doxygenCommentLocation = DoxygenUtil.getDoxygenLocation(node);
			}
		} else {
			if (doxygenCommentLocation == null) {
				doxygenCommentLocation = DoxygenUtil.getDoxygenLocation(node);
			}
		}
	}
}
