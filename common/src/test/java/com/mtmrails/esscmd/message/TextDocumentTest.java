package com.mtmrails.esscmd.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextDocumentTest {
    @Test
    void splitsIntroAndChaptersAndSkipsComments() {
        TextDocument doc = TextDocument.parse(String.join("\n",
                "## comment",
                "Intro line",
                "",
                "#Commands",
                "/home",
                "## another comment",
                "/spawn",
                "",
                "#empty",
                ""));
        assertEquals(List.of("Intro line"), doc.chapter(null));
        assertEquals(List.of("/home", "/spawn"), doc.chapter("commands"));
        assertEquals(List.of("/home", "/spawn"), doc.chapter("COMMANDS"));
        assertEquals(List.of(), doc.chapter("empty"));
        assertNull(doc.chapter("missing"));
    }

    @Test
    void handlesWindowsLineEndings() {
        TextDocument doc = TextDocument.parse("a\r\nb\r\n#c\r\nd\r\n");
        assertEquals(List.of("a", "b"), doc.chapter(null));
        assertEquals(List.of("d"), doc.chapter("c"));
    }
}
