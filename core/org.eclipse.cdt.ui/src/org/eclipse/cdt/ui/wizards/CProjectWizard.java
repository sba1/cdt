package org.eclipse.cdt.ui.wizards;

/*
 * (c) Copyright QNX Software Systems Ltd. 2002.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.ICProjectDescriptor;
import org.eclipse.cdt.core.ICProjectOwnerInfo;
import org.eclipse.cdt.internal.ui.CPlugin;
import org.eclipse.cdt.internal.ui.CPluginImages;
import org.eclipse.cdt.utils.ui.swt.IValidation;


/**
 * C Project wizard that creates a new project resource in
 */
public abstract class CProjectWizard extends BasicNewResourceWizard implements IExecutableExtension {

	private static final String OP_ERROR= "CProjectWizard.op_error"; //$NON-NLS-1$
	private static final String OP_DESC= "CProjectWizard.op_description"; //$NON-NLS-1$

	private static final String PREFIX= "CProjectWizard"; //$NON-NLS-1$
	private static final String WZ_TITLE= "CProjectWizard.title"; //$NON-NLS-1$
	private static final String WZ_DESC= "CProjectWizard.description"; //$NON-NLS-1$

	private static final String WINDOW_TITLE = "CProjectWizard.windowTitle"; //$NON-NLS-1$

	private String wz_title;
	private String wz_desc;
	private String op_error;

	protected IConfigurationElement fConfigElement;
	protected CProjectWizardPage fMainPage; 
	protected TabFolderPage fTabFolderPage; 
	protected IProject newProject;

	List tabItemsList = new ArrayList();

	public CProjectWizard() {
		this(CPlugin.getResourceString(WZ_TITLE), CPlugin.getResourceString(WZ_DESC), 
			CPlugin.getResourceString(OP_ERROR));
	}

	public CProjectWizard(String title, String description) {
		this(title, description, CPlugin.getResourceString(OP_ERROR));
	}

	public CProjectWizard(String title, String description, String error) {
		super();
		setDialogSettings(CPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		wz_title = title;
		wz_desc = description;
		op_error = error;
	}

	/** 
	 * @see Wizard#createPages
	 */		
	public void addPages() {
		fMainPage= new CProjectWizardPage(CPlugin.getResourceString(PREFIX));
		fMainPage.setTitle(wz_title);
		fMainPage.setDescription(wz_desc);
		addPage(fMainPage);

		fTabFolderPage = new TabFolderPage(this);
		addPage(fTabFolderPage);
	}

	public void addTabItem(IWizardTab item) {
		tabItemsList.add(item);
	}

	public IWizardTab [] getTabItems() {
		return (IWizardTab[])tabItemsList.toArray(new IWizardTab[0]);
	}

	public abstract void addTabItems(TabFolder tabFolder);

	protected abstract void doRunPrologue(IProgressMonitor monitor);

	protected abstract void doRunEpilogue(IProgressMonitor monitor);

	/**
	 * Gets the project location path from the main page
	 * Overwrite this method if you do not have a main page
	 */
	protected IPath getLocationPath() {
		return fMainPage.getLocationPath();
	}

	/**
	 * Gets the project handle from the main page.
	 * Overwrite this method if you do not have a main page
	 */

	protected IProject getProjectHandle() {
		return fMainPage.getProjectHandle();
	}

	/**
	 * Returns the C project handle corresponding to the project defined in
	 * in the main page.
	 *
	 * @returns the C project
	 */    
	protected IProject getNewProject() {
		return newProject;
	}

	protected IResource getSelectedResource() {
		return getNewProject();
	}

	protected IValidation getValidation() {
		return fTabFolderPage; 
	}

	/**
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		if (!invokeRunnable(getRunnable())) {
			return false;
		}
		BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
		IResource resource = getSelectedResource();
		selectAndReveal(resource);
		if (resource != null && resource.getType() == IResource.FILE) {
			IFile file = (IFile)resource;
			// Open editor on new file.
			IWorkbenchWindow dw = getWorkbench().getActiveWorkbenchWindow();
			if (dw != null) {
				try {
					IWorkbenchPage page = dw.getActivePage();
					if (page != null)
						page.openEditor(file);
				} catch (PartInitException e) {
					MessageDialog.openError(dw.getShell(),
						CPlugin.getResourceString(OP_ERROR), e.getMessage());
				}
			}
		}
		return true;
	}

	/**
	 * Stores the configuration element for the wizard.  The config element will be used
	 * in <code>performFinish</code> to set the result perspective.
	 *
	 * @see IExecutableExtension#setInitializationData
	 */
	public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
		fConfigElement= cfig;
	}
	
	/*
	 * Reimplemented method from superclass
	 */
	protected void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(CPluginImages.DESC_WIZABAN_NEW_PROJ);
	}

	/* (non-Javadoc)
	 * Method declared on IWorkbenchWizard.
	 */
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle(CPlugin.getResourceString(WINDOW_TITLE));
	}

	public IRunnableWithProgress getRunnable() {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				if (monitor == null) {
					monitor= new NullProgressMonitor();
				}
				monitor.beginTask(CPlugin.getResourceString(OP_DESC), 3);

				doRunPrologue(new SubProgressMonitor(monitor, 1));
				try {
					doRun(new SubProgressMonitor(monitor, 1));
				}
				catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
				doRunEpilogue(new SubProgressMonitor(monitor, 1));

				monitor.done();
			}
		};
	}

	/**
	 * Utility method: call a runnable in a WorkbenchModifyDelegatingOperation
	 */
	protected boolean invokeRunnable(IRunnableWithProgress runnable) {
		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(runnable);
		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			Shell shell= getShell();
			String title= CPlugin.getResourceString(OP_ERROR + ".title"); //$NON-NLS-1$
			String message= CPlugin.getResourceString(OP_ERROR + ".message"); //$NON-NLS-1$
                       
			Throwable th= e.getTargetException();
			if (th instanceof CoreException) {
				IStatus status= ((CoreException)th).getStatus();
				if (status != null) {
					ErrorDialog.openError(shell, title, message, status);
					CPlugin.log(status);
					return false;
				}
			}
			MessageDialog.openError(shell, title, message);
			CPlugin.log(th);
			try {
				getProjectHandle().delete(true, true, null);
			}
			catch (CoreException ignore) {
			}
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		return true;
	}

	protected void doRun(IProgressMonitor monitor) throws CoreException {
		createNewProject(monitor);
	}	

	/**
	 * Creates a new project resource with the selected name.
	 * <p>
	 * In normal usage, this method is invoked after the user has pressed Finish on
	 * the wizard; the enablement of the Finish button implies that all controls
	 * on the pages currently contain valid values.
	 * </p>
	 * <p>
	 * Note that this wizard caches the new project once it has been successfully
	 * created; subsequent invocations of this method will answer the same
	 * project resource without attempting to create it again.
	 * </p>
	 *
	 * @return the created project resource, or <code>null</code> if the project
	 *    was not created
	 */
	protected IProject createNewProject(IProgressMonitor monitor) throws CoreException {

		if (newProject != null)
			return newProject;

		// get a project handle
		IProject newProjectHandle = getProjectHandle();

		// get a project descriptor
		IPath defaultPath = Platform.getLocation();
		IPath newPath = getLocationPath();
		if (defaultPath.equals(newPath))
			newPath = null;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());
		description.setLocation(newPath);

		newProject = createProject(description, newProjectHandle, monitor);
		return newProject;
	}

	/**
	 * Creates a project resource given the project handle and description.
	 *
	 * @param description the project description to create a project resource for
	 * @param projectHandle the project handle to create a project resource for
	 * @param monitor the progress monitor to show visual progress with
	 *
	 * @exception CoreException if the operation fails
	 * @exception OperationCanceledException if the operation is canceled
	 */
	private IProject createProject(IProjectDescription description, IProject projectHandle,
		IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			if (monitor == null) {
				monitor = new NullProgressMonitor();
			}
			monitor.beginTask("Creating C Project", 3);//$NON-NLS-1$

			projectHandle.create(description, new SubProgressMonitor(monitor, 1));

			if (monitor.isCanceled())
				throw new OperationCanceledException();

			// Open first.
			projectHandle.open(new SubProgressMonitor(monitor, 1));
			// Add C Nature.
			CProjectNature.addCNature(projectHandle, new SubProgressMonitor(monitor, 1));
			CCorePlugin.getDefault().mapCProjectOwner(projectHandle, getProjectID());
		} finally {
			//monitor.done();
		}
		return projectHandle;
	}

	/**
	 * Method getID.
	 * @return String
	 */
	public abstract String getProjectID();

}
