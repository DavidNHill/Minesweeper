package minesweeper.gamestate;

public enum MoveMethod {

	HUMAN("Human"),
	BOOK("Opening Book"),
	CORRECTION("Correction"),
	TRIVIAL("Trivial analysis"),
	LOCAL("Local analysis"),
	PROBABILITY_ENGINE("Probability Engine"),
	BRUTE_FORCE("Brute Force"),
	BRUTE_FORCE_DEEP_ANALYSIS("Brute Force Deep Analysis"),
	GUESS("Guess"),
	ROLLOUT("Rollout"),
	UNAVOIDABLE_GUESS("Unavoidable Guess");
	
	public final String description;
	
	
	private MoveMethod(String description) {
		this.description = description;
	}
	
}
