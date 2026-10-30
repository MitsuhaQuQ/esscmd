package com.mtmrails.esscmd.message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * A text file for {@code /motd}, {@code /rules} or {@code /info}. Lines before the first
 * {@code #name} heading form the default chapter; each {@code #name} line starts a chapter.
 * Lines starting with {@code ##} are comments.
 */
public record TextDocument(List<String> intro, Map<String, List<String>> chapters) {
    public static TextDocument parse(String text) {
        List<String> intro = new ArrayList<>();
        Map<String, List<String>> chapters = new LinkedHashMap<>();
        List<String> current = intro;
        for (String line : text.split("\\R", -1)) {
            if (line.startsWith("##")) {
                continue;
            }
            if (line.startsWith("#")) {
                current = new ArrayList<>();
                chapters.put(line.substring(1).trim().toLowerCase(Locale.ROOT), current);
            } else {
                current.add(line);
            }
        }
        trimTrailingBlank(intro);
        chapters.values().forEach(TextDocument::trimTrailingBlank);
        return new TextDocument(List.copyOf(intro), Map.copyOf(chapters));
    }

    /** The intro for a null name, else that chapter; null if no such chapter. */
    @Nullable
    public List<String> chapter(@Nullable String name) {
        return name == null ? intro : chapters.get(name.toLowerCase(Locale.ROOT));
    }

    private static void trimTrailingBlank(List<String> lines) {
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
    }
}
