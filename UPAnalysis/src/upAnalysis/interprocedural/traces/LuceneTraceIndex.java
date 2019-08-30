package upAnalysis.interprocedural.traces;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import upAnalysis.interprocedural.traces.impl.AbstractTrace;
import upAnalysis.interprocedural.traces.impl.MethodTrace;

// Searches along a method trace for matches to a string


public class LuceneTraceIndex 
{
	private static String FULL_TEXT = "fullText";
	private static String ID = "ID";
	
	
	private Analyzer analyzer = new StandardAnalyzer();
	private IndexSearcher indexSearcher;
	private int MAX_HITS = 1000;		// The most hits that will ever be returned. TODO: consider if this is a problem
	
	
	public LuceneTraceIndex(MethodTrace traceRoot)
	{
		// TODO: look at which analyzer really makes the most sense for identifiers
		IndexWriter writer = null;		
		Directory directory = new RAMDirectory();
		try {
			 writer = new IndexWriter(directory, analyzer, true);
			 
			for (AbstractTrace trace : traceRoot)
			{
				Document doc = new Document();
				doc.add(new Field(FULL_TEXT, trace.toString(), Field.Store.NO, Field.Index.TOKENIZED));
				doc.add(new Field(ID, trace.getID(), Field.Store.YES, Field.Index.NO));
				writer.addDocument(doc);
			}
		
			writer.optimize();
			writer.close();
			indexSearcher = new IndexSearcher(directory);
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public List<AbstractTrace> find(String queryString)
	{
		ArrayList<AbstractTrace> results = new ArrayList<AbstractTrace>();
		
		try 
		{
			Query query = QueryParser.parse(queryString, FULL_TEXT, analyzer);
			Hits hits = indexSearcher.search(query);
			for (int i = 0; i < hits.length(); i++)
			{
				Document hitDoc = hits.doc(i);				
				String id = hitDoc.getField(ID).stringValue();
				results.add(AbstractTrace.getByID(id));
			}
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return results;
	}
	
}
