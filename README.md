# markdown2epub - markdown-to-epub creation tool

## What problem does this solve?

I like to write my ebooks as markdown file. 
But I honestly could not find a straight-forward solution to create a nice epub out of that markdown.

So I created a tiny command line application that can do that for you.

## Credits

- Paul Siegmann's awesome epublib library is used to create an epub
- markdown to html conversion is done using Atlassian's cool commonmark parser
- General coolness is provided by Google's Guava

## What is needed?

You have to have some knowledge about java, maven and how to build things (no coding skills needed).

You need the following files (naming is important):
- index.css (Some formatting)
- index.md (Your text with all chapters and sections)
- cover.png (A nice cover image)
- index.properties (Properties that specify author, title description and so on)

## How does index.properties look like?

Simple. Looks like this:

    # Note: This file should be utf-8 !!!
    title=My ebook title
    authorFirstname=Max E.
    authorLastname=Coolman
    description=The best book in the world

## How do I create a nice epub?

First of all you have to compile the source code of this repository and run

    mvn clean package

This will create a jar file called amardown2epub-1.0-SNAPSHOT-jar-with-dependencies.jar  in folder target.

**Note:** This could be done more user-friendly, but I don't have much time right now. Feel free to improve that :)

Then cd into the directory where your ebook markdown lives. 
You can check out the example directory and play around with it.

Then run

    cd example
    java -jar ../target/mardown2epub-1.0-SNAPSHOT-jar-with-dependencies.jar 

This will create a file called output.epub in the same folder. That's it.
You are don.

## Cool things

- You can use // at the beginning of a file in markdown to mark it as a comment. The line will be removed from the final epub.
- h1, h2 headings will be nicely splitted so that your epub reader can create a nice toc with proper navigation.

## Bad things

- Not a single test. Yea. I just wanted to release and document this. So no tests, but that might change :)

## Where to go from here

I personally use Calibre to improve the epub and also create a mobi for my Kindle. Calibre got a nice
command linetool called ebook-conver that I use:

    # Add nice inline toc
    /Applications/calibre.app/Contents/MacOS/ebook-convert output.epub output_final.epub --epub-inline-toc

    # convert epub into mobi for kindle
    /Applications/calibre.app/Contents/MacOS/ebook-convert output.epub output_final.mobi
