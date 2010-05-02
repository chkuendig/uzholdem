package com.stevebrecher.showdown;

import static java.lang.System.out;

public class Help {

	static void generalHelp() {

		out.println("This program displays a series of prompts for information.  After each prompt");
		out.println("it waits for a response from you.  End each of your responses with the Enter");
		out.println("or Return key (denoted by \"Enter/Return\" in this and other help messages).");
		out.println();
		out.println("Some prompts indicate valid responses in (parentheses).  And some indicate the");
		out.println("most typical situation in [square brackets]; to specify that condition, just");
		out.println("press Enter/Return by itself.");
		out.println();
		out.println("If you want to continue your response to a single prompt on another line (such");
		out.println("as when you need to enter many hole cards) end the line to be continued with");
		out.println("\"--\" (two hyphens).");
		out.println();
		out.println("For help with a specific response, type \"?\" and Enter/Return.");
		out.println("To quit the program, close its window or type Ctrl-Z and Enter/Return.");
	}

	static final String[] holeHelp = new String[] {
		"For each card enter the rank -- 2,3,4,5,6,7,8,9,(10 or T),J,Q,K,A --",
		"and suit -- c,d,h,s.  Enter two cards for each player.  Letters",
		"may be either upper- or lower-case." };

	static final String[] boardHelp = new String[] {
		"If no board cards are known (there's no flop yet) just press Enter/Return.",
		"If only the flop has been dealt, enter three cards; if the turn card has been",
		"dealt, enter four cards.  (You can also enter a partial flop -- one or two cards.)",
		"For each card enter the rank (2,3,4,5,6,7,8,9,10 or T,J,Q,K,A) and suit (c,d,h,s)." };
	
	static final String[] deadHelp = new String[] {
		"Enter any known cards which are unavailable to be dealt to the board or to",
		"players, if any, with unknown hole cards -- such as cards which have been",
		"exposed, perhaps accidentally.  If there are no such cards just press",
		"Enter/Return." };

	static final String[] unknownHelp = new String[] {
		"Enter the number of players holding unknown (\"random\") cards.  Only",
		"a small number of such players can be accommodated by this program",
		"because otherwise the number of possible outcomes would be too large.",
		"If there are no such players just press Enter/Return." };
	
	static final String[] confirmHelp = new String[] {
		"The program is ready to tabulate each possible outcome (\"deal\").  Nothing",
		"will happen while it is working.  When it is finished, you will see the results.",
		"The time it will take depends on your computer's speed and how many deals are",
		"required.  As a VERY rough rule of thumb, figure 2,000,000 deals per second per",
		"GHz of computer processor speed.",
		"To start the calculations, press Enter/Return.  To start over, type \"n\" and",
		"then press Enter/Return." };
}
