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
 * Allows polymorphic permission variables to be used in specification. After
 * declaring a polymorphic permission variable in a class or in a method, you
 * will actually want to use them somewhere in your specification. This annotation
 * allows you to do just that, for a method parameter, or for the method receiver.
 * (Just how this will work with receivers is still up in the air.)<br> 
 * <br>
 * For example, if a permission 'perm' is declared for a class, here's how you use
 * it at the constructor site:<br>
 * <code>
 * @Exact("perm")<br>
 * class OnePlaceBuffer {<br>
 *   public OnePlaceBuffer(@PolyVar(value="perm", returned=false) Object o) {...}<br>
 * }<br>
 * 
 * 
 * @author Nels E. Beckman
 * @since Nov 10, 2009
 * @see {@link Exact}
 * @see {@link Similar}
 * @see {@link Symmetric}
 * @see {@link ResultPolyVar}
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface PolyVar {
	/** The name of the polymorphic permission variable that is being referenced. */
	public String value();
	
	/** Is the permission returned? In other words, is this a borrow or a capture? */
	public boolean returned() default true;
}
