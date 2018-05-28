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
            if(input[i] != 0) inputQueue.add(input[i]);
        }


        while (true) {
            tokens.add(getToken("", 0));
            if(inputQueue.isEmpty()) break;
        }

        printSequenceOfTokens();
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
        int nextState = state;
        char character = inputQueue.peek();
        switch (state) {
            case 0:
                if (character == '/') {
                    inputQueue.poll();
                    lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
                    return getCommentToken(lexemeBuffer, 1);
                }
                break;
        }
        if (!lang.isLineBreak(character)) {
            lexemeBuffer = lexemeBuffer.concat(String.valueOf(inputQueue.peek()));
        }

        inputQueue.poll();
        if(inputQueue.peek() == null) {
            return new Token(TokenType.EOF, "eof");
        }
        return getToken(lexemeBuffer,  nextState);
    }

//    Token getKeyword() {
//        Token keywordToken = new Token();
//        keywordToken.type = TokenType.KEYWORD;
//        //TODO: ????
//
//        return keywordToken;
//    }

    Token getCommentToken(String lexemeBuffer, int state) {
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
                if (lang.isLineBreak(character)){
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

        if (!lang.isLineBreak(character)) {
            lexemeBuffer = lexemeBuffer.concat(String.valueOf(character));
        }
        inputQueue.poll();
        if(inputQueue.peek() == null) {
            return new Token(TokenType.COMMENT, lexemeBuffer);
        }
        return getCommentToken(lexemeBuffer, nextCommentState);
    }

    void printSequenceOfTokens(){
        for (Token token : tokens){
            System.out.println("--------------------------------------------------");
            System.out.println(token.type);
            System.out.println(token.value);
        }
    }
}
