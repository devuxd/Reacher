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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Bundle;

/**
 * This class is an extension point that will construct a
 * PlaidClasspathContainer. The container is what allows us to have a
 * Plaid annotations library that will always link to the correct jar
 * file. We compute the location of that jar file in this class, and
 * construct a PlaidClasspathContainer with this class path location.
 * 
 * @author Nels E. Beckman
 * @date Dec 12, 2008
 *
 */
public class PlaidClasspathContainerInitializer extends
		ClasspathContainerInitializer {

	/** This just goes here for now, since there is nowhere else to put it. */
	public static final String PLANNO_PLUGIN_ID = "edu.cmu.cs.planno";
	
	public static final String PLANNO_JAR_NAME = "plaid-annotations.jar";
	
	// This constructor is required.
	public PlaidClasspathContainerInitializer() {
		
	}

	@Override
	public void initialize(IPath containerPath, IJavaProject project)
			throws CoreException {
		IClasspathEntry planno_path = getPlaidAnnotationsLibraryEntry();
		
		PlaidClasspathContainer container = new PlaidClasspathContainer(planno_path, containerPath, project);
		JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project}, 
				new IClasspathContainer[] {container}, null);
	}

	private static Bundle getBundle(String plugin_id) {
		Bundle b = Platform.getBundle(plugin_id);
		
		// This would be a good place to tell the user they need to have
		// Plaid annotations installed. NEB
		if( b == null )
			throw new NullPointerException();
		
		return b;
	}
	
	private static IPath getBundleLocation(Bundle b) {
		URL local = null;
		
		try {
			local = FileLocator.toFileURL(b.getEntry("/"));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		String fullPath= new File(local.getPath()).getAbsolutePath();
		return Path.fromOSString(fullPath);
	}
	
	public static IClasspathEntry getPlaidAnnotationsLibraryEntry() {
		Bundle bundle = getBundle(PLANNO_PLUGIN_ID);
		IPath bundle_loc = getBundleLocation(bundle);
		
		IPath jar_loc = bundle_loc.append(PLANNO_JAR_NAME);
		
		// This code assumes that the jar file we have put into the project
		// also contains all of the source in a parallel directory.
		return JavaCore.newLibraryEntry(jar_loc, jar_loc, null, 
				new IAccessRule[] {}, new IClasspathAttribute[] {}, false);
	}
}
