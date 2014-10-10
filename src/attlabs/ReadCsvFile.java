package attlabs;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ReadCsvFile {

	private String fileName = null;
	private ArrayList<User> users = new ArrayList<User>();
	private int totalOfUsers = 0;
	

	public ReadCsvFile(String csvFile) {
		this.fileName = csvFile;
	}

	
	

	public void ReadFileEntries() throws Exception {
		int fileLine = 0;
		 try
		    {
		      FileReader fr = new FileReader(fileName);
		      @SuppressWarnings("resource")
		      BufferedReader br = new BufferedReader(fr);
		      String stringRead = br.readLine();

		      while( stringRead != null )
		      {
		    	  String[] elements = stringRead.split(",");
		    	  fileLine++;

		    	  	//If file format is incorrect kill all and tell user which line is wrong
		    	  
		    	    if(elements.length < 2 || elements.length >2) {
		    	      System.out.println("ERROR: " + "FileFormat Incorrect. Line: " + fileLine);
		    	      System.out.println("Please verify csv file contents and edit as necessary.\n");
		    	      throw new RuntimeException("FileFormat Incorrect. Line: " + fileLine); //handle missing entries
				    	
		    	    }
		    	        	    
		    	    String oldUser = elements[0];
		    	    String newUser = elements[1];
		    	    stringRead = br.readLine();
		    	    User usuario = new User();
		    	    usuario.setOldUserId(oldUser);
		    	    usuario.setNewUserId(newUser);
		    	    if(usuario.isValid()) {
		    	    	 users.add(usuario);
				    	 setTotalOfUsers(getTotalOfUsers() + 1);
		    	    }
		    	    else {
		    	    	System.out.println("Invalid user found. Line" + fileLine );
		    	    }
		    	   
		    	    
		      }
		      br.close( );
		    }
		 	catch(FileNotFoundException ioe) {
		 		System.out.println("ERROR: " + fileName + " File not found!");
		 		System.exit(1);
		 	}
		    catch(IOException ioe){
		    	throw ioe;
		    } 
	}
 
	public ArrayList<User> getUsers() {
		return users;
	}
	
	public void printAll() {
		for (int i=0;i< users.size();i++) {
			User entry = users.get(i);
			System.out.println(" Old userId: " + entry.getOldUserId() + " New userId: " + entry.getNewUserId());
			
		}
	   
	}

	public int getTotalOfUsers() {
		return totalOfUsers;
	}

	public void setTotalOfUsers(int totalOfUsers) {
		this.totalOfUsers = totalOfUsers;
	}
	
  
}
