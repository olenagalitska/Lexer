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
            Token token = getToken(0);
            tokens.add(token);
        }
    }

    void printSequenceOfTokens() {
        for (Token token : tokens) {
            System.out.println("--------------------------------------------------");
            System.out.println(token.type + " :" + token.value);
        }
    }

    void generateHTML() {
        HTMLGenerator.generate(tokens);
    }

    //-----------------------------------------------------------------------------

    private Token getToken(int state) {
        while (true) {
            int nextState = state;
            char character = inputQueue.peek();
            switch (state) {
                case 0:
                    if (character == '/') {
                        inputQueue.poll();
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        return getCommentToken(1);
                    }
                    if (character == '`') {
                        inputQueue.poll();
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        return getIdToken(2);
                    }
                    if (character == '$') {
                        inputQueue.poll();
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        return getIdToken(4);
                    }
                    if (character == '"') {
                        inputQueue.poll();
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        return getStringLiteralToken();
                    }
                    if (character == '#') {
                        inputQueue.poll();
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        return getDirectiveToken();
                    }
                    if (lang.isIdHead(character)) {
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        inputQueue.poll();
                        return getIdToken(1);
                    }


                    // TODO: NOT LIKE THAT
                    // TODO: do i need 0 state at all?
                    // TODO: CODE REUSE ????
                    if (lang.isOperatorHead(character)) {
                        inputQueue.poll();
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        return getOperatorToken();
                    }
                    if (lang.isPunctuationMark(String.valueOf(character))) {
                        inputQueue.poll();
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        return getPunctuationToken(character);
                    }
//                    if (lang.isPunctuationMark(String.valueOf(character))) {
//                        if (character == '-') {
//                            nextState = 1;
//                        } else {
//                            lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
//                            inputQueue.poll();
//                            return new Token(TokenType.PUNCTUATION, lexemeBuffer);
//                        }
//                    }
                    break;
                case 1:
                    if (character == '>') {
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        inputQueue.poll();
                        return new Token(TokenType.PUNCTUATION, lexemeBuffer);
                    }
                    break;
            }
//            if (!lang.isLineBreak(character)) {
            lexemeBuffer = lexemeBuffer.concat(String.valueOf(inputQueue.peek()));
//            }

            inputQueue.poll();
            if (inputQueue.peek() == null) {
                return new Token(TokenType.EOF, "eof");
            }
            state = nextState;
        }
    }

    private Token getCommentToken(int state) {

        while (true) {
            int nextCommentState = state;
            char character = inputQueue.peek();

            switch (state) {
                case 1:
                    if (character == '/') { // -> "//"
                        nextCommentState = 2;
                    } else if (character == '*') { // -> "/*"
                        nextCommentState = 3;
                    } else {
                        return new Token(TokenType.ERROR, lexemeBuffer);
                    }
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

            inputQueue.poll();
            lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));

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
                    } else {
                        return new Token(TokenType.ERROR, lexemeBuffer);
                    }
                    break;
                case 3:
                    if (lang.isIdChar(character)) {
                        nextIdState = 3;
                    } else if (character == '`') {
                        nextIdState = 5;
                    } else {
                        return new Token(TokenType.ERROR, lexemeBuffer);
                    }
                    break;
                case 4:
                    if (character >= 48 && character <= 57) {
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

            if (lang.isIdChar(character)) {
                lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                inputQueue.poll();
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
                return new Token(TokenType.PUNCTUATION, "->");
            }
        }
        return new Token(TokenType.PUNCTUATION, String.valueOf(character));
    }

    private Token getOperatorToken() {
        return new Token(TokenType.OPERATOR, lexemeBuffer);
    }

    private Token getStringLiteralToken() {
        char character = inputQueue.peek();
        
        while (character != '"' && !inputQueue.isEmpty()) {
            character = inputQueue.peek();
            if (character == '"') break;
            lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
            inputQueue.poll();
        }

        if (character == '"') {
            lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
            inputQueue.poll();
        }
        return new Token(TokenType.LITERAL, lexemeBuffer);
    }

    private Token getDirectiveToken() {
        char character = inputQueue.peek();

        while (!lang.isDirective(lexemeBuffer) && !inputQueue.isEmpty()) {
            character = inputQueue.peek();
            if (lang.isDirective(lexemeBuffer)) break;
            lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
            inputQueue.poll();
        }

        return new Token(TokenType.DIRECTIVE, lexemeBuffer);
    }


}
