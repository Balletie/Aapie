package com.some.aapie;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import com.some.aapie.exception.*;
import static com.some.aapie.TokenType.*;

public class Parser {
	/* The following Tokens are operands:
	 * - NUMBER
	 * - CELL
	 */
	private LinkedList<Token<?>> output;
	private LinkedList<Integer> arityStack;
	
	private Lexer lex;

	/**
	 * Creates a new expression Parser.
	 * @param lex The {@link Lexer} containing the expression 
	 */
	public Parser(Lexer lex){
		this.lex = lex;
		output = new LinkedList<Token<?>>();
		arityStack = new LinkedList<Integer>();
	}

	/**
	 * Creates a new expression Parser.
	 * @param expr The String containing the expression
	 */
	public Parser(String expr){
		this(new Lexer(expr));
	}
	
	/**
	 * Converts the expression in infix-notation to postfix-notation using the Shunting-yard Algorithm:<br>
	 * <br>
	 * infix               | postfix<br>
	 * ------------------- | -----------------<br>
	 * 3 + 7 / (4 * 5 - 6) | 3 7 4 5 * 6 - / +<br>
	 * @throws ParserException 
	 */
	@SuppressWarnings("incomplete-switch")
	public void toPostfix() throws ParserException{
		/*
		 * The following Tokens are operators:
		 * 
		 * - PLUS, MINUS
		 * - MULT, DIV
		 * - LBRACKET, RBRACKET
		 * - POW ?
		 * - FACT ?
		 */
		LinkedList<Token<?>> operators = new LinkedList<Token<?>>();
		LinkedList<Integer> numargsStack = new LinkedList<Integer>();
		boolean lastWasNumber = false;
		Token<?> currentToken;
		
		while(lex.hasNext()){
			currentToken = lex.next();
			
			switch (currentToken.type) {
			case NUMBER:
				output.push(currentToken);
				lastWasNumber = true;
				break;
			case STRING:
				output.push(currentToken);
				lastWasNumber = false;
				break;
			case MULTDIV:
				while(!operators.isEmpty() &&
					    (operators.getFirst().type == MULTDIV ||
						operators.getFirst().type == UNARYMINUS))
				{
					output.push(operators.pop());
				}
				operators.push(currentToken);
				lastWasNumber = false;
				break;
			case PLUSMINUS:
				if(!lastWasNumber && ((Character) currentToken.data) == '-') {
					operators.push(new Token<Object>(UNARYMINUS, null));
				} else {
					while(!operators.isEmpty() &&
						   (operators.getFirst().type == PLUSMINUS ||
							operators.getFirst().type == MULTDIV ||
							operators.getFirst().type == UNARYMINUS))
					{
						output.push(operators.pop());
					}
					operators.push(currentToken);
					lastWasNumber = false;
				}
				break;
			case LBRACKET:
				operators.push(currentToken);
				lastWasNumber = false;
				break;
			case RBRACKET:
				try {
					while(!(operators.getFirst().type == LBRACKET)){
						output.push(operators.pop());
					}
					operators.pop();
					if (!operators.isEmpty() && operators.getFirst().type == WORD){
						output.push(operators.pop());
						arityStack.push(numargsStack.pop());
					}
				} catch (NoSuchElementException e) {
					throw new MissingLBracketException();
				}
				break;
			case DELIM:
				try {
					Integer numargs = numargsStack.pop() + 1;
					numargsStack.push(numargs);
					while(!(operators.getFirst().type == LBRACKET)){
						output.push(operators.pop());
					}
				} catch (NoSuchElementException e) {
					throw new MissingLBracketException();
				}
				lastWasNumber = false;
				break;
			case WORD:
				numargsStack.push(1);
				operators.push(currentToken);
				lastWasNumber = false;
				break;
			case EOL:
				while(!operators.isEmpty()){
					output.push(operators.pop());
				}
				break;
			}
		}
	}
	
	/**
	 * Evaluates the stored mathematical expression represented in postfix-notation.
	 * @return The evaluated expression
	 * @throws ParserException
	 */
	@SuppressWarnings("incomplete-switch")
	public Object eval() throws ParserException {
		if (output.isEmpty()) {
			toPostfix();
		}
		
		LinkedList<Object> evalStack = new LinkedList<Object>();

		while(!output.isEmpty()){
			switch (output.getLast().type) {
			case UNARYMINUS:
				Object arg;
				try {
					arg = evalStack.pop();
				} catch (NoSuchElementException e) {
					throw new MissingArgException();
				}
				
				if (arg instanceof Double) {
					output.removeLast();
					evalStack.push(-(Double)arg);
				} else {
					throw new MissingArgException();
				}
				break;
			case NUMBER:
				evalStack.push((Double) output.removeLast().data);
				break;
			case STRING:
				evalStack.push(output.removeLast().data);
				break;
			case MULTDIV:
			case PLUSMINUS:
				Object a, b;
				Token<?> op;
				try {
					b = evalStack.pop();
					a = evalStack.pop();
					op = output.removeLast();
				} catch (NoSuchElementException e) {
					throw new MissingArgException();
				}
				
				if (a instanceof String && ((String) a).length() == 0)
					a = 0.0;
				if (b instanceof String && ((String) b).length() == 0)
					b = 0.0;
				
				if (a instanceof Double && b instanceof Double) {
					if ((Character) op.data == '+') {
						evalStack.push(new Double((Double)a + (Double)b));
					} else if ((Character) op.data == '-') {
						evalStack.push(new Double((Double)a - (Double)b));
					} else if ((Character) op.data == '*') {
						evalStack.push(new Double((Double)a * (Double)b));
					} else if ((Character) op.data == '/') {
						evalStack.push(new Double((Double)a / (Double)b));
					} else if ((Character) op.data == '%') {
						evalStack.push(new Double((Double)a % (Double) b));
					}
				} else {
					if ((Character) op.data == '+') {
						evalStack.push(a.toString() + b.toString());
					} else {
						throw new MissingArgException();
					}
				}
				break;
			case WORD:
				int numArgs = 0;
				Object args[];
				try {
					numArgs = arityStack.removeLast();
					args = new Object[numArgs];
				} catch (NoSuchElementException e) {
					throw new MissingLBracketException();
				}
				
				try {
					for (int i = numArgs - 1; i >= 0; i--) {
						args[i] = evalStack.pop();
					}
					evalStack.push(evalFunction((String) output.removeLast().data, args));
				} catch (NoSuchElementException e) {
					throw new MissingArgException();
				}
				break;
			case LBRACKET:
				throw new MissingRBracketException();
			}
		}
		
		Object retvalue = "";
		try {
			retvalue = evalStack.pop();
		} catch (NoSuchElementException e) {
			
		}
		
		if (retvalue instanceof Double) {
			retvalue = (Double)retvalue;
		}
		
		return retvalue;
	}
	
	/**
	 * Evaluates a function using the static methods in class <code>java.lang.Math</code>.
	 * @param function The function name
	 * @param args The function arguments (1..*)
	 * @return The evaluated function
	 * @throws FormulaException
	 */
	private Object evalFunction(String functionName, Object ... args) throws FormulaException {
		String className = "java.lang.Math";
		Object value = null;

		Class<?>[] parameters = new Class<?>[args.length];
		
		for(int i = 0; i < args.length; i++){
			if(args[i] instanceof Double){
				parameters[i] = double.class;
			} else {
				parameters[i] = args[i].getClass();
			}
		}
		
		try{
			Class<?> mathClass = Class.forName(className);
			Method mathFunction = mathClass.getMethod(functionName, parameters);
			value = mathFunction.invoke(null, args);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return value;
	}
}
