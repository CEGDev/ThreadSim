/**
 * 	Queens College
 *	Carl Gentile
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Random;

/**
 * This class contains the methods necessary for 
 * the wizard to create the forest for the competition.
 * 
 * @author Carl
 *
 */
public class Forest
{
	private File forestFile;
	
	public Forest()
	{
		forestFile = new File("forest.txt");
		buildForest();
	}//Forest Constructor
	
	public void buildForest()
	{
		PrintWriter pw = null;
		
		Random random = new Random();
		
		int wordCount = random.nextInt(300) + 300;
		
		String word = "";
		
		try
		{
			pw = new PrintWriter( 
							new FileOutputStream(forestFile));
		}//try
		catch (FileNotFoundException e)
		{
			System.out.println("The forest was not successfully created.");
		}//catch
		
		
		//Print the words to the file.
		for(int i = 0; i < wordCount; i++)
		{
			//Create the next word. 
			for(int j = 0; j < 5; j++)
			{
				word += (char)(random.nextInt(4) + 'a');
			}//for
			
			pw.println(word);
			word = "";
		}//for
		
		pw.close();
		
	}//buildForest
	
	
	public File getForestFile()
	{
		return forestFile;
	}//getForest
	
}//Forest class
