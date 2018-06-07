import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

class Lexer {
    private Queue<Character> inputQueue;
    private SwiftLang lang;

    private String lexemeBuffer;
    private ArrayList<Token> tokens;

    //-----------------------------------------------------------------------------

    Lexer(String filename) {
        lang = new SwiftLang("lang/keywords.txt", "lang/punctuation.txt",
                "lang/directives.txt");
        tokens = new ArrayList<>();

        String inputString = readFileContents(filename);
        inputQueue = new ArrayBlockingQueue<>(inputString.length());

        char[] input = inputString.toCharArray();
        for (char character : input) {
            if (character != 0) inputQueue.add(character);
        }

        tokenize();
    }

    private String readFileContents(String filename) {
        String result = "";

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            StringBuilder stringBuilder = new StringBuilder();
            char[] buffer = new char[10];
            while (reader.read(buffer) != -1) {
                stringBuilder.append(new String(buffer));
                buffer = new char[10];
            }
            reader.close();
            result = stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private void tokenize() {
        while (!inputQueue.isEmpty()) {
            lexemeBuffer = "";
            Token token = getToken();
            token.value = lang.clearEmptyChars(token.value);
            tokens.add(token);
        }
    }

    //-----------------------------------------------------------------------------

    void printSequenceOfTokens() {
        for (Token token : tokens) {
            System.out.println("--------------------------------------------------");
            System.out.println(token.type + " :" + token.value);
        }
    }

    void generateHTML() {
        HTMLGenerator.generate(tokens);
    }

    void printByType() {
        ArrayList<ArrayList<String>> valuesByType = new ArrayList<>();
        int typesCount = TokenType.values().length;
        for (int i = 0; i < typesCount; i++) {
            valuesByType.add(new ArrayList<>());
        }

        for (Token token : tokens) {
            String value = token.value;
            if (!(token.type == TokenType.COMMENT || token.type == TokenType.LITERAL))
                value = lang.clearEmptyChars(token.value);
            if (!valuesByType.get(token.type.ordinal()).contains(value))
                valuesByType.get(token.type.ordinal()).add(value);
        }

        for (int i = 0; i < valuesByType.size(); i++) {
            System.out.println(TokenType.values()[i]);
            for (String lexeme : valuesByType.get(i)) {
                System.out.println(lexeme);
            }
            System.out.println("---------------------------------------------------");
        }

    }

    //-----------------------------------------------------------------------------

    private Token getToken() {
        while (true) {
            char character = inputQueue.peek();
            updateLexeme(character);

            switch (character) {
                case '/':
                    // look ahead to see if it is a comment and not operator
                    if (inputQueue.peek() == '/' || inputQueue.peek() == '*')
                        return getCommentToken();
                    break;
                case '`':
                    return getIdToken(2);
                case '$':
                    return getIdToken(4);
                case '"':
                    return getStringLiteralToken();
                case '#':
                    return getDirectiveToken();
                case '.':
                    if (lang.isOperatorChar(inputQueue.peek()))
                        return getOperatorToken(true);
                case '-':
                    if (inputQueue.peek() >= '0' && inputQueue.peek() <= '9')
                        return getNumberToken();
            }

            if (lang.isIdHead(character)) return getIdToken(1);
            if (lang.isOperatorHead(character)) return getOperatorToken(false);
            if (lang.isPunctuationMark(String.valueOf(character))) return getPunctuationToken(character);
            if (character >= '0' && character <= '9') return getNumberToken();
            if (inputQueue.peek() == null) return new Token(TokenType.EOF, "eof");
        }
    }

    private Token getCommentToken() {
        int state = 1;

        while (true) {
            int nextCommentState = state;
            char character = inputQueue.peek();

            switch (state) {
                case 1:
                    if (character == '/') { // -> "//"
                        nextCommentState = 2;
                    } else if (character == '*') { // -> "/*"
                        nextCommentState = 3;
                    }
//                    else {
//                        return new Token(TokenType.ERROR, lexemeBuffer);
//                    }
                    break;
                case 2:
                    if (lang.isLineBreak(character)) { // end of "//" comment
                        nextCommentState = 5;
                    } else {
                        nextCommentState = 2;
                    }
                    break;
                case 3:
                    if (character == '*') {     // -> "...*"
                        nextCommentState = 4;
                    } else {
                        nextCommentState = 3;
                    }
                    break;
                case 4:
                    if (character == '/') {     // -> "/* ... */"
                        nextCommentState = 5;
                    } else {                    // -> "...*."
                        nextCommentState = 3;
                    }
                    break;
                case 5:
                    return new Token(TokenType.COMMENT, lexemeBuffer);
            }

            updateLexeme(character);

            //TODO: think how to tokenize last comment better
            if (inputQueue.peek() == null) {
                return new Token(TokenType.COMMENT, lexemeBuffer);
            }
            state = nextCommentState;
        }
    }

    private Token getIdToken(int state) {
        while (true) {
            int nextIdState = state;
            char character = inputQueue.peek();

            switch (state) {
                case 1:
                    if (lang.isIdChar(character)) {
                        nextIdState = 1;
                    } else nextIdState = 5;
                    break;
                case 2:
                    if (lang.isIdHead(character)) {
                        nextIdState = 3;
                    }
//                    else {
//                        return new Token(TokenType.ERROR, lexemeBuffer);
//                    }
                    break;
                case 3:
                    if (lang.isIdChar(character)) {
                        nextIdState = 3;
                    } else if (character == '`') {
                        nextIdState = 5;
                    }
//                    else {
//                        return new Token(TokenType.ERROR, lexemeBuffer);
//                    }
                    break;
                case 4:
                    if (character >= '0' && character <= '9') {
                        nextIdState = 4;
                    } else {
                        nextIdState = 5;
                    }
                    break;
                case 5:
                    if (lang.isKeyword(lexemeBuffer)) {
                        return new Token(TokenType.KEYWORD, lexemeBuffer);
                    }
                    return new Token(TokenType.IDENTIFIER, lexemeBuffer);
            }

            //TODO: think how can i break "id.id" better
            if (lang.isIdChar(character) || character == '`') {
                updateLexeme(character);
            }

            if (inputQueue.peek() == null) {
                return new Token(TokenType.IDENTIFIER, lexemeBuffer);
            }
            state = nextIdState;
        }
    }

    private Token getPunctuationToken(char character) {
        if (character == '-') {
            if (inputQueue.peek() == '>') {
                updateLexeme(character);
                return new Token(TokenType.PUNCTUATION, lexemeBuffer);
            }
        }
        return new Token(TokenType.PUNCTUATION, lexemeBuffer);
    }

    private Token getOperatorToken(boolean canContainDot) {
        while (!inputQueue.isEmpty()) {
            char character = inputQueue.peek();
            if (!(lang.isOperatorChar(character) || (character == '.' && canContainDot))) {
                break;
            }
            updateLexeme(character);
        }
        return new Token(TokenType.OPERATOR, lexemeBuffer);
    }

    private Token getStringLiteralToken() {
        char character = inputQueue.peek();

        while (character != '"' && !inputQueue.isEmpty()) {
            character = inputQueue.peek();
            if (character == '"') break;
            updateLexeme(character);
        }

        if (character == '"') {
            updateLexeme(character);

            // third " in a row
            if (inputQueue.peek() == '"')
                updateLexeme(inputQueue.peek());
                return getMultilineStringLiteral();
        }
        return new Token(TokenType.LITERAL, lexemeBuffer.replaceAll("\"", ""));
    }

    // fix
    private Token getMultilineStringLiteral(){
        char character = inputQueue.peek();

        while (character != '"' && !inputQueue.isEmpty()) {
            character = inputQueue.peek();
//            if (character == '"') break;
            updateLexeme(character);
        }

        if (character == '"') {
            updateLexeme(character);
            if (inputQueue.peek() == '"') {
                updateLexeme(inputQueue.peek());
            }
        }
        return new Token(TokenType.LITERAL, lexemeBuffer.replaceAll("\"", ""));
    }

    private Token getDirectiveToken() {

        while (!inputQueue.isEmpty()) {
            char character = inputQueue.peek();
            if (lang.isDirective(lexemeBuffer)) break;
            updateLexeme(character);
        }

        return new Token(TokenType.DIRECTIVE, lexemeBuffer);
    }

    private Token getNumberToken() {
        while (!inputQueue.isEmpty()) {
            char character = inputQueue.peek();
            if ((character < '0' || character > '9')
                    && character != '.'
                    && character != 'p' && character != 'P'
                    && character != 'e' && character != 'E'
                    && character != '+' && character != '-') break;
            updateLexeme(character);
        }

        return new Token(TokenType.NUMBER, lexemeBuffer);
    }

    //-----------------------------------------------------------------------------

    private void updateLexeme(char character) {
        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
        inputQueue.poll();
    }
}

//TODO: errors
//TODO: multiline literals
//TODO: escape characters

// прибрала пробільні символи з токенів
// прибрала "" з токенів-літералів
// виправила оператори з крапками (оператор може містити крапку тільки якщо він з неї починається)