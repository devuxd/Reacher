package upAnalysis.summary.summaryBuilder.rs;

import java.io.Serializable;
import java.util.ArrayList;

/* ResolvedSources differ from Sources in not having any dependency on the Eclipse AST and instead keeping
 * tracking of variables as lattice elements.  Thus, unlike Sources, ResolvedSources can be kept around in summaries
 * without also keeping the Eclipse AST of the method around.
 * 
 * The identity of resolved sources, establish by eqauls and hashCode, depends only on the invocation site of the source,
 * not details about what args happened to be passed or the receivers. This identity is used during trace execution
 * to look up values of ResolvedSources that have executed. We bind to alternative resolved sources that executed on any path.
 */

public interface ResolvedSource extends Serializable
{
	// Gets a list of objects defining the state of the object. Should only be used to compare the states of 2 objects
	//public ArrayList<Object> getState();

	
}
