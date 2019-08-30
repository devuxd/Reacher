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
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

/**
 * The Plaid class path container creates a library for the Plaid annotation
 * binaries so that users of Crystal can easily add them to the classpath of
 * their project. This example is very closed to the tutorial I found 
 * <a href="http://www.ibm.com/developerworks/edu/os-dw-os-eclipse-classpath.html">here</a>,
 * with the major difference being that I have to look somewhere else for the
 * Plaid annotations jar file. 
 * 
 * @author Nels E. Beckman
 * @date Dec 12, 2008
 *
 */
public class PlaidClasspathContainer implements IClasspathContainer {

	private final IPath path;
	private final IClasspathEntry classpathEntry;
	
	public static final Path ID = new Path("edu.cmu.cs.planno.PLAID_ANNOTATION_CONTAINER");
	
	public PlaidClasspathContainer(IClasspathEntry planno_classpath, IPath path, IJavaProject project) {
		this.path = path;
		this.classpathEntry = planno_classpath;
	}

	public IClasspathEntry[] getClasspathEntries() {
		return new IClasspathEntry[] { classpathEntry };
	}

	public String getDescription() {
		return "Plaid Annotations";
	}

	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	public IPath getPath() {
		return path;
	}
}
