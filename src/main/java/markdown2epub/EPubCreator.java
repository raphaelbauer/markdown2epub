package markdown2epub;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;

import nl.siegmann.epublib.epub.EpubWriter;

public class EPubCreator {

    public void createEbookFromSingleHtmlFiles(
            Path workingDir,
            Path targetDirWithSplittedFiles,
            String title,
            String description,
            String authorFirstname,
            String authorLastname) throws Exception {
        // Create new Book
        Book book = new Book();
        Metadata metadata = book.getMetadata();

        // Set the title
        metadata.addTitle(title);
        metadata.addDescription(description);

        // Add an Author
        metadata.addAuthor(new Author(authorFirstname, authorLastname));
        
        var cover = workingDir.resolve("cover.png");
        if (cover.toFile().exists()) {
            book.setCoverImage(getResource(workingDir.resolve("cover.png"), "cover.png"));
        } else {
            System.out.println("Cover'cover.png' not found. Please add it if you want to have a cover image.");
        }


        // read all files
        // sort
        File file = targetDirWithSplittedFiles.toFile();
        List<String> files = Arrays.asList(file.listFiles())
                .stream()
                .map(f -> f.getName())
                .sorted()
                .collect(Collectors.toList());

        int currentChapter = 0;
        TOCReference parentHeading = null;
        for (String fileName : files) {
            if (fileName.equals(".DS_Store")) {
                continue;
            }
            
            int chapter = Integer.parseInt(fileName.split("_")[0]);
            if (chapter != currentChapter) {
                currentChapter = chapter;

                String header = parseAndExtractHeader(targetDirWithSplittedFiles.resolve(fileName));
                parentHeading = book.addSection(header, getResource(targetDirWithSplittedFiles.resolve(fileName), fileName));
            } else {
                String header = parseAndExtractHeader(targetDirWithSplittedFiles.resolve(fileName));
                book.addSection(parentHeading, header, getResource(targetDirWithSplittedFiles.resolve(fileName), fileName));
            }

        }

        // Add css file
        var indexCss = workingDir.resolve("index.css");
        if (cover.toFile().exists()) {
            book.getResources().add(getResource(indexCss, "index.css"));
        } else {
            System.out.println("Css file 'index.css' not found. Please add it if you want to have a nicely formatted epub.");
        }

        EpubWriter epubWriter = new EpubWriter();

        // Write the Book as Epub
        epubWriter.write(book, new FileOutputStream(workingDir.resolve("output.epub").toFile()));
    }

    private String parseAndExtractHeader(Path file) throws Exception {
        String content = Files.readString(file);

        List<String> contentSplit = Arrays.asList(content.split("\n"));

        for (String line : contentSplit) {

            if (line.startsWith("<h1>") || line.startsWith("<h2>")) {

                return line.replaceAll("<[^>]*[^\\s>][^>]*>", "");

            }

        }

        throw new RuntimeException("Cannot find header in your file. Make sure you got an h1 or h2...");
    }

    private Resource getResource(Path path, String href) throws Exception {

        byte[] readAllBytes = Files.readAllBytes(path);

        return new Resource(readAllBytes, href);
    }
}
