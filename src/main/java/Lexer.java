import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

class Lexer {
    private Queue<Character> inputQueue;
    private SwiftLang lang;

    private ArrayList<Token> tokens;

    Lexer(String filename) {
        lang = new SwiftLang("lang/keywords.txt", "lang/punctuation.txt");
        tokens = new ArrayList<Token>();

        String inputString = readFileContents(filename);
        inputQueue = new ArrayBlockingQueue<Character>(inputString.length());

        char[] input = inputString.toCharArray();
        for (int i = 0; i < input.length; i++) {
            if (input[i] != 0) inputQueue.add(input[i]);
        }

        while (!inputQueue.isEmpty()) {
            tokens.add(getToken("", 0));
        }

        printSequenceOfTokens();
        System.out.println(lang.isKeyword("import"));
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

    Token getToken(String lexemeBuffer, int state) {
        while (true) {
            int nextState = state;
            char character = inputQueue.peek();
            switch (state) {
                case 0:
                    if (character == '/') {
                        inputQueue.poll();
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        return getCommentToken(lexemeBuffer, 1);
                    }
                    if (character == '`') {
                        inputQueue.poll();
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        return getIdToken(lexemeBuffer, 2);
                    }
                    if (character == '$') {
                        inputQueue.poll();
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        return getIdToken(lexemeBuffer, 4);
                    }
                    if (lang.isIdHead(character)) {
                        inputQueue.poll();
                        lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                        return getIdToken(lexemeBuffer, 1);
                    }
                    break;
            }
            if (!lang.isLineBreak(character)) {
                lexemeBuffer = lexemeBuffer.concat(String.valueOf(inputQueue.peek()));
            }

            inputQueue.poll();
            if (inputQueue.peek() == null) {
                return new Token(TokenType.EOF, "eof");
            }
            state = nextState;
        }
    }

//    Token getKeyword() {
//        Token keywordToken = new Token();
//        keywordToken.type = TokenType.KEYWORD;
//        //TODO: ????
//
//        return keywordToken;
//    }

    Token getCommentToken(String lexemeBuffer, int state) {
        while (true) {
            int nextCommentState = state;
            char character = inputQueue.peek();
            switch (state) {
                case 1:
                    if (character == '/') {
                        nextCommentState = 2;
                    } else if (character == '*') {
                        nextCommentState = 3;
                    }
                    break;
                case 2:
                    if (lang.isLineBreak(character)) {
                        nextCommentState = 5;
                    } else {
                        nextCommentState = 2;
                    }
                    break;
                case 3:
                    if (character == '*') {
                        nextCommentState = 4;
                    } else {
                        nextCommentState = 3;
                    }
                    break;
                case 4:
                    if (character == '/') {
                        nextCommentState = 5;
                    } else {
                        return new Token(TokenType.ERROR, lexemeBuffer);
                    }
                    break;
                case 5:
                    return new Token(TokenType.COMMENT, lexemeBuffer);
            }

            if (inputQueue.poll() != 10 || nextCommentState == 3) {
                lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
            }
            if (inputQueue.peek() == null) {
                return new Token(TokenType.COMMENT, lexemeBuffer);
            }
            state = nextCommentState;
        }
    }

    Token getIdToken(String lexemeBuffer, int state) {
        while (true) {
            int nextIdState = state;
            char character = inputQueue.poll();
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

            lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
            if (inputQueue.peek() == null) {
                return new Token(TokenType.IDENTIFIER, lexemeBuffer);
            }
            state = nextIdState;
        }

    }

    void printSequenceOfTokens() {
        for (Token token : tokens) {
            System.out.println("--------------------------------------------------");
            System.out.println(token.type);
            System.out.println(token.value);
        }
    }
}
