package com.some.aapie;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import com.some.aapie.exception.*;
import static com.some.aapie.TokenType.*;

public class Parser {
	/* The following Tokens are operands:
	 * - NUMBER
	 * - CELL
	 */
	private LinkedList<Token<?>> postfix;
	private LinkedList<Integer> arityStack;
	private static HashSet<String> librarySet = new HashSet<String>();
	private Lexer infix;

	static{
		librarySet.add("java.lang.Math");
	}
	
	/**
	 * Creates a new expression Parser.
	 * @param infix The {@link Lexer} containing the expression 
	 */
	public Parser(Lexer infix){
		this.infix = infix;
		postfix = new LinkedList<Token<?>>();
		arityStack = new LinkedList<Integer>();
	}

	/**
	 * Creates a new expression Parser.
	 * @param expr The String containing the expression
	 */
	public Parser(String expr){
		this(new Lexer(expr));
	}

	public static void addLibrary(String s){
		librarySet.add(s);
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
		LinkedList<Token<?>> operators = new LinkedList<Token<?>>();
		LinkedList<Integer> numargsStack = new LinkedList<Integer>();
		boolean lastWasNumber = false;
		Token<?> currentToken;
		
		while(infix.hasNext()){
			currentToken = infix.next();
			
			switch (currentToken.type) {
			case NUMBER:
				postfix.push(currentToken);
				lastWasNumber = true;
				break;
			case STRING:
				postfix.push(currentToken);
				lastWasNumber = false;
				break;
			case MULT: case DIV: case MOD:
				while(!operators.isEmpty() &&
					  (operators.getFirst().type == MULT ||
					   operators.getFirst().type == DIV ||
					   operators.getFirst().type == MOD ||
					   operators.getFirst().type == UNARYMINUS))
				{
					postfix.push(operators.pop());
				}
				operators.push(currentToken);
				lastWasNumber = false;
				break;
			case PLUS: case MINUS:
				if(!lastWasNumber && currentToken.type == MINUS) {
					operators.push(new Token<Object>(UNARYMINUS, null));
				} else {
					while(!operators.isEmpty() &&
						  (operators.getFirst().type == PLUS ||
						   operators.getFirst().type == MINUS ||
						   operators.getFirst().type == MULT ||
						   operators.getFirst().type == DIV ||
						   operators.getFirst().type == MOD ||
						   operators.getFirst().type == UNARYMINUS))
					{
						postfix.push(operators.pop());
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
						postfix.push(operators.pop());
					}
					operators.pop();
					if (!operators.isEmpty() && operators.getFirst().type == WORD){
						postfix.push(operators.pop());
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
						postfix.push(operators.pop());
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
					postfix.push(operators.pop());
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
		if (postfix.isEmpty()) {
			toPostfix();
		}
		
		LinkedList<Object> evalStack = new LinkedList<Object>();

		while(!postfix.isEmpty()){
			switch (postfix.getLast().type) {
			case UNARYMINUS:
				Object arg;
				try {
					arg = evalStack.pop();
				} catch (NoSuchElementException e) {
					throw new MissingArgException();
				}
				
				if (arg instanceof Double) {
					postfix.removeLast();
					evalStack.push(-(Double)arg);
				} else {
					throw new MissingArgException();
				}
				break;
			case NUMBER:
				evalStack.push((Double) postfix.removeLast().data);
				break;
			case STRING:
				evalStack.push(postfix.removeLast().data);
				break;
			case MULT: case DIV: case MOD: case PLUS: case MINUS:
				Object a, b;
				Token<?> op;
				try {
					b = evalStack.pop();
					a = evalStack.pop();
					op = postfix.removeLast();
				} catch (NoSuchElementException e) {
					throw new MissingArgException();
				}
				
				if (a instanceof String && ((String) a).length() == 0)
					a = 0.0;
				if (b instanceof String && ((String) b).length() == 0)
					b = 0.0;
				
				if (a instanceof Double && b instanceof Double) {
					if (op.type == PLUS) {
						evalStack.push(new Double((Double)a + (Double)b));
					} else if (op.type == MINUS) {
						evalStack.push(new Double((Double)a - (Double)b));
					} else if (op.type == MULT) {
						evalStack.push(new Double((Double)a * (Double)b));
					} else if (op.type == DIV) {
						evalStack.push(new Double((Double)a / (Double)b));
					} else if (op.type == MOD) {
						evalStack.push(new Double((Double)a % (Double)b));
					}
				} else {
					if (op.type == PLUS) {
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
					evalStack.push(evalFunction((String) postfix.removeLast().data, args));
				} catch (NoSuchElementException e) {
					throw new MissingArgException();
				}
				break;
			case LBRACKET:
				throw new MissingRBracketException();
			}
		}
		
		return evalStack.pop();
	}
	
	/**
	 * Evaluates a function using the static methods in class <code>java.lang.Math</code>.
	 * @param function The function name
	 * @param args The function arguments (1..*)
	 * @return The evaluated function
	 * @throws FormulaException
	 */
	private Object evalFunction(String functionName, Object ... args) throws FormulaException {
		Object value = null;

		Class<?>[] parameters = new Class<?>[args.length];
		
		for(int i = 0; i < args.length; i++){
			if(args[i] instanceof Double){
				parameters[i] = double.class;
			}else if(args[i] instanceof Integer){
				parameters[i] = int.class;
			}else{
				parameters[i] = args[i].getClass();
			}
		}
		
		Class<?> mathClass = null;
		Method mathFunction = null;
		for(String s : Parser.librarySet){
			try {
				mathClass = Class.forName(s);
				mathFunction = mathClass.getMethod(functionName, parameters);
			} catch (ClassNotFoundException cnf) {
				continue;
			} catch (NoSuchMethodException e) {
				continue;
			}
		}

		if(mathClass != null && mathFunction != null){
			try {
				value = mathFunction.invoke(null, args);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				System.err.println("Invalid arguments for function " + functionName);
			} catch (InvocationTargetException e) {
				System.err.println("Exception thrown by function " + functionName);
			}
		} else {
			throw new FormulaException();
		}

		return value;
	}
}
