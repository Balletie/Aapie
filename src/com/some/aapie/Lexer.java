package com.some.aapie;

import java.util.LinkedList;
import static com.some.aapie.TokenType.*;

/**
 * This class splits an incoming String into Tokens for analysis by the {@link Parser}.
 * @author Team Awesome
 */
public class Lexer {
	private LinkedList<Token<?>> tokens;
	private StringBuilder token = new StringBuilder();
	
	private enum State {
		/**
		 * Used when the Lexer's position is not inside a {@link Token}.
		 */
		NONE,
		/**
		 * Used when the Lexer's position is inside a NUMBER {@link Token}.
		 */
		NUMBER,
		/**
		 * Used when the Lexer's position is inside a WORD {@link Token}.
		 */
		WORD
	}
	
	private State state = State.NONE;
	
	/**
	 * Creates a new expression {@link Lexer}.
	 * @param input	The String containing the expression
	 */
	public Lexer(String input) {
		this.tokens = new LinkedList<Token<?>>();
		
		if (input == null)
			return;
		
	    for (int i = 0; i < input.length(); i++) {
	    	char ch = input.charAt(i);
	    	
	    	switch (ch) {
	    	case '*': case '/': case '%':
	    		setState(State.NONE);
	    		tokens.add(new Token<Character>(MULTDIV, ch));
	    		break;
	    	case '+': case '-':
	    		setState(State.NONE);
	    		tokens.add(new Token<Character>(PLUSMINUS, ch));
	    		break;
	    	case '(':
	    		setState(State.NONE);
	    		tokens.add(new Token<Object>(LBRACKET, null));
	    		break;
	    	case ')':
	    		setState(State.NONE);
	    		tokens.add(new Token<Object>(RBRACKET, null));
	    		break;
	    	case ',':
	    		setState(State.NONE);
	    		tokens.add(new Token<Object>(DELIM, null));
	    		break;
	    	case '.':
	    		setState(State.NUMBER);
	    		token.append(ch);
	    		break;
	    	case ' ': case '\t': case '\n': case '\r': case '\f':
	    		setState(State.NONE);
	    		break;
	    	case '"':
	    		setState(State.NONE);
	    		while (input.charAt(++i) != '"') {
	    			token.append(input.charAt(i));
	    		}
	    		tokens.add(new Token<String>(STRING, token.toString()));
	    		token = new StringBuilder();
	    		break;
	    	default:
	    		if (Character.isDigit(ch)) {
    				setState(State.NUMBER);
    				token.append(ch);
	    		} else if (Character.isLetter(ch)) {
		    		setState(State.WORD);
	    			token.append(ch);
	    		}
	    	}
	    }
	    setState(State.NONE);
	    tokens.add(new Token<Object>(EOL, null));
	}
	
	/**
	 * Checks whether the {@link Lexer} has another {@link Token} in its input. 
	 * @return True is the {@link Lexer} has another {@link Token} in its input
	 */
	public boolean hasNext() {
		return !tokens.isEmpty();
	}
	
	/**
	 * Returns the next {@link Token} the {@link Lexer} has in its input.
	 * @return The next {@link Token}
	 */
	public Token<?> next() {
		return hasNext() ? tokens.pop() : new Token<Object>(EOL, null);
	}
	
	/**
	 * Changes the parser's internal {@link State}.
	 * @param state	The {@link State} we want to change to
	 */
	private void setState(State state) {
		if (this.state != state) {
			if (this.state == State.NUMBER) {
				tokens.add(new Token<Double>(NUMBER, Double.valueOf(token.toString())));
			} else if (this.state == State.WORD) {
				tokens.add(new Token<String>(WORD, token.toString()));
			}
			token = new StringBuilder();
		}
		this.state = state;
	}
}
