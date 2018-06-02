public class Main {
    public static void main(String[] args) {
        Lexer lexer = new Lexer("input/input.txt");
        lexer.generateHTML();
        lexer.printSequenceOfTokens();
    }
}
