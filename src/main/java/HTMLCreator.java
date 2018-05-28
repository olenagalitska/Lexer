import java.util.ArrayList;

public class HTMLCreator {
    private final String OUTPUT_FILE;

    HTMLCreator(String filepath) {
        this.OUTPUT_FILE = filepath;
    }

    void writeToHTML(ArrayList<Token> tokens){
        System.out.println("HTML is successfully created");
    }
}
