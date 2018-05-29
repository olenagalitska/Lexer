import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class SwiftLang {
    private ArrayList<String> keywords;
    private ArrayList<String> punctuations;

    SwiftLang(String keywordsFile, String punctFile) {
        keywords = new ArrayList<String>();
        punctuations = new ArrayList<String>();

        readLines(keywordsFile, keywords);
        readLines(punctFile, punctuations);
    }

    void readLines(String filename, List output) {
        try{

            Scanner scanner = new Scanner(new File(filename));
            while (scanner.hasNext()){
                output.add(scanner.nextLine());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isKeyword(String lexeme){
        return keywords.contains(lexeme);
    }

    boolean isPunctuationMark(String lexeme){
        return punctuations.contains(lexeme);
    }

    boolean isLineBreak(char character){
        // carriage return or line feed
        return character == 10 || character == 13;
    }

    boolean isIdHead(char character) {
        // uppercase letter or lowercase letter
        if ((character >= 65 && character <= 90) || (character >= 97 && character <= 122)) {
            return true;
        }

        // other unicode characters
        if (character == '_') {
            return true;
        }

        return false;
    }

    boolean isIdChar(char character) {
        if (isIdHead(character)) return true;
        if (character >= 48 && character <= 57) return true;
        return false;
    }
}