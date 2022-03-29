import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.Stack;

/**
 * A translator called mylang2IR is developed for a language called MyLang
 * @author Nurlan Dadashov(2019400300) and Aziza Mankenova(2018400387)
 *
 */
public class Main {
    /**
     * Keeps track of temporary variables
     */
    public static int temp = 1;
    /**
     * Keeps track of if statements
     */
    public static int ifTemp = 0;
    /**
     * Keeps track of while statement
     */
    public static int whileTemp = 0;
    /**
     * Keeps track of the current line
     */
    public static int currentLine = 0;
    /**
     * stores the one half of the output 
     */
    public static ArrayList < String > output = new ArrayList < String > ();
    /**
     * stores the first half of the output 
     */
    public static ArrayList < String > outputFirst = new ArrayList < String > ();
    /**
     * stores the indices of the lines where the error occurs 
     */
    public static ArrayList < Integer > errors = new ArrayList < Integer > ();
    /**
     * error variable, which is true when there is an error and false otherwise
     */
    public static boolean error = false;
    /**
     * stores the lines
     */
    public static ArrayList < String > lines = new ArrayList < String > ();
    /**
     * stores the variables
     */
    public static ArrayList < String > variables = new ArrayList < String > ();
    /**
     * the output
     */
    public static PrintStream out;
    /**
     * The main objective is to generate low-level LLVM IR code that 
     * will compute and output the statements.
     * Program carries out the necessary actions by
     * parsing the input file composed of the sequential operations.
     * The main method reads an input file that is composed of sequential
     * commands and prints the necessary 
     * output into the .ll file.
     * @param args input file name
     * @throws FileNotFoundException if such file was not found
     */
    public static void main(String[] args) throws FileNotFoundException {
        if (args.length != 1) {
            System.out.println("Expected one file!");
            return;
        }

        String input = args[0];
        Scanner in = new Scanner(new File(input));
        input = trim(input);
        input = input.substring(0, input.length()-2) +  "ll";


        String outputFile = input;
        out = new PrintStream(new File(outputFile));

        while ( in .hasNextLine()) {
            lines.add( in .nextLine());
        }

        currentLine = 0;

        outputFirst.add("; ModuleID = 'mylang2ir'");
        outputFirst.add("declare i32 @printf(i8*, ...)");
        outputFirst.add("@print.str = constant [4 x i8] c\"%d\\0A\\00\"");
        outputFirst.add("");

        outputFirst.add("define i32 @choose(i32 %$expr1, i32 %$expr2, i32 %$expr3, i32 %$expr4) {");
        outputFirst.add("$entry:");
        outputFirst.add("\t%$retval = alloca i32");
        outputFirst.add("\t%$0 = icmp ne i32 %$expr1, 0");
        outputFirst.add("\tbr i1 %$0, label %$ne0, label %$e0");
        outputFirst.add("");
        outputFirst.add("$ne0:");
        outputFirst.add("\t%$1 = icmp slt i32 %$expr1, 0");
        outputFirst.add("\tbr i1 %$1, label %$slt0, label %$sgt0");
        outputFirst.add("");
        outputFirst.add("$slt0:");
        outputFirst.add("\tstore i32 %$expr4, i32* %$retval");
        outputFirst.add("\tbr label %$end");
        outputFirst.add("");
        outputFirst.add("$sgt0:");
        outputFirst.add("\tstore i32 %$expr3, i32* %$retval");
        outputFirst.add("\tbr label %$end");
        outputFirst.add("");
        outputFirst.add("$e0:");
        outputFirst.add("\tstore i32 %$expr2, i32* %$retval");
        outputFirst.add("\tbr label %$end");
        outputFirst.add("");
        outputFirst.add("$end:");
        outputFirst.add("\t%$2 = load i32* %$retval");
        outputFirst.add("\tret i32 %$2");
        outputFirst.add("}");
        outputFirst.add("");

        outputFirst.add("define i32 @main() {");

        try {
            for (currentLine = 0; currentLine < lines.size(); currentLine++) {
                String line = lines.get(currentLine);

                if (line.contains("#")) {
                    line = line.substring(0, line.indexOf("#"));
                }

                if (!handle(line)) {
                    logError();
                    break;
                } else {
                    parse(line);
                }
            }
        } catch (Exception ex) {
            logError();
        }


        output.add("\tret i32 0");
        output.add("}");

        if (!error) {
            printOutput();
        } else {
            printError();
        }

        in .close();
    }
    /**
     * Trims the expression
     * @param exp the expression to trim
     * @return trimmed expression
     */
    public static String trim(String exp) {
        return exp.trim();
    }
    /**
     * Checks if the expression is empty or not
     * @param exp the expression to check if it is empty
     * @return true if the expression is empty and false otherwise
     */
    public static boolean isEmptyString(String exp) {
        if (exp.length() == 0) {
            logError();
            return true;
        }
        return false;
    }

    /**
     * Checks if the expression is in parentheses or contains choose statement,
     * otherwise checks if it is a variable
     * @param exp the experssion to check
     * @return the parsed expression
     */
    public static String parseVar(String exp) {
        exp = trim(exp);
        if (exp.length() > 0 && exp.charAt(0) == '(' && exp.charAt(exp.length() - 1) == ')') {
            return add(exp.substring(1, exp.length() - 1));
        }
        if (exp.contains("choose")) {
            return choose(exp);
        } else {
            return isVariable(exp);
        }
    }
    /**
     * Checks if the given expression is a variable,
     * and performs different operations if it was allocated
     * or not. Logs error if the variable name is invalid.
     * @param exp the expression to check
     * @return the string which contains  "%" and the temporary variable
     */
    public static String isVariable(String exp) {
        exp = trim(exp);
        if (isInteger(exp)) {
            return exp;
        } else {
            String tempStr = "%" + temp;
            if (variables.contains(exp)) {
                output.add("\t" + tempStr + " = load i32* %" + exp);
                temp++;
                return tempStr;
            } else if (isValidName(exp)) {
                outputFirst.add("\t%" + exp + " = alloca i32");
                outputFirst.add("\tstore i32 0, i32* %" + exp);
                output.add("\t" + tempStr + " = load i32* %" + exp);
                variables.add(exp);
                temp++;
                return tempStr;
            } else {
                logError();
                error = true;
            }
        }
        return "";
    }
    /**
     * Checks the validity of the variable name
     * @param name the name to check
     * @return true if the the varible name is valid and false otherwise
     */
    public static boolean isValidName(String name) {
        if (!Character.isAlphabetic(name.charAt(0))) {
            logError();
            return false;
        }
        if (!name.chars().allMatch(Character::isLetterOrDigit)) {
            logError();
            return false;
        }
        name = trim(name);
        if (name.equals("while") || name.equals("print") || name.equals("if") || name.equals("choose")) {
            logError();
            return false;
        }
        return true;
    }
    /**
     * Checks if the expression is an integer
     * @param exp the expression to be checked
     * @return true if the expression is integer and false otherwise
     */
    public static boolean isInteger(String exp) {
        try {
            int i = Integer.parseInt(exp);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
    /**
     * Logs the error 
     */
    public static void logError() {
        errors.add(currentLine);
        error = true;
    }
    /**
     * Prints the output 
     */
    public static void printOutput() {
        for (String line: outputFirst) {
            out.println(line);
        }
        for (String line: output) {
            out.println(line);
        }
    }
    /**
     * Prints the error
     */
    public static void printError() {
        Collections.sort(errors);
        out.println("; ModuleID = 'mylang2ir'");
        out.println("declare i32 @printf(i8*, ...)");
        out.println("@print.str = constant [23 x i8] c\"Line %d: syntax error\\0A\\00\"");
        out.println("");
        out.println("define i32 @main() {");
        out.println("call i32 (i8*, ...)* @printf(i8* getelementptr ([23 x i8]* @print.str, i32 0, i32 0), i32 " + errors.get(0) + " )");
        out.println("\tret i32 0");
        out.println("}");
    }
    /**
     * Checks the given line for syntax errors
     * @param line the line to check for errors
     * @return true if the line does not contain any errors and false otherwise
     */
    public static boolean handle(String line) {
        line = trim(line);

        int countOpenPar = 0;
        int countClosedPar = 0;
        int equalsCount = 0;

        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '(') countOpenPar++;
            if (line.charAt(i) == ')') countClosedPar++;
            if (line.charAt(i) == '=') equalsCount++;
        }

        if (countOpenPar != countClosedPar || equalsCount > 1) {
            return false;
        }

        if (line.length() > 0 && !line.contains("=") && !line.contains("}") && !line.contains("while") && !line.contains("if") && !line.contains("#") && !line.contains("print")) {
            return false;
        }

        return true;
    }
    /**
     * Parses a line depending on the statements written
     * @param line a line to parse
     */
    public static void parse(String line) {
        line = trim(line);

        if (line.contains("#")) {
            line = line.substring(0, line.indexOf("#"));
        }

        if (line.contains("=")) {
            int index = line.indexOf("=");
            String left = line.substring(0, index);
            String right = line.substring(index + 1);

            left = trim(left);

            if (!variables.contains(left) && isValidName(left)) {
                outputFirst.add("\t%" + left + " = alloca i32");
                outputFirst.add("\tstore i32 0" + ", i32* %" + left);
                variables.add(left);
            }

            String res = add(right);
            output.add("\tstore i32 " + res + ", i32* %" + left);
        } else if (line.contains("print")) {
            String print = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
            print = trim(print);
            String res = add(print);
            output.add("\t%" + temp + " = call i32 (i8*, ...)* @printf(i8* getelementptr ([4 x i8]* @print.str, i32 0, i32 0), i32 " + res + " )");
            temp++;
        } else if (line.contains("if")) {
            String ifCondText = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
            ifCondText = trim(ifCondText);

            if (!line.contains("{")) {
                logError();
                return;
            }

            ifCondition(ifCondText);

            String lineIf = line;
            ArrayList < String > ifBody = new ArrayList < String > ();
            int currentline = currentLine;
            currentline++;
            lineIf = lines.get(currentline);
            while (!lineIf.contains("}")) {
                if (lineIf.contains("#")) {
                    lineIf = lineIf.substring(0, lineIf.indexOf("#"));
                }
                if (!handle(lineIf) || lineIf.contains("while") || lineIf.contains("if")) {
                    currentLine = currentline;
                    logError();
                    break;
                }
                ifBody.add(lineIf);
                if (currentline + 1 < lines.size()) {
                    currentline++;
                    lineIf = lines.get(currentline);
                } else {
                    currentLine = currentline;
                    logError();
                    return;
                }
            }

            ifBody(ifBody);
            currentLine++;
        } else if (line.contains("while")) {
            String whileCondText = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
            whileCondText = trim(whileCondText);

            if (!line.contains("{")) {
                logError();
                return;
            }

            whileCondition(whileCondText);

            String lineWhile = line;
            ArrayList < String > whileBody = new ArrayList < String > ();
            int currentline = currentLine;
            currentline++;
           lineWhile = lines.get(currentline);
            while (!lineWhile.contains("}")) {
                if (lineWhile.contains("#")) {
                    lineWhile = lineWhile.substring(0, lineWhile.indexOf("#"));
                }
                if (lineWhile.contains("while") || lineWhile.contains("if")) {
                    currentLine = currentline;
                    logError();
                    return;
                }
                whileBody.add(lineWhile);
                if (currentline + 1 < lines.size()) {
                    currentline++;
                    lineWhile = lines.get(currentline);
                } else {
                    currentLine = currentline;
                    logError();
                    return;
                }
            }

            whileBody(whileBody);
            currentLine++;
        } else if (line.length() == 0) {

        } else {
            logError();
        }
    }
    /**
     * Calls the add function and adds the llvm code of "if" into the output.
     * @param exp the "if" condition
     */
    public static void ifCondition(String exp) {
        output.add("\tbr label %ifcond" + ifTemp);
        output.add("");
        output.add("ifcond" + ifTemp + ":");
        String res = add(exp);
        String tempVar = "%" + temp;
        output.add("\t" + tempVar + " = icmp ne i32 " + res + ", 0");
        output.add("\tbr i1 " + tempVar + ", label %ifbody" + ifTemp + ", label %ifend" + ifTemp);
        output.add("");
        temp++;
    }
    /**
     * Parses the body of "if" and adds the llvm
     * code of "if" into the output
     * @param body the array list of lines in the "if" body 
     */
    public static void ifBody(ArrayList < String > body) {
        output.add("ifbody" + ifTemp + ":");

        for (String line: body) {
            currentLine++;
            parse(line);
        }

        output.add("\tbr label %ifend" + ifTemp);
        output.add("");
        output.add("ifend" + ifTemp + ":");
        ifTemp++;
    }
    /**
     * Calls the add function and adds the llvm code of "while" into the output.
     * @param exp the "while" condition
     */
    public static void whileCondition(String exp) {
        output.add("\tbr label %whcond" + whileTemp);
        output.add("");
        output.add("whcond" + whileTemp + ":");
        String res = add(exp);
        String tempVar = "%" + temp;
        output.add("\t" + tempVar + " = icmp ne i32 " + res + ", 0");
        output.add("\tbr i1 " + tempVar + ", label %whbody" + whileTemp + ", label %whend" + whileTemp);
        output.add("");
        temp++;
    }
    /**
     * Parses the body of "while" and adds the llvm
     * code of "while" into the output
     * @param body the array list of lines in the "while" body
     */
    public static void whileBody(ArrayList < String > body) {
        output.add("whbody" + whileTemp + ":");

        for (String line: body) {
            currentLine++;
            parse(line);
        }

        output.add("\tbr label %whcond" + whileTemp);
        output.add("");
        output.add("whend" + whileTemp + ":");
        output.add("");
        whileTemp++;
    }
    /**
     * Handles addition operations
     * @param exp expression to parse
     * @return expression after the addition
     */
    public static String add(String exp) {

        exp = trim(exp);

        if (isEmptyString(exp)) {
            return "";
        }

        if (exp.contains("+")) {
            int len = exp.length() - 1;
            Stack < Character > parentheses = new Stack < Character > ();

            for (int index = len; index >= 0; index--) {
                char c = exp.charAt(index);
                if (c == ')')
                    parentheses.push(c);
                else if (c == '(') {
                    if (!parentheses.empty()) {
                        parentheses.pop();
                    }
                }

                if (parentheses.isEmpty() && exp.charAt(index) == '+') {
                    String left = add(exp.substring(0, index));
                    String right = add(exp.substring(index + 1));
                    String tempString = "%" + temp;
                    output.add("\t" + tempString + " = add i32 " + left + ", " + right);
                    temp++;
                    return tempString;
                }
            }
            return subtract(exp);

        } else {
            return subtract(exp);
        }
    }
    /**
     * Handles choose operation
     * @param exp expression to parse
     * @return expression after the choose operation
     */
    public static String choose(String exp) {
        exp = trim(exp);

        int indexBegin = exp.indexOf("choose");

        exp = exp.substring(indexBegin + 6);

        exp = trim(exp);

        if (exp.charAt(0) != '(' && exp.charAt(exp.length() - 1) != ')') {
            logError();
            return "";
        }

        Stack < Character > parentheses = new Stack < Character > ();

        exp = exp.substring(1, exp.length() - 1);

        ArrayList < String > exps = new ArrayList < String > ();


        exp += ",";

        String expr = "";
        for (int i = 0; i < exp.length(); i++) {
            char ch = exp.charAt(i);

            if (ch == ',') {
                exps.add(expr);
                expr = "";
                continue;
            }

            if (ch == 'c') {
                if (exp.substring(i, i + 6).equals("choose")) {
                    int start = i;
                    i += 6;
                    ch = exp.charAt(i);
                    while (ch != '(') {
                        if (ch == ' ') {
                            i++;
                        } else {
                            logError();
                            return "";
                        }
                        ch = exp.charAt(i);
                    }
                    parentheses.push(ch);
                    i++;
                    while (!parentheses.isEmpty()) {
                        ch = exp.charAt(i);

                        if (ch == '(')
                            parentheses.push(ch);
                        else if (ch == ')') {
                            if (!parentheses.empty()) {
                                parentheses.pop();
                            }
                        }

                        i++;
                    }
                    expr += exp.substring(start, i);
                    i--;
                } else {
                    expr += ch;
                }
            } else {
                expr += ch;
            }
        }


        String res[] = new String[4];

        if (exps.size() != 4) {
            logError();
            return "";
        }

        for (int i = 0; i < 4; i++) {
            res[i] = add(exps.get(i));
        }

        String tempString = "%" + temp;

        output.add("\t" + tempString + " = call i32 @choose(i32 " + res[0] + ",i32 " + res[1] + ",i32 " + res[2] + ",i32 " + res[3] + ")");

        temp++;



        return tempString;

    }
    /**
     * Handles division operation
     * @param exp expression to parse
     * @return expression after the division operation
     */
    public static String divide(String exp) {
        exp = trim(exp);
        if (isEmptyString(exp)) {
            return "";
        }

        if (exp.contains("/")) {
            int len = exp.length() - 1;
            Stack < Character > parentheses = new Stack < Character > ();

            for (int index = len; index >= 0; index--) {
                char c = exp.charAt(index);
                if (c == ')')
                    parentheses.push(c);
                else if (c == '(') {
                    if (!parentheses.empty()) {
                        parentheses.pop();
                    }
                }

                if (parentheses.isEmpty() && exp.charAt(index) == '/') {
                    String left = divide(exp.substring(0, index));
                    String right = divide(exp.substring(index + 1));
                    String tempString = "%" + temp;
                    output.add("\t" + tempString + " = sdiv i32 " + left + ", " + right);
                    temp++;
                    return tempString;
                }
            }
            return parseVar(exp);

        } else {
            return parseVar(exp);
        }


    }
    /**
     * Handles multiplication operation
     * @param exp expression to parse
     * @return expression after the multiplication operation
     */
    public static String multiply(String exp) {

        exp = trim(exp);
        if (isEmptyString(exp)) {
            return "";
        }

        if (exp.contains("*")) {

            int len = exp.length() - 1;
            Stack < Character > parentheses = new Stack < Character > ();

            for (int index = len; index >= 0; index--) {
                char c = exp.charAt(index);
                if (c == ')')
                    parentheses.push(c);
                else if (c == '(') {
                    if (!parentheses.empty()) {
                        parentheses.pop();
                    }
                }

                if (parentheses.isEmpty() && exp.charAt(index) == '*') {
                    String left = multiply(exp.substring(0, index));
                    String right = multiply(exp.substring(index + 1));
                    String tempString = "%" + temp;
                    output.add("\t" + tempString + " = mul i32 " + left + ", " + right);
                    temp++;
                    return tempString;
                }
            }
            return divide(exp);

        } else {
            return divide(exp);
        }
    }
    /**
     * Handles subtraction operation
     * @param exp expression to parse
     * @return expression after the subtraction
     */
    public static String subtract(String exp) {

        exp = trim(exp);
        if (isEmptyString(exp)) {
            return "";
        }

        if (exp.contains("-")) {

            int len = exp.length() - 1;
            Stack < Character > parentheses = new Stack < Character > ();

            for (int index = len; index >= 0; index--) {
                char c = exp.charAt(index);
                if (c == ')')
                    parentheses.push(c);
                else if (c == '(') {
                    if (!parentheses.empty()) {
                        parentheses.pop();
                    }
                }

                if (parentheses.isEmpty() && exp.charAt(index) == '-') {

                    String left = subtract(exp.substring(0, index));
                    
                    String right = subtract(exp.substring(index + 1));
                    String tempString = "%" + temp;
                    output.add("\t" + tempString + " = sub i32 " + left + ", " + right);
                    temp++;
                    return tempString;
                }
            }
            return multiply(exp);

        } else {
            return multiply(exp);
        }
    }
}