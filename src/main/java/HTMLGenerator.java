import j2html.tags.ContainerTag;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

import static j2html.TagCreator.*;

public class HTMLGenerator {

    public static void generate(ArrayList<Token> tokens) {

        // render html string using j2html library
        String html = html(
                head(
                        title("Title"),
                        link().withRel("stylesheet").withHref("main.css")
                ),
                body(
                        div(
                                tokens.stream().map(token ->
                                        p(token.value).withClass(token.type.name())
                                ).toArray(ContainerTag[]::new)
                        )
                )
        ).render();

        // write html string to output file
        FileWriter fileWriter;
        BufferedWriter bufferedWriter;
        try {
            fileWriter = new FileWriter("output/output.html");
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(html);
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
