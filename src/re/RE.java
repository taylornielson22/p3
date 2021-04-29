package re;

import java.util.LinkedHashSet;
import java.util.Set;


import fa.State;
import fa.nfa.NFA;
import fa.nfa.NFAState;

/**
 * Constructs an NFA for a given regular expression
 *
 */
public class RE implements REInterface {
	String regEx = "";
	int stateCounter = 1;
	
	/**
	 * Constructor
	 * @param regEx String of the regular expression
	 */
	public RE(String regEx) {
		this.regEx = regEx;
	}

	/**
	 * @return The NFA built from the regular expression
	 */
	public NFA getNFA() {
		return regEx();
	}

	/**
	 * Builds an NFA from the regular expression
	 * @return NFA 
	 */
	private NFA regEx() {
		NFA term = term();

		//If the regex requires a union operation
		if (more() && peek() == '|') {

			eat ('|') ;
			NFA regex = regEx() ;
			//now create a union of term and regex NFA
			//1. create a new NFA
			NFA nfa = new NFA();
			//create a start state for it
			String startName = "q" + stateCounter;
			stateCounter++;
			nfa.addStartState(startName);

			//add all states from other NFAS
			nfa.addNFAStates(term.getStates());
			nfa.addNFAStates(regex.getStates());

			//add the transitions between startName and startStates of other two NFAs
			nfa.addTransition(startName, 'e', term.getStartState().getName());
			nfa.addTransition(startName, 'e', regex.getStartState().getName());

			//add the alphabets of the other two NFAs to new NFA
			nfa.addAbc(term.getABC());
			nfa.addAbc(regex.getABC());

			return nfa;

		//If no union is needed, just return the simple NFA
		} else {
			return term;
		}
	}

	/**
	 * Looks at the regex to determine what it needs to build
	 * @return parsed portion of the regex in the form of an NFA
	 */
	private NFA term() {
		NFA factor = new NFA();
		
		while( more() && peek() != ')' && peek() != '|'){
			NFA fact = factor();
			
			//If RE tries to process regex and there no operations to perform, just return simplest NFA
			if(factor.getStates().isEmpty()){
				factor = fact;
			//If there are multiple terms following each other, perform concatenate operation on the NFAs
			}else{
				factor = connect(factor, fact);

			}
		}
		
		return factor;
	}
	
	/**
	 * Concatenates the two NFAs, when two NFAs are in the regex together
	 * @param reg1 - the NFA we are adding reg2 onto
	 * @param reg2 - additional NFA that follows reg1
	 * @return NFA that has been concatenated
	 */
	private NFA connect(NFA reg1, NFA reg2) {
		//Get final states and name of start state in second NFA
		String reg2Start = reg2.getStartState().getName();
		Set<State> reg1Finals = reg1.getFinalStates();

		//Add all states from second NFA to first NFA
		reg1.addNFAStates(reg2.getStates());
		
		//Make sure first NFA's final states are not final
		//But add their transitions to begining of second NFA
		for(State state: reg1Finals) {
			((NFAState)state).setNonFinal();
			reg1.addTransition(state.getName(), 'e', reg2Start);
		}
		
		//Make sure both alphabets are included
		reg1.addAbc(reg2.getABC());
		
		return reg1;
	}

	/**
	 * A factor is a base followed by a possibly empty sequence of '*'.
	 * @return root NFA or root with the star operator if the regex has a '*'
	 */
	private NFA factor() {
		NFA base = base();
		
		//If star op is needed, descend into recursion
		while(more() && peek() == '*'){
			eat('*');
			base = star(base);
		}
		return base;
	}

	/**
	 * Handles the star operator with a union
	 * @param root - NFA we are building upon
	 * @return NFA - the previous NFA has now incorporated the star operator
	 */
	private NFA star(NFA root) {

		NFA union = new NFA();
		
		//Make new start state
		String start = "q" + stateCounter;
		stateCounter++;
		union.addStartState(start);
		
		//Make new final state
		String finState = "q" + stateCounter;
		stateCounter++;
		union.addFinalState(finState);
		
		//Add all states from root to new NFA
		union.addNFAStates(root.getStates());
		
		//Add empty transitions because star allows for 0 occurrences of term/NFA
		union.addTransition(start, 'e', finState);
		union.addTransition(finState, 'e', root.getStartState().getName());
		
		//Tie new start to root NFA
		union.addTransition(start, 'e', root.getStartState().getName());
		
		//Make sure old alphabet is included
		union.addAbc(root.getABC());
		
		//Add empty transitions from old final states to new final state
		for(State state: root.getFinalStates()) {
			union.addTransition(state.getName(), 'e', finState);
			
			//Make sure new final is the only final state in new NFA
			for(State s2: union.getFinalStates()){
				if(s2.getName().equals(state.getName())) {
					((NFAState)s2).setNonFinal();
				}
			}
		}

    	return union;
	}

	/**
	 * base is a character, an escaped character, or a parenthesized regular expression.
	 * @return an NFA built from the next symbol or within the parenthesis
	 */
	private NFA base() {
		if(peek() == '('){
			eat('(');
			NFA reg = regEx();
			eat(')');
			return reg;
		}
			return symbol(next());
		
	}

	/**
	 * Builds an NFA from the given character
	 * @param c Character to define transition on
	 * @return NFA from given character
	 */
	private NFA symbol(char c) {
		NFA nfa = new NFA();
		
		//Make a new NFA of 2 states and a transition on input char
		String s = "q" + stateCounter++;
		nfa.addStartState(s);
		String f = "q" + stateCounter++;
		nfa.addFinalState(f);
		
		nfa.addTransition(s, c, f);
		
		Set<Character> alphabet = new LinkedHashSet<Character>();
		alphabet.add(c);
		nfa.addAbc(alphabet);
		return nfa;
		
	}

	/**
	 * Peeks at the first index(current character) of the regEx 
	 * @return The next unprocessed character in the regEx
	 */
	private char peek(){
		return regEx.charAt(0);
	}
	
	/**
	 * Checks if input char is the current character, and will remove it if so.
	 * @param c Character to process
	 */
	private void eat(char c){
		if(peek() == c){
			this.regEx = this.regEx.substring(1);
		}else{
			throw new RuntimeException("Received: " + peek() + "\n" + "Expected: " + c);
		}
	}
	
	/**
	 * Removes current character from regEx and returns the next current character
	 * @return the character that was processed
	 */
	private char next(){
		char c = peek();
		eat(c);
		return c;
	}
	
	/**
	 * Returns true if there are more characters to read in regEx
	 * @return boolean
	 */
	private boolean more(){
		return regEx.length() > 0;
	}
	

}