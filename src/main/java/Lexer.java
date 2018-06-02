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
        for (TokenType type : TokenType.values()) {
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
                    return getCommentToken(1);
                case '`':
                    return getIdToken(2);
                case '$':
                    return getIdToken(4);
                case '"':
                    return getStringLiteralToken();
                case '#':
                    return getDirectiveToken();
            }

            if (lang.isIdHead(character)) return getIdToken(1);
            if (lang.isOperatorHead(character)) return getOperatorToken();
            if (lang.isPunctuationMark(String.valueOf(character))) return getPunctuationToken(character);
            if (character >= '0' && character <= '9') return getNumberToken();
            if (inputQueue.peek() == null) return new Token(TokenType.EOF, "eof");
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
        while (!inputQueue.isEmpty()) {
            char character = inputQueue.peek();
            if (!lang.isOperatorHead(character)) {
                break;
            }
            inputQueue.poll();
            lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
        }
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
        char character;

        while (!lang.isDirective(lexemeBuffer) && !inputQueue.isEmpty()) {
            character = inputQueue.peek();
            if (lang.isDirective(lexemeBuffer)) break;
            lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
            inputQueue.poll();
        }

        return new Token(TokenType.DIRECTIVE, lexemeBuffer);
    }

    private Token getNumberToken() {
        char character;

        while (!inputQueue.isEmpty()) {
            character = inputQueue.peek();
            if (character < '0' || character > '9') break;
            lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
            inputQueue.poll();
        }

        return new Token(TokenType.NUMBER, lexemeBuffer);

    }

    //-----------------------------------------------------------------------------

    private void updateLexeme(char character) {
        lexemeBuffer = lexemeBuffer.concat(String.valueOf(inputQueue.peek()));
        inputQueue.poll();
    }
}
