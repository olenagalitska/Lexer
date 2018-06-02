import j2html.tags.ContainerTag;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

import static j2html.TagCreator.*;


public class HTMLGenerator {

    public static void generate(ArrayList<Token> tokens) {
        String html = html(
                head(
                        title("Title"),
                        link().withRel("stylesheet").withHref("main.css")
                ),
                body(
                        div(attrs("#tokens"),
                                tokens.stream().map(token ->
                                        p(token.value).withClass(token.type.name())
                                ).toArray(ContainerTag[]::new)
                        )
                )
        ).render();

        FileWriter fWriter;
        BufferedWriter writer;
        try {
            fWriter = new FileWriter("output/output.html");
            writer = new BufferedWriter(fWriter);
            writer.write(html);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
