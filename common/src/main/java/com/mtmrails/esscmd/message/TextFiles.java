package com.mtmrails.esscmd.message;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * {@code config/cmdessentials/<name>_<lang>.txt} for {@code motd}, {@code rules} and {@code info},
 * picked in the recipient's language like {@link Messages}. Bundled files are written out on
 * first run and never overwritten.
 */
public final class TextFiles {
    public static final String MOTD = "motd";
    public static final String RULES = "rules";
    public static final String INFO = "info";
    private static final List<String> NAMES = List.of(MOTD, RULES, INFO);
    private static final List<String> BUNDLED_LANGUAGES = List.of("en_us", "zh_cn");
    private static final Pattern FILE = Pattern.compile("(motd|rules|info)_([a-z0-9_]+)\\.txt");

    /** name -> language -> document */
    private static volatile Map<String, Map<String, TextDocument>> documents = Map.of();

    private TextFiles() {}

    @Nullable
    public static TextDocument get(ServerPlayer recipient, String name) {
        return find(name, Messages.language(recipient));
    }

    @Nullable
    public static TextDocument get(CommandSourceStack recipient, String name) {
        return find(name, Messages.language(recipient));
    }

    @Nullable
    private static TextDocument find(String name, String language) {
        Map<String, TextDocument> byLanguage = documents.getOrDefault(name, Map.of());
        for (String candidate : Messages.fallbackChain(language)) {
            TextDocument doc = byLanguage.get(candidate);
            if (doc != null) {
                return doc;
            }
        }
        return null;
    }

    public static void set(Map<String, Map<String, TextDocument>> loaded) {
        documents = Map.copyOf(loaded);
    }

    public static Map<String, Map<String, TextDocument>> read(Path dir) throws IOException {
        for (String name : NAMES) {
            for (String language : BUNDLED_LANGUAGES) {
                Path file = dir.resolve(name + "_" + language + ".txt");
                if (!Files.exists(file)) {
                    try (InputStream in = TextFiles.class.getResourceAsStream("/cmdessentials/text/" + name + "_" + language + ".txt")) {
                        if (in != null) {
                            Files.write(file, in.readAllBytes());
                        }
                    }
                }
            }
        }
        Map<String, Map<String, TextDocument>> result = new HashMap<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.txt")) {
            for (Path file : files) {
                Matcher m = FILE.matcher(file.getFileName().toString());
                if (m.matches()) {
                    String text = Files.readString(file, StandardCharsets.UTF_8);
                    result.computeIfAbsent(m.group(1), n -> new HashMap<>()).put(m.group(2), TextDocument.parse(text));
                }
            }
        }
        return result;
    }
}
