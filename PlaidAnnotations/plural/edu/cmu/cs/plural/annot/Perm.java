/**
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
package edu.cmu.cs.plural.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This is a general permission annotation for a method. If you don't want to
 * use Share, Pure and the like, you can use this permission instead and
 * specify requires/ensures in our text language.
 * 
 * The major difference is that you must use #0, #1, etc. when referring to
 * method parameters, b/c these names will be eliminated during compilation.
 * 'result' and 'this' still work as usual.
 * 
 * Use {@link Cases} to annotate a method with multiple <code>Perm</code>
 * annotations.
 * 
 * @see edu.cmu.cs.plural.annot.State For language grammar.
 * 
 * @author Nels Beckman
 * @since Apr 2, 2008
 *
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Perm {

	/**
	 * Permission that is required to call this method specified in our textual
	 * permission language. Default is empty.
	 */
	String requires() default "";

	/**
	 * Permission that is returned after calling this method specified in our textual
	 * permission language. Default is empty.
	 */
	String ensures() default "";
}
