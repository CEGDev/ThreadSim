import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class Competitor extends Thread
{
	
	/**
	 * Reference to the competition that 
	 * this competitor is in.
	 */
	private Competition competition;

	/**
	 * Random number generator.
	 */
	private Random random;
		
	/**
	 * This competitor's points that were award for
	 * completing the forest obstacle.
	 */
	private int forestScore;
	
	/**
	 * This competitor's points that were award for
	 * completing the mountain obstacle.
	 */
	private int mountainScore;
	
	/**
	 * This competitor's points that were award for
	 * completing the river obstacle.
	 */
	private int riverScore;

	/**
	 * Represents the time spent in the 
	 * forest for this competitor.
	 */
	private long elapsedForestTime;
	
	
	/**
	 * Represents the mountain crossing
	 * time for this competitor.
	 */
	private long elapsedMountainTime;
	
	
	/**
	 * Represents the river crossing time 
	 * for this competitor.
	 */
	private long elapsedRiverTime;
	
	/**
	 * Represents the total time each competitor spent resting.
	 */
	private long totalRestTime;
	
	/**
	 * totalElapsedObstacleTime = 
	 * (elapsedForestTime 	+ 
	 * 	elapsedMountainTime + 
	 * 	elapsedRiverTime 	+ 
	 * 	the random value from both rests.
	 * 
	 */
	private long totalElapsedObstacleTime;
	
	/**
	 * The total number of competitors that
	 * have crossed the river so far.
	 */
	private static int riverCount;
	
	/**
	 * Group size for the river part of the competition.
	 */
	private static int groupSize;

	/**
	 * This semaphore is used to allow the all of
	 * the competitors to arrive
	 * before the competition starts.
	 */
	private Semaphore gatherBeforeCompetition;

	/**
	 * This semaphore is used in conjunction 
	 * with the gatherBeforeCompetition semaphore.
	 * This releases the Wizard from being blocked 
	 * until the competitors arrive.
	 * The wizard will then release all permits
	 * on gatherBeforeCompetition.
	 */
	private Semaphore startCompetition;
	
	/**
	 * This semaphore is used to 
	 * add the competitors to the forestWinners
	 * list, so that FCFS is not compromised
	 * and the Competitors are inserted into the 
	 * list correctly.
	 */
	private static Semaphore magicWordMutex;
	
	/**
	 * This semaphore is used to prevent more than
	 * one competitor accessing the mountain at a time.
	 */
	public static Semaphore mountainMutex;
	
	/**
	 * This semaphore causes the competitors
	 * to form in groups of groupSize at the river.
	 */
	public static Semaphore riverGroup;
	
	/**
	 * This semaphore is used for two reasons.
	 * Correct access to the shared variable riverCount,
	 * as well as the correct evaluation of the if 
	 * statement allowing the groups to form correctly.
	 */
	public static Semaphore riverMutex;
	
	/**
	 * This semaphore is used to release the Wizard,
	 * so that he knows all the competitors have finished 
	 * the competition and he can compute the scores.
	 */
	public Semaphore finishedCompetition;

	/**
	 * This semaphore dismisses all the competitors
	 * once the wizard has computed all the scores.
	 */
	private Semaphore goHome;
	
	/**
	 * Keeps track of the order that competitors 
	 * found the magic word, in order to
	 * give each competitor the correct amount of
	 * points.
	 */
	public static LinkedList<Competitor> forestWinners;
	
	static
	{

		forestWinners = new LinkedList<Competitor>();
		
		magicWordMutex = new Semaphore(1, true);
		
		mountainMutex = new Semaphore(0, true);
		
		riverGroup = new Semaphore(0, true);
		
		riverMutex = new Semaphore(1, true);
		
		riverCount = 0;
		
		groupSize = 3;
		
	}//static initialization block.
	
	/**
	 * Constructs a competitor for the competition.
	 * @param id - The name of the Competitor.
	 * @param comp - Reference to the competition that this competitor is in.
	 * @param startCompetition 
	 * @param gatherBeforeCompetition 
	 * @param goHome 
	 */
	public Competitor(int id, Competition comp, Semaphore gatherBeforeCompetition, Semaphore startCompetition, Semaphore finishedCompetition, Semaphore goHome)
	{
		super("Competitor-" + id);
		
		competition = comp;
		
		random = new Random();
		
		forestScore = 0;
		
		mountainScore = 0;
		
		riverScore = 0;
		
		this.gatherBeforeCompetition = gatherBeforeCompetition;
		
		this.startCompetition = startCompetition;
		
		this.finishedCompetition = finishedCompetition;
		
		this.goHome = goHome;
		
	}//Competitor
	
	public void run()
	{
		startCompetition.release();
		
		msg("is waiting to start the competition.");
		
		gatherBeforeCompetition.acquireUninterruptibly();
		
		msg("has started the competition");
		
		msg("is resting and eating before the forest.");
		
		rest();
		
		msg("has finished resting.");
		
		enterForest();
		
		enterMountainPassage();
		
		msg("is resting and eating before the river");
		
		rest();
		
		msg("has finished resting.");
		
		crossTheRiver();
		
		totalElapsedObstacleTime = (elapsedForestTime + elapsedMountainTime + elapsedRiverTime + totalRestTime);
		
		finishedCompetition.release();
		
		goHome.acquireUninterruptibly();
		
		msg("has been dismissed.");
	}//run
	
	
	/**
	 * Simulates a rest stop for each competitor.
	 */
	private void rest()
	{
		long restTime = random.nextInt(21) + 40 ;
		
		totalRestTime += restTime;
		
		try
		{
			sleep(restTime);
		}//try
		catch ( InterruptedException ie )
		{
			msg("Failed to rest due to an interruption");
		}//catch
	}//rest
	
	
	/**
	 * Simulates the forest obstacle for this competitor.
	 */
	public void enterForest()
	{
		elapsedForestTime = competition.age();

		msg("has entered the forest.");
		
		BufferedReader reader = null;
		
		String word = "";
		
		String magicWord = competition.getMagicWord();
		
		boolean foundMagicWord = false;
		
		msg("is using the compass to find the map with the magic word " + magicWord + ".");
		
		/*
		 * Search for the magic word.
		 */
		try
		{
			reader = new BufferedReader (
						new InputStreamReader (
							new FileInputStream( competition.getForestFile() )));
			 
			while((word = reader.readLine()) != null)
			{

				if(magicWord.equalsIgnoreCase(word))
				{
					foundMagicWord = true;
					
					break;
				}//if
			}//while
		
			reader.close();
			
		}//try
		catch(FileNotFoundException fnfe)
		{
			System.out.println("Unable to access the forest");
		}//catch
		catch (IOException e)
		{
			System.out.println("Error: IO Exception.");
		}//catch
		
		
		if(!foundMagicWord)
		{
			
			msg("did not find the magic word " + magicWord + " and is leaving the forest." );
		
			
			msg("is being forced to yield as a penalty for not finding the magic word.");
		
			//Penalty for not finding the magic word.
			yield();
			yield();
		}//if
		else
		{
			try
			{
				magicWordMutex.acquire();
				
				forestWinners.add(this);
				
			}//try
			catch(InterruptedException ie)
			{
				msg("failed to acquire a permit for the magic word.");
			}//catch
			
			magicWordMutex.release();
			
			msg("** has found the magic word " + magicWord + " and is leaving the forest. ** ");
			
			
		}//else
		
		elapsedForestTime = competition.age() - elapsedForestTime;
		
		msg("completed the forest in " + elapsedForestTime + "ms.");
	}//enterForest
	
	/**
	 * Simulates the mountain passage for this competitor.
	 */
	private void enterMountainPassage()
	{
		
		mountainMutex.acquireUninterruptibly();
		
		//Arrival Time
		long mountainArrivalTime = competition.age();
		
		msg("is entering the mountain passage.");
		
		try
		{
			sleep(random.nextInt(1500));
		}//catch
		catch (InterruptedException e)
		{
			msg("failed the mountain passage and has recieved a penalty.");
			
			elapsedMountainTime = 1500; // Give penalty.
			
			return;
		}//catch
		
		//Calculate the elapsed time.
		elapsedMountainTime = competition.age() - mountainArrivalTime;
		
		msg("has completed the mountain with a travel time of " + elapsedMountainTime + "ms.");
		
		Wizard.mountainSignal.release();
		
	}//enterMountainPassage
	
	/**
	 * Simulates the river obstacle for this competitor.
	 */
	private void crossTheRiver()
	{
		msg("has arrived at the river and is joining a group.");
		
		riverMutex.acquireUninterruptibly();
		
		riverCount++;
		
		if( (riverCount % groupSize) != 0 && riverCount != competition.getNumCompetitors())
		{
			riverMutex.release();
			
			riverGroup.acquireUninterruptibly();
		}//if
		else
		{
			riverMutex.release();
			
			Wizard.riverSignal.release();
			
			riverGroup.acquireUninterruptibly();
			
		}//else
			
		long riverArrivalTime = competition.age();
		
		msg(" is crossing the river.");

		try
		{
			
			sleep(random.nextInt(1500));
			
		}//try
		catch (InterruptedException e)
		{
			msg("failed to cross the river and has received a penalty.");
			
			elapsedRiverTime = 1500;
			
			return;
		}//catch
		
		elapsedRiverTime = competition.age() - riverArrivalTime;
		
		msg("has crossed the river with a time of " + elapsedRiverTime + "ms.");
		
	}//crossTheRiver
	
	/**
	 * Prints a status update to the screen.
	 * @param c
	 */
	public void msg(String c) 
	{
		System.out.println(getName() + " ["+(competition.age())+"] "+": " + c + "\n");
	}//msg

	public void setForestScore(int score)
	{
		forestScore = score;
	}//setForestScore

	public int getForestScore()
	{
		return forestScore;
	}//getForestScore
	
	public void setMountainScore(int score)
	{
		mountainScore = score;
	}//setMountainScore

	public int getMountainScore()
	{
		return mountainScore;
	}//getMountainScore
	
	public void setRiverScore(int score)
	{
		riverScore = score;
	}//setRiverScore

	public int getRiverScore()
	{
		return riverScore;
	}//getRiverScore
	
	public long getTotalObstacleTime()
	{
		return totalElapsedObstacleTime;
	}//getTotalObstacleTime

	public long getElapsedForestTime()
	{
		return elapsedForestTime;
	}//getElapsedForestTime

	public long getElapsedMountainTime()
	{
		return elapsedMountainTime;
	}//getElapsedMountainTime
	
	public long getElapsedRiverTime()
	{
		return elapsedRiverTime;
	}//getElapsedRiverTime
	
	public static int getRiverCount()
	{
		return riverCount;
	}//getRiverCount

	public String toString()
	{
		return getName();
	}//toString
	
}//Competitor
