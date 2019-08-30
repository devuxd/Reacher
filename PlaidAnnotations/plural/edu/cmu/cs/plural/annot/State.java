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

import java.lang.annotation.Target;

/**
 * Another prototype interface, this one allows us to declare individual
 * states and the state invariants that they have.
 * 
 * @author Nels Beckman
 * @since Feb 22, 2008
 * @see ClassStates
 */
@Target({})
public @interface State {

	/**
	 * What is the name of this state?
	 */
	String name() default "alive";
	
	/**
	 * State invariant, written in a string and parsed!
	 * The grammar for state invariants is as follows (and sure to change in the
	 * near future):
	 * 
	 * <pre>
	 * START ::= EOF | PERM EOF
	 * PERM  ::= POREXPR OP POREXPR
	 * POREXPR ::= ACCESS_PRED | JAVA_EXPR | STATE_ONLY
	 * ACCESS_PRED ::= ptype(ref)
	 *               | ptype(ref) in stateInfo
	 *               | ptype(ref,root)
	 *               | ptype(ref,root) in stateInfo
	 *               | ptype(ref,root,ffun,below_ffun) in stateInfo
	 *               
	 * OP    ::= alt | +
	 *         | with | &
	 *         | tens | *
	 *         
	 * JAVA_EXPR ::= PRI_EXPR JOP PRI_EXPR
	 * PRI_EXPR  ::= null | ref
	 * JOP       ::= == | !=
	 * 
	 * STATE_ONLY ::= ref in stateInfo
	 * </pre>
	 * Note that currently fraction functions (ffun and below_ffun) are just
	 * strings and are not really parsed at all. Actually, I don't even think
	 * they can be anything more than letters and numbers.
	 */
	String inv() default "";
}
