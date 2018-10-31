package edu.kit.minijava.cli;

import java.io.*;

import edu.kit.minijava.lexer.*;
import edu.kit.minijava.parser.*;

public class ParserCommand extends Command {

    @Override
    public int execute(String path) {
       
        try {
            
            FileInputStream stream = new FileInputStream(path);
            InputStreamReader reader = new InputStreamReader(stream, "US-ASCII");
            
            Lexer lexer = new Lexer(reader);
            Parser parser = new Parser(lexer);
            Program program = parser.parseProgram();

            if (program == null) {
                throw new AssertionError();
            }

            return 0;
        }
        catch (ParserException exception) {
            System.err.println("error: " + exception.getLocalizedMessage());

            return 1;
        }
        catch (FileNotFoundException exception) {
            System.err.println("error: File '" + path + "' was not found!");

            return 1;
        } 
        catch (IOException exception) {
            System.err.println("error: File '" + path + "' could not be read!");

            return 1;
        }
       
        
    }
}
