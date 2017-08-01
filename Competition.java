import java.io.File;
import java.util.concurrent.Semaphore;


public class Competition
{
	/**
	 * This semaphore is used to allow the all of
	 * the competitors to arrive
	 * before the competition starts.
	 */
	private Semaphore startCompetition;
	
	/**
	 * This semaphore is used in conjunction 
	 * with the gatherBeforeCompetition semaphore.
	 * This releases the Wizard from being blocked 
	 * until the competitors arrive.
	 * The wizard will then release all permits
	 * on gatherBeforeCompetition.
	 */
	private Semaphore gatherBeforeCompetition;
	
	/**
	 * This semaphore is used to release the Wizard,
	 * so that he knows all the competitors have finished 
	 * the competition and he can compute the scores.
	 */
	private Semaphore finishedCompetition;
	
	/**
	 * This semaphore dismisses all the competitors
	 * once the wizard has computed all the scores.
	 */
	private Semaphore goHome;
	
	/**
	 * Starting time of the competition.
	 */
	private static long startTime = System.currentTimeMillis();
	
	/**
	 * This array provides access to the Wizard,
	 * of all the competitors in the competition, 
	 * in order to compute scores, and form groups.
	 */
	private Competitor competitors[];
	
	/**
	 * The Wizard for this competition.
	 */
	private Wizard wizard;
	
	/**
	 * The forest obstacle for this competition.
	 */
	private Forest forest;
	
	/**
	 * The total number of competitors in the competition.
	 */
	private int numberOfCompetitors;
	
	/**
	 * The groupSize for the river obstacle.
	 */
	private static int groupSize;
	
	public Competition(int size, int groupSize)
	{
		competitors = new Competitor[size];
		
		numberOfCompetitors = size;
		
		startCompetition = new Semaphore((size * -1) + 1, true);
		
		gatherBeforeCompetition = new Semaphore(0, true);
		
		finishedCompetition = new Semaphore((size * -1) + 1, true);
		
		goHome = new Semaphore(0, true);
		
		Competition.groupSize = groupSize;
		
		init();
	}//Competition Constructor
	
	/**
	 * Create the threads and start the competition.
	 */
	public void init()
	{
		
		wizard = new Wizard(competitors, this, gatherBeforeCompetition, startCompetition, finishedCompetition, goHome);
		
		//Create all the competitor threads for N = 8
		for(int i = 0; i < competitors.length; i++)
		{
				competitors[i] = new Competitor(i, this, gatherBeforeCompetition, startCompetition, finishedCompetition, goHome);
		}//for
		
		wizard.start();
		
		//Start the threads.
		for(int i = 0; i < competitors.length; i++)
		{
			competitors[i].start();
		}//for

	}//init
	
	/**
	 * Returns the time elapsed.
	 * @return
	 */
	public long age()
	{
		return System.currentTimeMillis() - startTime;
	}//age
	
	/**
	 * Returns the file with magic words.
	 * @return
	 */
	public File getForestFile()
	{
		while(forest == null){} // Busy wait in case the forest has not yet been created.
		
		return forest.getForestFile();
	}//getForestFile
	

	/**
	 * Sets the forest for the competition 
	 * after the wizard creates it.
	 * 
	 * @param f
	 */
	public void setForest(Forest f)
	{
		forest = f;
	}//setForest

	/**
	 * Gets a randomly generated magic word.
	 * @return
	 */
	public String getMagicWord()
	{
		return wizard.getMagicWord();
	}//getMagicWord
	
	/**
	 * Returns the number of competitors in the competition.
	 * @return
	 */
	public int getNumCompetitors()
	{
		return numberOfCompetitors;
	}//getNumCompetitors
	
	/**
	 * Returns the groupSize for this competition.
	 * @return
	 */
	public static int getGroupSize()
	{
		return groupSize;
	}//getGroupSize
	
	//main
	public static void main(String args[])
	{
		new Competition(8, 3);
		
	}//main
	
	
}//Competition class


