/**
 * Copyright (c) 2006, 2007, 2008 Marwan Abi-Antoun, Jonathan Aldrich, Nels E. Beckman,    
 * Kevin Bierhoff, David Dickey, Ciera Jaspan, Thomas LaToza, Gabriel Zenarosa, and others.
 *
 * This file is part of Crystal.
 *
 * Crystal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Crystal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Crystal.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.cmu.cs.crystal.cfg.method;

import org.eclipse.jdt.core.IMember;

import edu.cmu.cs.crystal.cfg.ICFGEdge;
import edu.cmu.cs.crystal.cfg.ICFGNode;
import edu.cmu.cs.crystal.flow.ILabel;
import edu.cmu.cs.crystal.flow.NormalLabel;

/**
 * @author thomaslatoza
 * @since 3.3.3
 */
public class MemberCFGEdge implements ICFGEdge<IMember>
{
	private MemberCFGNode source;
	private MemberCFGNode dest;
	
	protected MemberCFGEdge(MemberCFGNode source, MemberCFGNode dest)
	{
		this.source = source;
		this.dest = dest;
	}

	public ILabel getLabel() 
	{
		return NormalLabel.getNormalLabel();
	}

	public MemberCFGNode getSink() 
	{
		return dest;
	}

	public MemberCFGNode getSource() 
	{
		return source;
	}
}
