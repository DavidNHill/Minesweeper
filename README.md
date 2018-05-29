# Minesweeper
My Minesweeper implementation with automated Solver

![GUI](https://github.com/DavidNHill/Minesweeper/blob/master/Images/ReadMe/GUI.JPG)

For expert has a 40% win rate using traditional rules (30x16x99 safe but not always zero start) and 53% for modern rules (30x16x99 zero start)

The solver uses various strategies to try and solve the puzzle, each being more expensive in processing time.

1) Trivial search

The Solver looks for positions where:

a) a tile is fully satisfied, but still has surrounding tiles which can be cleared

![Trivial clear](https://github.com/DavidNHill/Minesweeper/blob/master/Images/ReadMe/Trivial_analysis2.jpg)

b) a file needs 'n' mines to be satisifed and has only 'n' surounding tiles remaining to put them

![Trivial flag](https://github.com/DavidNHill/Minesweeper/blob/master/Images/ReadMe/Trivial_analysis1.JPG)


2) Local search

Local search considers each revealed tile and tries every way of placing the remaining mines around it. Doing this will in many cases reveal tiles which must be either clear or a mine. In the example below, the 2 mines can be placed in 10 distimct ways in the 5 surrounding tiles. Only 4 of those satisify the other revealed tiles and in none of those cases is the marked tile a mine.

![Local search](https://github.com/DavidNHill/Minesweeper/blob/master/Images/ReadMe/Local_analysis1.JPG)

3) Probability Engine

If no moves are found using methods 1 & 2 then the Probability Engine is used to calculate the exact (to 20 decimal places) probability of a tile being clear for every remaining unrevealed tile.

In this example there are no certain moves to be found

![Probability Engine](https://github.com/DavidNHill/Minesweeper/blob/master/Images/ReadMe/Probability_engine1.JPG)


The Solver will also consider all moves which have the same or similar best probabilities and try to pick the 'best move'. Below is an example where 3 tiles have the same probability (small red dots on them). The Solver picks the tile which has the possibility of being useful. If the chosen tile reveals a '1' then local search will immediately be able to clear other tiles around it. The other 2 tiles will not help move the puzzle forward and another guess will always be needed.  

![Probability Engine best option](https://github.com/DavidNHill/Minesweeper/blob/master/Images/ReadMe/Probability_engine2.JPG)

The probabilty engine is not able to answer the question; how likely is this tile to be a '1'. To do that we need...

4) Brute Force

When there are relatively few ways remaining to place the remaining mines then all possible positions can be considered. Those which don't contradict the revealed tiles are candidate solutions. Once we have all the candidate solutions we can now answer the question; what is the probability of any given tile having a particular value?

This information can be used to pick guesses which are mathematically more likely to result in positions which can be solved by local analysis.

When the number of candidate solutions is small then Brute Force Deep Analysis can be used to completely solve the position.

5) Brute Force Deep Analysis

When the number of candidate solutions is less then 4000 the solver will create a game tree which plays out every possible sequence of moves and determine which route down the tree provides the best probability of winning the game. This is in contrast to the best probability of not revealing a mine, which is mainly what the Probability Engine is doing.

This approach is perfect in terms of game analysis and allows the Solver to get a significant up lift in win rate compared to other purely probability based solvers.

In the example below There are 8 tiles which have a 75% chance of being clear. Many solvers would pick one of these. Using Brute Force Deep Analysis the Solver picks a move with 66.67% probability of being clear, but which give better long term odds of winning the game. 

![Probability Engine best option](https://github.com/DavidNHill/Minesweeper/blob/master/Images/ReadMe/BruteForce_deep_analysis1.JPG)

----- Brute Force Deep Analysis starting ----

1008 solutions in BruteForceAnalysis

15 (12,1) is living with 5 possible values and probability 75.00, winning probability is 44.64

17 (13,1) is living with 5 possible values and probability 75.00, winning probability is 41.67

19 (14,1) is living with 4 possible values and probability 75.00, winning probability is 43.45

12 (11,0) is living with 3 possible values and probability 75.00, this location was pruned

13 (11,1) is living with 3 possible values and probability 75.00, winning probability is 41.57

14 (12,0) is living with 3 possible values and probability 75.00, winning probability is 44.25

16 (13,0) is living with 3 possible values and probability 75.00, winning probability is 41.77

18 (14,0) is living with 3 possible values and probability 75.00, winning probability is 43.45

3 (12,2) is living with 5 possible values and probability 66.67, this location was pruned

9 (13,2) is living with 5 possible values and probability 66.67, this location was pruned

0 (10,2) is living with 4 possible values and probability 66.67, this location was pruned

11 (14,2) is living with 4 possible values and probability 66.67, winning probability is 46.23

2 (11,2) is living with 3 possible values and probability 66.67, this location was pruned

1 (10,3) is living with 2 possible values and probability 66.67, winning probability is 33.33

10 (13,3) is living with 2 possible values and probability 66.67, this location was pruned

6 (15,1) is living with 5 possible values and probability 50.00, this location was pruned

5 (15,0) is living with 4 possible values and probability 50.00, this location was pruned

4 (12,3) is living with 2 possible values and probability 33.33, this location was pruned

Total nodes in cache = 28432 total cache hits = 11453

process took 253 milliseconds and explored 67984 nodes

----- Brute Force Deep Analysis finished ----

