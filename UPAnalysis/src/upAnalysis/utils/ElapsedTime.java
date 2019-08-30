package upAnalysis.utils;

import java.util.ArrayList;

// Generates a time everytime click is called. Dump causes it to print out all times since the last dump, if any.

public class ElapsedTime 
{
	private ArrayList<Long> times = new ArrayList<Long>();
	private long lastTime = System.nanoTime();
	
	
	public void click()
	{
		long time = System.currentTimeMillis();
		times.add(time - lastTime);
		lastTime = time;
	}
	
	public String dump()
	{
		StringBuilder output = new StringBuilder();
		
		int i = 0;
		for (long time : times)
		{
			output.append(i + ":" + time + " ");
			i++;
		}
		
		clear();
		return output.toString();
	}
	
	public void clear()
	{		
		lastTime = System.currentTimeMillis();
		times.clear();		
	}
}
