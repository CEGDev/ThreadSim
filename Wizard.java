import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;


public class Wizard extends Thread
{
	/**
	 * The wizard needs access to the competitor array
	 * in order to compute the scores.
	 */
	private Competitor competitors[];
	
	/**
	 * Forest containing the words that competitors need to match
	 * with the magic word.
	 */
	private Forest forest;
	
	/**
	 * Reference to the competition the wizard is hosting.
	 */
	private Competition competition;
	
	/**
	 * The competitor that won the forest obstacle.
	 */
	private Competitor forestWinner;
	
	/**
	 * The competitor that won the mountain obstacle.
	 */
	private Competitor mountainWinner;
	
	/**
	 * The competitor that won the river obstacle.
	 */
	private Competitor riverWinner;

	/**
	 * The total number of competitors 
	 * that are in the competition.
	 */
	private int numberOfCompetitors;

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
	 * Allows the Wizard to wait until the mountain 
	 * path is clear before releasing the mountainMutex
	 * semaphore for the next cmopetitor(s).
	 */
	public static Semaphore mountainSignal;

	/**
	 * Allows the Wizard to release the competitors in groups 
	 * of groupSize at the river, by waiting until a riverSignal
	 * permit is available and releasing groupSize competitors
	 * from the riverMutex semaphore.
	 */
	public static Semaphore riverSignal;

	static
	{
		mountainSignal = new Semaphore(0);
		
		riverSignal = new Semaphore(0);
	}//static initialization block
	
	public Wizard(Competitor comps[], Competition comp, Semaphore gatherBeforeCompetition, Semaphore startCompetition, Semaphore finishedCompetition, Semaphore goHome)
	{
		super("Wizard");
		
		competition = comp;
		
		competitors = comps;
		
		numberOfCompetitors = comps.length;
		
		this.gatherBeforeCompetition = gatherBeforeCompetition;
		
		this.startCompetition = startCompetition;
		
		this.finishedCompetition = finishedCompetition;
		
		this.goHome = goHome;
		
	}//Wizard Constructor
	
	public void run()
	{
		msg("is waiting for all the competitors to arrive.");
		
		startCompetition.acquireUninterruptibly();
		
		msg("is releasing the competitors.");
		
		gatherBeforeCompetition.release(numberOfCompetitors);
		
		msg("is creating the forest.");
		
		forest = new Forest();
		
		competition.setForest(forest);
		
		msg("has created the forest.");
		
		handleMountain();
		
		handleRiver();
		
		msg("is waiting for all the competitors to finish before calculating the scores.");
		
		finishedCompetition.acquireUninterruptibly();
		
		printScores();
		
		goHome.release(numberOfCompetitors);
		
	}//run
	
	

	private void handleMountain()
	{
		msg("is waiting at the mountain.");
		
		Competitor.mountainMutex.release();
		
		for(int i = 0; i < competitors.length - 1; i++)
		{
			mountainSignal.acquireUninterruptibly();
			
			msg("is signaling the next competitor for the mountain obstacle.");
			
			Competitor.mountainMutex.release();
		}//for
		
		
	}//handleMountain

	
	private void handleRiver()
	{
		
		
		int numCompetitors = competitors.length;
		
		double count = ((double)numCompetitors / Competition.getGroupSize());
		
		
		for(int i = 0; i < count; i++)
		{
			riverSignal.acquireUninterruptibly();
			
			msg("is releasing group: " + (i + 1) + ".");
			
			Competitor.riverGroup.release(Competition.getGroupSize());
			
		}//while
		
		
	}//handlRiver
	
	/**
	 * Prints a status update to the screen.
	 * @param c
	 */
	public void msg(String c) 
	{
		System.out.println(getName() + " ["+(competition.age())+"] "+": " + c + "\n");
	}//msg
	
	/**
	 * Randomly generates a 5 letter magic word, for each competitor, from the set { a, b, c, d }.
	 * @return
	 */
	public String getMagicWord()
	{
		String word = "";
		
		Random random = new Random();
		
		for(int j = 0; j < 5; j++)
		{
			word += (char)(random.nextInt(4) + 'a');
		}//for
		
		return word;
		
	}//getMagicWord
	

	/**
	 * Print the scores, and find the winners, of each obstacle.
	 */
	private void printScores()
	{
		awardForestPoints();
		
		printForestResults();
		
		printMountainResults();
		
		printRiverResults();
		
		printObstacleTime();
		
		msg("************** Winners **************");
		
		printWinners();
		
		findPrince();
		
	}//printScores
	
	
	/**
	 * Assigns points to the competitors based on FCFS discovery of the magic word.
	 * The forestWinner is set to the first competitor to find the magic word, which
	 * is the first competitor to be taken from the forestWinners LinkedList.
	 * If the list is empty, the forestWinner is decided by elapsedForestTime.
	 */
	private void awardForestPoints()
	{
	
		int score = Competitor.forestWinners.size();
	
		boolean foundWinner = false;
		
		if( ! (Competitor.forestWinners.isEmpty() ))
		{
			
			
			int listSize = Competitor.forestWinners.size();
			
			for(int i = 0; i < listSize; i++)
			{
			
				if(foundWinner)
				{
					Competitor.forestWinners.remove().setForestScore(score);
				}//if
				else
				{
					forestWinner = Competitor.forestWinners.remove();
					forestWinner.setForestScore(score);
					foundWinner = true;
				}//else
							
				score--;
				
			}//for
			
		}//if
		else
		{
			//If the forestWinners Linked List is empty, then nobody found the magic word.
			//forestWinner becomes the competitor who completed the forest the fastest.
			
			forestWinner = competitors[0];
			
			for(int i = 1; i < competitors.length; i++)	
			{
				if(competitors[i].getElapsedForestTime() < forestWinner.getElapsedForestTime())
				{
					forestWinner = competitors[i];
				}//if
			}//for
		}//else
		
	}//awardForestPoints
	
	/**
	 * Prints the results for the forest obstacle.
	 */
	private void printForestResults()
	{
		
		msg("************** Forest Results **************");
		
		for(int i = 0; i < competitors.length; i++)
		{
			
			int index = i;
			
			for(int j = i+1; j < competitors.length; j++)
			{
				if(competitors[j].getForestScore() > competitors[index].getForestScore())
				{
					index = j;
				}//if
			}//for
			
			if(index != i)
			{
				//swap
				Competitor temp = competitors[i];
				competitors[i] = competitors[index];
				competitors[index] = temp;
			}//if
			
		}//for
		
		for(Competitor comp : competitors)
		{
			msg(comp.getName() + " completed the forest in " + comp.getElapsedForestTime() 
					+ "ms and was awarded " + comp.getForestScore() + " point(s) for the forest." );
		}//for

	}//printForestResults
	
	

	/**
	 * Prints the results for the mountain obstacle.
	 */
	private void printMountainResults()
	{
		msg("************** Mountain Results **************");
		
		//Sort the competitors by elapsedMountainTime.
		for(int i = 0; i < competitors.length; i++)
		{
			
			int index = i;
			
			for(int j = i+1; j < competitors.length; j++)
			{
				if(competitors[j].getElapsedMountainTime() < competitors[index].getElapsedMountainTime())
				{
					index = j;
				}//if
			}//for
			
			if(index != i)
			{
				//swap
				Competitor temp = competitors[i];
				competitors[i] = competitors[index];
				competitors[index] = temp;
			}//if
			
		}//for
		
		if(competitors.length >= 3)
		{
			competitors[0].setMountainScore(8);
			competitors[1].setMountainScore(6);
			competitors[2].setMountainScore(3);
		}//if
		
		for(Competitor comp : competitors)
		{
			msg(comp.getName() + " completed the mountain in " + comp.getElapsedMountainTime()
					+ "ms and was awarded " + comp.getMountainScore() + " point(s) for the mountain." );
		}//for
		
		mountainWinner = competitors[0];
		
		
	}//printMountainResults
	
	/**
	 * Prints the results for the river obstacle.
	 */
	private void printRiverResults()
	{
		msg("************** River Results **************");
		
		//Sort the competitors by elapsedRiverTime.
		for(int i = 0; i < competitors.length; i++)
		{
			
			int index = i;
			
			for(int j = i+1; j < competitors.length; j++)
			{
				if(competitors[j].getElapsedRiverTime() < competitors[index].getElapsedRiverTime())
				{
					index = j;
				}//if
			}//for
			
			if(index != i)
			{
				//swap
				Competitor temp = competitors[i];
				competitors[i] = competitors[index];
				competitors[index] = temp;
			}//if
			
		}//for
		
		if(competitors.length >= 3)
		{
			competitors[0].setRiverScore(8);
			competitors[1].setRiverScore(6);
			competitors[2].setRiverScore(3);
		}//if
		
		for(Competitor comp : competitors)
		{
			msg(comp.getName() + " completed the river in " + comp.getElapsedRiverTime() 
					+ "ms and was awarded " + comp.getRiverScore() + " point(s) for the river." );
		}//for
		
		riverWinner = competitors[0];
		
	}//printRiverResults
	
	/**
	 * Prints the total time spent of each obstacle and rest stop, 
	 * for each competitor in sorted order.
	 */
	private void printObstacleTime()
	{
		
		msg("************** Total Obstacle And Rest Times **************");
		
		
		//Sort the competitors by total obstacle time.
		for(int i = 0; i < competitors.length; i++)
		{
			
			int index = i;
			
			for(int j = i+1; j < competitors.length; j++)
			{
				if(competitors[j].getTotalObstacleTime() < competitors[index].getTotalObstacleTime())
				{
					index = j;
				}//if
			}//for
			
			if(index != i)
			{
				//swap
				Competitor temp = competitors[i];
				competitors[i] = competitors[index];
				competitors[index] = temp;
			}//if
			
		}//for
		
		for(Competitor comp : competitors)
		{
			msg(comp.getName() + " total turn-around time: " + comp.getTotalObstacleTime() + "ms.");
		}//for
	}//printObstacleTime
	
	/**
	 * Finds the prince and handles a tie.
	 */
	private void findPrince()
	{
		ArrayList<Competitor> winnerList = new ArrayList<Competitor>();
		
		winnerList.add(competitors[0]);
		
		//Check for a tie.
		for(int i = 0; i < competitors.length - 1; i++)
		{
			if(competitors[i].getTotalObstacleTime() == competitors[i+1].getTotalObstacleTime())
			{
				winnerList.add(competitors[i+1]);
			}//if
			else
			{
				break;
			}//else
		}//for
		
		if(winnerList.size() > 1)
		{
			Random rand = new Random();
			int winner = rand.nextInt(winnerList.size());
			
			msg("There was a tie!");
			msg(winnerList.toString() + " have all finished with the same turnaround time.");
			msg(winnerList.get(winner).getName() + " has become the prince!!");
			msg("The princess chooses to marry: " + winnerList.get(winner) + "!");
		}//if
		else
		{
			msg(winnerList.get(0).getName() + " has become the prince!!");
			msg("The princess chooses to marry: " + winnerList.get(0) + "!");
		}//else
	}//findPrince
	
	/**
	 * Displays the winners of each obstacle.
	 */
	private void printWinners()
	{
		msg(forestWinner + " won the forest obstacle!");
		msg(mountainWinner + " won the mountain obstacle!");
		msg(riverWinner + " won the river obstacle!");
	}//printWinners
	
}//Wizard
