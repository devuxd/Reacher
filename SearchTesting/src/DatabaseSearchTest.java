import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class DatabaseSearchTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		try 
		{
			Connection c = DriverManager.getConnection("jdbc:hsqldb:mem:mymemdb", "SA", "");
			Statement db_statement = c.createStatement();
			db_statement.executeUpdate ("create table employee ( id INT, type INT, text CHAR(150) );");


			
			
			PreparedStatement insertStmt = c.prepareStatement("insert into employee values ( ?, ? , ?)"); 
			
			int j = 0;
			for (int i = 0; i < 1000000; i++)		
			{
				insertStmt.setInt(1, i);
				insertStmt.setInt(2, j);
				insertStmt.setString(3, "blasdf" + new Integer(i).toString() + "a111a1112222");
				insertStmt.executeUpdate();				
				j = (j + 1) % 10;
			}
			
			
			
			
			// Commit changes
			c.commit();
			db_statement.executeUpdate ("CREATE INDEX employeeIndex ON employee (text);");
			
			System.out.println("Done adding data");
			
			
			ResultSet results = db_statement.executeQuery("SELECT * FROM employee WHERE type = 5 AND text LIKE '%11111%'");
			
			results.next();
			while (!results.isAfterLast())
			{
				System.out.println(results.getInt(1));				
				results.next();
			}
			
			results = db_statement.executeQuery("SELECT * FROM employee WHERE text LIKE '%22222%'");
			
			results.next();
			while (!results.isAfterLast())
			{
				System.out.println(results.getInt(1));				
				results.next();
			}
			
			System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
					/ 1024 / 1024 + "MB");
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		int j = 0;
		for (int i = 0; i < 1000000; i++)		
		{
			String str = "blasdf" + new Integer(i).toString() + "a111a1112222";
			
			if (str.contains("11111"))
				System.out.println(str);
			j = (j + 1) % 10;
		}
		
		System.out.println("Done");
		
		
		
		// TODO Auto-generated method stub

	}

}
