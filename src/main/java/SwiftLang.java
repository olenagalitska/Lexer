import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class SwiftLang {
    private ArrayList<String> keywords;
    private ArrayList<String> punctuations;
    private ArrayList<String> directives;

    SwiftLang(String keywordsFile, String punctFile, String dirFile) {
        keywords = new ArrayList<>();
        punctuations = new ArrayList<>();
        directives = new ArrayList<>();

        readLines(keywordsFile, keywords);
        readLines(punctFile, punctuations);
        readLines(dirFile, directives);
    }

    private void readLines(String filename, ArrayList<String> output) {
        try {

            Scanner scanner = new Scanner(new File(filename));
            while (scanner.hasNext()) {
                output.add(scanner.nextLine());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //-----------------------------------------------------------------------------

    //TODO: add all characters for every token type (now only main)

    boolean isKeyword(String lexeme) {
        return keywords.contains(clearEmptyChars(lexeme));
    }

    boolean isDirective(String lexeme) {
        return directives.contains(clearEmptyChars(lexeme));
    }

    boolean isPunctuationMark(String lexeme) {
        return punctuations.contains(lexeme) || lexeme.equals("-");
    }

    boolean isLineBreak(char character) {
        // carriage return or line feed
        return character == 10 || character == 13;
    }

    boolean isIdHead(char character) {
        // uppercase letter or lowercase letter
        return ((character >= 'a' && character <= 'z')
                || (character >= 'A' && character <= 'Z')
                || (character == '_'));
    }

    boolean isIdChar(char character) {
        return (isIdHead(character) || (character >= '0' && character <= '9'));
    }

    boolean isOperatorHead(char character) {
        char[] operatorHeads = {'/', '=', '-', '+', '!', '*', '%', '<', '>', '&', '|', '^', '~', '?'};
        for (char head : operatorHeads) {
            if (character == head)
                return true;
        }
        return false;
    }

    boolean isOperatorChar(char character) {
        return isOperatorHead(character);
    }

    //-----------------------------------------------------------------------------

    String clearEmptyChars(String input) {
        return input.replaceAll("\\s+", "");
    }
}