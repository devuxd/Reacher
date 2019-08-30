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
 * Declares a polyomrphic permission, where each use of the permission
 * must have the same kind, but can have different fractions.<br>
 * <br>
 * E.g., to declare a polymorphic permission scoped for a class named 'perm'
 * you would write<br>
 * <code>
 * @Similar("perm")<br>
 * class Foo { ... }<br>
 * </code>
 * <br>
 * Polypmorphic permissions can also be created on a
 * per-method basis.
 * 
 * @author Nels E. Beckman
 * @since Nov 10, 2009
 * @see {@link Exact}
 * @see {@link Symmetric}
 * @see {@link ResultPolyVar} - To use a polyvar in a method spec.
 * @see {@link PolyVar} - To use a polyvar in a method spec.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Similar {
	/** The name of the polymorphic permission variable. Required. */
	public String value();
}
