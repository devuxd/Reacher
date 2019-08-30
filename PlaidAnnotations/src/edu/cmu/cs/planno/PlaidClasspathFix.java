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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.swt.graphics.Image;

/**
 * A classpath fix for the plaid annotations. A classpath fix isn't as nice
 * as the normal quickfix because the user first has to select "Fix project setup..."
 * which they might not always expect to work.
 * 
 * @author Nels E. Beckman
 * @date Dec 12, 2008
 */
public class PlaidClasspathFix extends ClasspathFixProcessor {
	
	private static final String PLURAL_ANNOTATION_PKG = "edu.cmu.cs.plural.annot";
	private static final String CRYSTAL_ANNOTATION_PKG = "edu.cmu.cs.crytal.annotations";
	
	/** annotations from edu.cmu.cs.plural.annot */
	private static final Set<String> PLURAL_CLASS_NAMES;
	
	private static final String PLURAL_PACKAGE_NAME = "edu.cmu.cs.plural.annot";
	
	/** annotations from edu.cmu.cs.crystal.annotations */
	private static final Set<String> CRYSTAL_CLASS_NAMES;
	
	private static final String CRYSTAL_PACKAGE_NAME = "edu.cmu.cs.crystal.annotations";
	
	static {
		String[] plaid_names = 
		{ 
			"Apply", "ApplyToSuper", "Capture", "Cases", "ClassStates", "Exact", 
			"FalseIndicates", "ForcePack", "Full", "Fulls", "Imm", "Imms", "In",
			"IsResult", "Lend", "NoEffects","NonReentrant","Param","Perm","PolyVar",
			"Pures","Pure", "Range","Refine","Release","ResultApply", "ResultFull",
			"ResultImm", "ResultPolyVar","ResultPure","ResultShare",
			"ResultUnique","Share","Shares", "Similar", "State","States", "Symmetric",
			"TrueIndicates", "Unique","Uniques", "Use" 
		};
		
		String[] crystal_names =
		{
			"FailingTest","MultiAnnotation","PassingTest","UseAnalyses"
		};
		
		PLURAL_CLASS_NAMES = new HashSet<String>();
		for(String s : plaid_names)
			PLURAL_CLASS_NAMES.add(s);
		
		CRYSTAL_CLASS_NAMES = new HashSet<String>();
		for(String s : crystal_names)
			CRYSTAL_CLASS_NAMES.add(s);
		
	}
	
	public static boolean isProbablyPlaidAnno(String name) {
		return name.startsWith(PLURAL_ANNOTATION_PKG) ||  
			name.startsWith(CRYSTAL_ANNOTATION_PKG) ||
			PLURAL_CLASS_NAMES.contains(name) ||
			CRYSTAL_CLASS_NAMES.contains(name);
	}
	
	/**
	 * What is our best guess for the fully qualified type name of the
	 * given string? If it's a plaid annotation, we should probably
	 * know, otherwise we just return the name.
	 */
	public static String bestGuessQualifiedName(String name) {
		if( isProbablyPlaidAnno(name) ) {
			if( PLURAL_CLASS_NAMES.contains(name) )
				return PLURAL_PACKAGE_NAME + "." + name;
			
			if( CRYSTAL_CLASS_NAMES.contains(name) )
				return CRYSTAL_PACKAGE_NAME + "." + name;
		}
		 
		return name;
	}
	
	@Override
	public ClasspathFixProposal[] getFixImportProposals(IJavaProject project,
			String name) throws CoreException {

		if( isProbablyPlaidAnno(name) ) {
			// This is probably ours.
			return new ClasspathFixProposal[] { new PlaidFixProposal(project) };
		}
		
		return null;
	}


	private class PlaidFixProposal extends ClasspathFixProposal {

		private final IJavaProject project;

		public PlaidFixProposal(IJavaProject project) {
			super();
			this.project = project;
		}

		@Override
		public Change createChange(IProgressMonitor monitor)
				throws CoreException {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}
			monitor.beginTask("Adding Plaid annotations to classpath.", 1);
			try {
				// This 'new' classpath is a combination of the old one, plus the plaid
				// class path.
				IClasspathEntry[] old_classpath = project.getRawClasspath();
				
				// We do want to see if Plaid annotations is already on the classpath.
				IPath planno_container_path = PlaidClasspathContainer.ID;
				for( IClasspathEntry curr : old_classpath ) {
					if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
						IPath path= curr.getPath();
						if (path.equals(planno_container_path)) {
							return new NullChange(); // already on build path
						}
					}
				}
				
				IClasspathEntry[] new_classpath = new IClasspathEntry[old_classpath.length + 1];
				System.arraycopy(old_classpath, 0, new_classpath, 0, old_classpath.length);
				
				// We put our entry in as the last element.
		        IClasspathEntry new_entry = JavaCore.newContainerEntry(planno_container_path);
				new_classpath[old_classpath.length] = new_entry;
				
				Change result = newClasspathChange(project, new_classpath, project.getOutputLocation());
				return result;
			} finally {
				monitor.done();
			}
		}

		@Override
		public String getAdditionalProposalInfo() {
			return "This will add the Plaid Annotations to the project classpath. If you are using a Crystal analysis, this is probably important.";
		}

		@Override
		public String getDisplayString() {
			return "Add Plaid Annotations";
		}

		@Override
		public Image getImage() {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
		}

		@Override
		public int getRelevance() {
			// Why 15? I have no idea. It's what JUnit returns, and it's supposed to be between 1 and 100.
			return 15;
		}
		
	}
}
