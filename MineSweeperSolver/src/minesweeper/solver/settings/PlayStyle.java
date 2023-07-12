package minesweeper.solver.settings;

public enum PlayStyle {
	FLAGGED(false, false),
	NO_FLAG(true, false),
	EFFICIENCY(false, true),
	NO_FLAG_EFFICIENCY(true, true);
	
	public final boolean flagless;
	public final boolean efficiency;
	
	private PlayStyle(boolean flagless, boolean efficiency) {
		this.flagless = flagless;
		this.efficiency = efficiency;
	}
}
