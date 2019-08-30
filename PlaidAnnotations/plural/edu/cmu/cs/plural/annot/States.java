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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to declare a set of mutually exclusive states.
 * States are always a refinement of an existing state, possibly the pre-defined
 * root-state <i>alive</i>.  A set of states is orthogonal to other sets of states
 * refining the same state.  Each orthogonal set forms a <i>state dimension</i>
 * that can optionally be named using the attribute {@link #dim()}.   
 * 
 * @author Kevin Bierhoff
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface States {

	/** 
	 * The name of the state dimension being introduced.
	 * If not provided this declares an <i>anonymous</i> dimension
	 * for which Plural will make up a random, unique name. 
	 */
	String dim() default "";
	
	/** 
	 * List of state names being introduced as mutually exclusive
	 * (empty by default).
	 * By leaving this list empty you create a state dimension with
	 * no states in it, which is helpful to keep "data" without a protocol.
	 */
	String[] value() default { };
	
	/** Existing state being refined (defaults to <i>alive</i>). */
	String refined() default "alive";
	
	/** 
	 * If this attribute is <code>true</code> then objects won't 
	 * change state in this dimension (<code>false</code> by default). 
	 */
	boolean marker() default false;
}
