package upAnalysis.utils;

import java.util.HashMap;
import java.util.HashSet;

public class OneToManyIndex<K, V> 
{
	private final HashSet<V> emptySet = new HashSet<V>(); 
	private HashMap<K, HashSet<V>> index = new HashMap<K, HashSet<V>>();
	
	public void put(K key, V value)
	{
		HashSet<V> values = index.get(key);
		if (values == null)
		{
			values = new HashSet<V>();
			index.put(key, values);
		}
		values.add(value);	
	}
	
	public void remove(K key, V value)
	{
		HashSet<V> values = index.get(key);
		if (values != null)
			values.remove(value);
	}
	
	// Retrieves the values for a key. If there is no entry for the key, returns an empty set of V. 
	public HashSet<V> get(K key)
	{
		HashSet<V> values = index.get(key);
		if (values != null)
			return values;
		else 
			return emptySet;
	}
	
	public boolean containsKey(K key)
	{
		return index.containsKey(key);
	}
	
	public int hashCode()
	{
		return index.hashCode();
	}
	
	public boolean equals(Object o)
	{
		if (o instanceof OneToManyIndex)
			return this.index.equals(((OneToManyIndex) o).index);
		else
			return false;
	}	
	
	public String toString()
	{
		return index.toString();
	}
}
