/*
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

/**
 * This interface collects string constants for Plural's analysis names.
 * @author Kevin Bierhoff
 * @since Oct 28, 2008
 *
 */
public interface PluralAnalysis {
	
	/** Syntax checker */
	public static final String SYNTAX = "PluralAnnotationAnalysis";
	
	/** Effect checker */
	public static final String EFFECT = "EffectChecker";
	
	/** Plural checker: Permission tracker that disregards concurrency */
	public static final String PLURAL = "FractionalAnalysis";
	
	/** NIMBY checker: Permission tracker for AtomicPower! (Java + <b>atomic</b> blocks) */
	public static final String NIMBY = "NIMBYChecker";
	
	/** Sync-or-Swim prerequisite checker */
	public static final String SOS_PRE = "SyncChecker";
	
	/** Sync-or-Swim checker: Permission tracker for concurrent Java programs */
	public static final String SOS = "SyncOrSwim";

}
