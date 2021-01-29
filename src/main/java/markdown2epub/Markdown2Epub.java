package markdown2epub;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class Markdown2Epub {

    private static final String INDEX_PROPERTIES = "index.properties";
    
    private static final String DIRECTORY_INDEX_HTML_SPLITTED = "index.html_splitted";

    //private static final String baseDirectoryWithTrailingSlash = "/Users/ra/workspace/monkeyshed/cto-book/";

    public static void main(String[] args) throws Exception {
        var workingDirectory = Paths.get(".").toAbsolutePath().normalize();

        var indexPropertiesFile = workingDirectory.resolve(INDEX_PROPERTIES).toFile();

        try (InputStream input = new FileInputStream(indexPropertiesFile)) {

            Properties prop = new Properties();

            prop.load(new InputStreamReader(input, Charset.forName("UTF-8")));

            String title = ensureNotNullOrExplode(prop.getProperty("title"), "Ops. Property 'title' in file " + indexPropertiesFile.getAbsolutePath() + " is missing. Please add it.");

            String description = ensureNotNullOrExplode(prop.getProperty("description"), "Ops. Property 'description' in file " + indexPropertiesFile.getAbsolutePath() + " is missing. Please add it.");
            String authorFirstname = ensureNotNullOrExplode(prop.getProperty("authorFirstname"), "Ops. Property 'authorFirstname' in file " + indexPropertiesFile.getAbsolutePath() + " is missing. Please add it.");
            String authorLastname = ensureNotNullOrExplode(prop.getProperty("authorLastname"), "Ops. Property 'authorLastname' in file " + indexPropertiesFile.getAbsolutePath() + " is missing. Please add it.");

            Markdown2Epub authr = new Markdown2Epub();

            // clean target directory where all created files are stores
            var targetDirectory = workingDirectory.resolve("target");
            if (targetDirectory.toFile().exists()) {
                MoreFiles.deleteRecursively(targetDirectory, RecursiveDeleteOption.ALLOW_INSECURE);
            }
            targetDirectory.toFile().mkdirs();
            
            
            var targetDirWithSplittedFiles = targetDirectory.resolve(DIRECTORY_INDEX_HTML_SPLITTED);
            if (targetDirWithSplittedFiles.toFile().exists()) {
                MoreFiles.deleteRecursively(targetDirWithSplittedFiles, RecursiveDeleteOption.ALLOW_INSECURE);
            }
            targetDirWithSplittedFiles.toFile().mkdirs();
            
            // export as text
            // so we get a nice markdown
            authr.convertMarkdownToHtml(workingDirectory, targetDirectory);

            // let's split that one html file to multiple html files.
            // This is needed to get a nice directory structure in epub / mobi.
            authr.splitHtmlIntoSectionsAndNiceSingleHtmlFiles(targetDirectory, targetDirWithSplittedFiles);

            // Let's parse the single html files and combine them in a nicely structured epub
            EPubCreator ePubCreator = new EPubCreator();
            ePubCreator.createEbookFromSingleHtmlFiles(
                    workingDirectory,
                    targetDirWithSplittedFiles,
                    title,
                    description,
                    authorFirstname,
                    authorLastname);

            // => epub can now be converted with calibre to mobi or other formats...
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public void convertMarkdownToHtml(Path baseDirectoryWithTrailingSlash, Path targetDirectoryWithTrailingSlash) throws Exception {
        // read
        String file = baseDirectoryWithTrailingSlash.resolve("index.md").toString();

        String content = Files
                .readString(Paths.get(file))
                .replaceAll("________________", ""); // google docs creates this as pagebreak. We don't need it...
        String contentWithoutComments = Arrays.asList(content.split("\n"))
                .stream()
                .flatMap(l -> l.startsWith("//") ? Stream.empty() : Stream.of(l))
                .collect(Collectors.joining("\n"));

        // convert into html
        Parser parser = Parser
                .builder()
                .extensions(
                        // automatically convert http:// to <a href...>
                        Collections.singleton(AutolinkExtension.create()))
                .build();

        Node document = parser.parse(contentWithoutComments);

        HtmlRenderer renderer = HtmlRenderer.builder().build();

        String html = renderer.render(document);

        writeToFile(html, targetDirectoryWithTrailingSlash.resolve("index.html").toString());
    }

    public void splitHtmlIntoSectionsAndNiceSingleHtmlFiles(
            Path targetDirectory,
            Path targetDirWithSplittedFiles) throws Exception {
        String file = targetDirectory.resolve("index.html").toString();
        String content = Files.readString(Paths.get(file));

        List<String> fileSplit = Arrays.asList(content.split("\n"));

        int h1Count = 0;
        int h2Count = 0;
        String currentSectionFileName = "";
        String currentSection = "";

        for (String line : fileSplit) {
            if (line.startsWith("<h1>")) {
                h1Count++;
                if (!currentSectionFileName.isBlank()) {
                    String htmlContent = makeNiceXHtml(currentSection);
                    writeToFile(htmlContent, currentSectionFileName);

                    h2Count = 0;
                }

                String h1 = String.format("%03d", h1Count);
                String h2 = String.format("%03d", h2Count);

                currentSection = line;
                currentSectionFileName = targetDirWithSplittedFiles.resolve(
                        h1 + "_" + h2 + "_"
                        + line
                                .toLowerCase()
                                .replaceAll("<[^>]*[^\\s>][^>]*>", "")
                                .replaceAll(" ", "_")
                                .replaceAll("[^\\.a-z0-9_]", "")
                                .toLowerCase()
                        + ".xhtml").toString();

            } else if (line.startsWith("<h2>")) {
                if (!currentSectionFileName.isBlank()) {
                    String htmlContent = makeNiceXHtml(currentSection);
                    writeToFile(htmlContent, currentSectionFileName);

                    h2Count++;
                }

                String h1 = String.format("%03d", h1Count);
                String h2 = String.format("%03d", h2Count);

                currentSection = line;
                currentSectionFileName = targetDirWithSplittedFiles.resolve(
                        h1 + "_" + h2 + "_"
                        + line
                                .toLowerCase()
                                .replaceAll("<[^>]*[^\\s>][^>]*>", "")
                                .replaceAll(" ", "_")
                                .replaceAll("[^\\.a-z0-9_]", "")
                                .toLowerCase()
                        + ".xhtml").toString();

            } else {
                currentSection = currentSection + "\n" + line;
            }

        }

        // write out last section (not caught by loop):
        if (!currentSectionFileName.isBlank()) {
            String htmlContent = makeNiceXHtml(currentSection);
            writeToFile(htmlContent, currentSectionFileName);
        }

    }

    private String makeNiceXHtml(String content) {
        String xHtmlHeader
                = "<?xml version='1.0' encoding='utf-8'?>\n"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>\n"
                + "    <meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\" />\n"
                + "    <title>CTO Cookbook</title>"
                + "    <link rel=\"stylesheet\" type=\"text/css\" href=\"index.css\" /> "
                + "  </head>";

        String fullHtml = xHtmlHeader + "<body><div class=\"inner\">\n" + content + "\n</div></body></html>";

        return fullHtml;

    }

    private static void writeToFile(String content, String fileName) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(content);
        }

    }

    private static String ensureNotNullOrExplode(String value, String errorMessage) {
        if (value == null) {
            System.out.println(errorMessage);
            System.exit(-1);
        }

        return value;
    }

}
