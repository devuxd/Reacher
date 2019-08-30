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
 * Annotate objects that lend the result of a method with this annotation.
 * Lending the result means to temporarily invalidate the lender, as long
 * as the result is used.  
 * Placing this annotation on a method means that the receiver lends the result.
 * Therefore, this annotation is meaningless on static methods, but it is
 * sensible to place it on parameters of static methods.
 * This annotation is also not meaningful for constructors or parameters
 * of constructors, and for methods and parameters 
 * of methods with <code>void</code> return type.
 * 
 * This annotation is for example useful for <b>getters</b>: The object is
 * invalidated while the result of a getter is in use. 
 * Notice that Plural also assumes the specification of the annotated method
 * to consume the permission for the lending object.
 * Plural currently can only soundly lend objects
 * from {@link Unique unique} and {@link Imm immutable} permissions.
 * 
 * @author Kevin Bierhoff
 * @since 9/25/2008
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface Lend {

	/** 
	 * The parameter being lent.
	 * This attribute should be provided if a parameter is lent
	 * but can be omitted.
	 * @see Param 
	 */
	String param() default "";
	
}
