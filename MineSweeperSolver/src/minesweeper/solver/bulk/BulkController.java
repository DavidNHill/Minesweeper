package minesweeper.solver.bulk;

import java.util.Random;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.bulk.BulkRequest.BulkAction;
import minesweeper.solver.settings.PlayStyle;
import minesweeper.solver.settings.SolverSettings;

abstract public class BulkController implements Runnable {
	
	private final int gamesToPlay;
	private final int workers;
	private final SolverSettings[] solverSettings;
	private final int bufferSize;
	private final BulkRequest[] buffer;
	private final BulkWorker[] bulkWorkers;
	
	private final static int REPORT_INTERVAL = 200;
	private final static int DEFAULT_BUFFER_PER_WORKER = 1000;
	
	private volatile int waitingSlot = -1;   // this is the next slot we are waiting to be returned
	private volatile int nextSlot = 0;      // this is the next slot to be dispatched
	
	private volatile int nextSequence = 1;
	private volatile int waitingSequence = 0;
	private volatile int reportInterval = REPORT_INTERVAL;
	private final Random seeder;
	private volatile boolean finished = false;
	private volatile BulkEventMain event;
	private volatile BulkEventMain finalEvent;
	
	private Thread mainThread;
	private long startTime;
	private long endTime;
	
	private BulkListener eventListener;
	private GamePostListener postGameListener;
	private GamePreListener[] preGameListeners;
	private BulkEventController[] bulkEventControllers;
	
	/*
	private volatile int failedToStart = 0;
	private volatile int wins = 0;
	private volatile int guesses = 0;
	private volatile int noGuessWins = 0;
	//private volatile BigDecimal totalGamesValue = BigDecimal.ZERO;
	
	private volatile int totalActions = 0;
	private volatile long total3BV = 0;
	private volatile long total3BVSolved = 0;
	private volatile BigDecimal fairness = BigDecimal.ZERO;
	private volatile int currentWinStreak = 0;
	private volatile int bestWinStreak = 0;
	private volatile int currentMastery = 0;
	private volatile int bestMastery = 0;
	
	private volatile boolean[] mastery = new boolean[100];
	*/
	
	private PlayStyle playStyle = PlayStyle.NO_FLAG;
	
	public BulkController(Random seeder, int gamesToPlay, SolverSettings solverSettings, int workers) {
		this(seeder, gamesToPlay, new SolverSettings[] {solverSettings}, workers, DEFAULT_BUFFER_PER_WORKER);
	}
	
	public BulkController(Random seeder, int gamesToPlay, SolverSettings[] solverSettings, int workers) {
		this(seeder, gamesToPlay, solverSettings, workers, DEFAULT_BUFFER_PER_WORKER);
	}
	
	public BulkController(Random seeder, int gamesToPlay, SolverSettings[] solverSettings, int workers, int bufferPerWorker) {
		this.seeder = seeder;
		this.gamesToPlay = gamesToPlay;
		this.workers = workers;
		this.bulkWorkers = new BulkWorker[this.workers];
		this.solverSettings = solverSettings;
		
		this.bufferSize = bufferPerWorker * this.workers;
		this.buffer = new BulkRequest[bufferSize];
		
		// allow for as many of these as there are solver settings
		this.preGameListeners = new GamePreListener[solverSettings.length];  
		this.bulkEventControllers = new BulkEventController[solverSettings.length];
		
		for (int i=0; i < solverSettings.length; i++) {
			this.bulkEventControllers[i] = new BulkEventController(this.gamesToPlay);
		}
	}
	
	public void registerEventListener(BulkListener listener) {
		this.eventListener = listener;
		
	}
	
	public void registerPostGameListener(GamePostListener listener) {
		this.postGameListener = listener;
	}
	
	public void registerPreGameListener(GamePreListener listener) {
		registerPreGameListener(0, listener);
	}
	public void registerPreGameListener(int index, GamePreListener listener) {
		this.preGameListeners[index] = listener;
	}
	
	// create an array from a list of things
	//static protected <T> T[] toArray(T... things) {
	//	return things;
	//}
	
	/**
	 * Set the play style
	 */
	public void setPlayStyle(PlayStyle playStyle) {
		this.playStyle = playStyle;
	}

	public PlayStyle getPlayStyle() {
		return this.playStyle;
	}
	
	
	public void setReportInterval(int reportInterval) {
		this.reportInterval = reportInterval;
	}
	
	/**
	 * Start the number of workers and wait for them to complete. If you don't want your main thread paused then run this on a separate thread.
	 */
	@Override
	public void run() {
		
		
		this.startTime = System.currentTimeMillis();
		
		// remember the current thread so we can wake it when completed
		mainThread = Thread.currentThread();
		
		for (int i=0; i < workers; i++) {
			bulkWorkers[i] = new BulkWorker(this, solverSettings);
			new Thread(bulkWorkers[i], "worker-" + (i+1)).start();
		}
		
		while (!finished) {
			try {
				Thread.sleep(10000);
				//System.out.println("Main thread waiting for bulk run to complete...");
			} catch (InterruptedException e) {
				//System.out.println("Main thread wait has been interrupted");

				// process the event and then set it to null
				if (event != null) {
					if (eventListener != null) {
						eventListener.intervalAction(event);
					}
					event = null;
				}
			}
		}

		this.endTime = System.currentTimeMillis();
		//System.out.println("Finished after " + getDuration() + " milliseconds");
		
	}
	
	/**
	 * Request each of the workers to stop and then stop the run
	 */
	public void stop() {
		
		for (BulkWorker worker: bulkWorkers) {
			worker.stop();
		}
		
		// create a final event
		finished = true;
		
		this.event = createEvent();
		this.finalEvent = this.event;
		
		// set the process to finished and wake the main thread
		mainThread.interrupt();
		
	}
	
	/**
	 * When the process is finished you can get the final results from here
	 */
	public BulkEventMain getResults() {
		return this.finalEvent;
	}
	
	/**
	 * Returns true when the bulk run is completed or been stopped
	 */
	public boolean isFinished() {
		return finished;
	}
	
	/**
	 * returns how log the bulk run took in milliseconds, or how long it has been running depending if it has finished ot not
	 * @return
	 */
	public long getDuration() {
		
		if (startTime == 0) {  // not started
			return 0;
		} else if (finished && endTime != 0) {  // finished
			return endTime - startTime;
		} else {
			return System.currentTimeMillis() - startTime;  // in flight
		}
		
	}
	
	private void processSlots() {
		
		boolean doEvent = false;
		BulkEventMain mainEvent = null; 
		
		// process all the games which have been processed and are waiting in the buffer 
		while (buffer[waitingSlot] != null) {
			
			BulkRequest request = buffer[waitingSlot];

			// accumulate the stats for each game played
			for (int i=0; i < solverSettings.length; i++) {
				this.bulkEventControllers[i].processGame(request, i);;
			}
			
			// clear the buffer and move on to the next slot
			buffer[waitingSlot] = null; 
			waitingSlot++;
			waitingSequence++;
			
			// recycle the buffer when we get beyond the top
			if (this.waitingSlot >= bufferSize) {
				this.waitingSlot = this.waitingSlot - bufferSize;
			}
			
			// if we have run and processed all the games then wake the main thread
			if (waitingSequence == gamesToPlay) {
				//System.out.println("All games played, waking the main thread");
				
				finished = true;
				
				this.finalEvent = createEvent();
				mainEvent = this.finalEvent;

				doEvent = true;
				
			// provide an update every now and again, do that on the main thread
			} else if (this.reportInterval != 0 && waitingSequence % this.reportInterval == 0) {
				mainEvent = createEvent();
				doEvent = true;
			}
		
			if (postGameListener != null) {
				postGameListener.postAction(request);
			}
			
		}
		
		// if we have an event to do then interrupt the main thread which will post it
		if (doEvent) {
			if (this.event == null) {
				this.event = mainEvent;
				mainThread.interrupt();
			} else {
				System.out.println("Event suppressed because earlier event is still in progress");
			}
		}
		
	}
	
	private BulkEventMain createEvent() {
		
		BulkEventMain event = new BulkEventMain();
		event.setGamesToPlay(gamesToPlay);
		event.setGamesPlayed(waitingSequence);
		
		long duration = getDuration();
		
		long timeLeft;
		if (waitingSequence != 0) {
			timeLeft = ((duration * gamesToPlay) / waitingSequence) - duration;
		} else {
			timeLeft = 0;
		}

		event.setTimeSoFar(duration);
		event.setEstimatedTimeLeft(timeLeft);
		
		event.setFinished(finished);
		
		BulkEventGame[] gameEvents = new BulkEventGame[this.solverSettings.length];
		
		for (int i=0; i < this.solverSettings.length; i++) {
			gameEvents[i] = this.bulkEventControllers[i].createEvent();		
		}
	
		event.setGameEvents(gameEvents);
		
		return event;
		
	}
	
	/**
	 * Returns the last request and gets the next
	 */
	protected synchronized BulkRequest getNextRequest(BulkRequest request) {
		
		if (request != null) {
			buffer[request.slot] = request;
			
			// if this is the slot we are waiting on then process the games which are in the buffer - this is all synchronised so nothing else arrives will it happens
			if (request.slot == waitingSlot) {
				processSlots();
			}
		}
		
		// if we have played all the games or we have been stopped then tell the workers to stop
		if (nextSequence > gamesToPlay || finished) {
			return BulkRequest.STOP;			
		}
		
		// if the next sequence is a long way ahead of the waiting sequence then wait until we catch up.  Tell the worker to wait.
		if (nextSequence > waitingSequence + bufferSize - 2) {
			System.out.println("Buffer is full after " + nextSequence + " games dispatched");
			return BulkRequest.WAIT;
		}
		
		
		BulkRequest next = new BulkRequest();
		next.action = BulkAction.RUN;
		next.sequence = this.nextSequence;
		next.slot = this.nextSlot;
		
		next.games = new BulkRequestGame[solverSettings.length];
		
		long seed = Math.abs(seeder.nextLong() & 0xFFFFFFFFFFFFFl);
		
		for (int i=0; i < solverSettings.length; i++) {
			
			BulkRequestGame game = new BulkRequestGame();
			
			game.solverSettings = solverSettings[i];
			game.gs = getGameState(seed);
			
			// if this setting hs a pre-game listener than run it
			if (this.preGameListeners[i] != null) {
				this.preGameListeners[i].preAction(game.gs);
			}
			
			if (game.gs.getGameState() == GameStateModel.LOST) {
				game.startedOkay = false;
			}
			
			next.games[i] = game;
			
		}

		// roll onto the next sequence
		this.nextSequence++;
		this.nextSlot++;
		
		// if this is the first request then initialise the waiting slot
		if (waitingSlot == -1) {
			waitingSlot = 0;
		}
		
		// recycle the buffer when we get beyond the top
		if (this.nextSlot >= bufferSize) {
			this.nextSlot = this.nextSlot - bufferSize;
		}
		
		return next;
		
	}

	abstract protected GameStateModel getGameState(long seed);

	
}
