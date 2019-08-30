/*
 * Copyright (c) 2006-2008 by Carnegie Mellon University and others.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. Neither the names of the authors nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.cmu.cs.planno;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
/**
 * This wizard currently does nothing at all except generate the classpath
 * container string that will be added to a project's .classpath file. Once
 * this string is added to the .classpath file, the initializer will be called.
 * 
 * @see {@link PlaidClasspathContainerInitializer}
 * 
 * @author Nels E. Beckman
 * @date Dec 12, 2008
 *
 */
public class PlaidAnnotationContainerPage extends WizardPage 
	implements IClasspathContainerPage, IClasspathContainerPageExtension {

    /**
     * Default Constructor - sets title, page name, description
     */
    public PlaidAnnotationContainerPage() {
        super("Add Plaid Annotations", "Add Plaid Annotations Wizard", 
        		ImageDescriptor.createFromImage(JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY)));
        setDescription("This wizard will help to add the Plaid annotations to your classpath.");
        setPageComplete(true);
    }
    
    public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
    }
    
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));
        composite.setFont(parent.getFont());
        
        setControl(composite);    
    }
      
    public boolean finish() {  
        return true;        
    }

    public IClasspathEntry getSelection() {
        IPath containerPath = PlaidClasspathContainer.ID;
        return JavaCore.newContainerEntry(containerPath);
    }

    public void setSelection(IClasspathEntry containerEntry) {
       
    }  
}
