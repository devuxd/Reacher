/**
 * Copyright (c) 2006-2009 by Carnegie Mellon University and others.
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
package edu.cmu.cs.plural.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation used to apply a permission to a type or method that is
 * parametric over permissions. The application in a sense becomes a
 * part of the type of a reference, (field, local, or superclass) for
 * use in the analysis. Here's an example of an application. Given
 * the following polymorphic class:<br>
 * <br>
 * <code>
 * @Similar("p")<br>
 * class Foo {...}<br>
 * </code>
 * <br>
 * We can apply a permission in the following manner, here a local variable:<br>
 * <code>
 * void bar() {<br>
 * @Apply("share(alive)")<br>
 * Bar local bar = new Bar();<br>
 * ...<br>
 * }<br>
 * </code>
 * 
 * This annotation is the analog of the angle brackets for Java's Generics. Note
 * that because of limitations in the Java annotation syntax, we cannot actually
 * apply permissions to the constructor, only to the reference declaration. This
 * means that any variable that needs to be instantiated must be first assigned to
 * a local variable.
 * 
 * @author Nels E. Beckman
 * @since Nov 12, 2009
 * @see {@link ApplyToSuper}
 * @see {@link Exact}
 * @see {@link Similar}
 * @see {@link Symmetric}
 */
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.LOCAL_VARIABLE,
	ElementType.METHOD, ElementType.PARAMETER})
public @interface Apply {

	/** The arguments that are being applied, currently represented as strings. */
	public String[] value();
	
}
