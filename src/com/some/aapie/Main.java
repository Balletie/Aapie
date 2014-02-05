package com.some.aapie;

import java.util.Scanner;

import org.apache.commons.cli.*;
import com.some.aapie.exception.*;

/**Example CommandLine program using the Parser API
 * 
 * @author Skip Lentz
 * @since 2014-02-05
 */
public class Main {
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		//Define Options
		Options options = new Options();
		options.addOption("h", "help", false, "print help message");
		options.addOption(OptionBuilder.withLongOpt("library")
									   .withDescription("use additional java math libraries")
									   .hasArgs()
									   .withArgName("Java class path(s)")
									   .create('l'));
		options.addOption("i", "interactive", false, "enable interactive mode");
		boolean interactive = false;
		
		//Construct CommandLineParser
		CommandLineParser parser = new BasicParser();
		CommandLine line;
		try {
			line = parser.parse(options, args);
			if(line.hasOption('h') || args.length == 0){
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("aap [options] [expression]", options);
			}
			if(line.hasOption('l')){
				String[] libraries = line.getOptionValues('l');
				for(String s : libraries){					
					Parser.addClass(s);
				}
			}
			if(line.hasOption('i')){
				interactive = true;
			}
			args = line.getArgs();
			if(args.length != 0){
				String expression = args[0];
				for(int i = 1; i < args.length; i++){
					expression += args[i];
				}
				Parser eval = new Parser(expression);
				System.out.println(eval.eval().toString());
			}
		} catch (ParseException p) {
			System.out.println("Unexpected exception:" + p.getMessage());
		} catch (ParserException e) {
			handleException(e);
		}

		Scanner sc = new Scanner(System.in);
		while(interactive){
			System.out.print("> ");
			String expression = sc.nextLine();
			if(expression.equals("quit")){
				interactive = false;
				continue;
			}
			Parser eval = new Parser(expression);
			try {
				System.out.println(eval.eval().toString());
			} catch (ParserException e) {
				handleException(e);
			}
		}
		sc.close();
	}

	public static void handleException(ParserException e){
		if(e instanceof FormulaException){
			System.err.println("No such library or function");
		}else if(e instanceof MissingArgException) {
			e.printStackTrace();
			System.err.println("Missing argument in expression");
		}else {
			e.printStackTrace();
			System.err.println("Missing bracket in expression");
		}		
	}
}
