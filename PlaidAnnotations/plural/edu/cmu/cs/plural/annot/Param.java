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
 * Placed on a class or interface, this annotation defines a parameter, 
 * similar to a specification field in behavioral specification approaches.
 * Optionally, permissions associated with the defined parameter can
 * be automatically released by Plural when the owner becomes available
 * for garbage collection.
 * Placed on a field, this annotation binds a parameter to a concrete field.
 * 
 * @author Kevin Bierhoff
 * @since 6/03/2008
 * @see Capture Instantiating a parameter
 * @see Release Explicitly releasing a parameter
 * @see Lend Temporarily lending a parameter
 */
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Param {

	/** The name of the parameter. */
	String name();
	
	/** Optionally, the type of the parameter. */
	Class<?> type() default Object.class;
	
	/**
	 * The state that this parameter should be automatically released from
	 * when the owner becomes available for garbage collection, if any.
	 * The given state should be a state defined for the annotated type.
	 * If this attribute is not specified then the parameter is never
	 * automatically released.
	 */
	String releasedFrom() default "";
}
