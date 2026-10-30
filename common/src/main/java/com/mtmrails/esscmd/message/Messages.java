package com.mtmrails.esscmd.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.compat.Texts;
import com.mtmrails.esscmd.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * User-facing text from {@code config/cmdessentials/messages_<lang>.json}, in the recipient's
 * client language when a file for it exists. Lookup order: the player's language, the
 * configured {@code language}, {@code en_us}, the key itself. Templates use {@code {0}},
 * {@code {1}}, ... for arguments.
 *
 * <p>Every method takes the recipient, never a bare language string, so a message can't be
 * rendered without deciding whose language it is in.
 */
public final class Messages {
    /** Languages shipped in the jar under {@code /cmdessentials/lang/}. */
    private static final List<String> BUNDLED = List.of("en_us", "zh_cn");
    private static final String FALLBACK = "en_us";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>() {}.getType();
    private static final Pattern ARG = Pattern.compile("\\{(\\d+)}");
    private static final Pattern FILE = Pattern.compile("messages_([a-z0-9_]+)\\.json");

    private static volatile Map<String, Map<String, String>> tables = Map.of();

    private Messages() {}

    // Normal feedback: gray text, gold arguments. Errors: red text, gold arguments.

    public static MutableComponent info(ServerPlayer recipient, String key, Object... args) {
        return render(language(recipient), key, ChatFormatting.GRAY, args);
    }

    public static MutableComponent info(CommandSourceStack recipient, String key, Object... args) {
        return render(language(recipient), key, ChatFormatting.GRAY, args);
    }

    public static MutableComponent error(ServerPlayer recipient, String key, Object... args) {
        return render(language(recipient), key, ChatFormatting.RED, args);
    }

    public static MutableComponent error(CommandSourceStack recipient, String key, Object... args) {
        return render(language(recipient), key, ChatFormatting.RED, args);
    }

    /** The bare template text, for button labels and the like. */
    public static String raw(ServerPlayer recipient, String key) {
        return lookup(language(recipient), key);
    }

    public static String raw(CommandSourceStack recipient, String key) {
        return lookup(language(recipient), key);
    }

    static String language(ServerPlayer player) {
        if (!ConfigManager.get().perPlayerLanguage) {
            return ConfigManager.get().language;
        }
        return Players.language(player).toLowerCase(Locale.ROOT);
    }

    static String language(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player ? language(player) : ConfigManager.get().language;
    }

    /** The language itself, then the configured default, then {@code en_us}. */
    static List<String> fallbackChain(String language) {
        return List.of(language, ConfigManager.get().language, FALLBACK);
    }

    private static String lookup(String language, String key) {
        Map<String, Map<String, String>> all = tables;
        for (String candidate : fallbackChain(language)) {
            Map<String, String> table = all.get(candidate);
            if (table != null) {
                String text = table.get(key);
                if (text != null) {
                    return text;
                }
            }
        }
        return key;
    }

    private static MutableComponent render(String language, String key, ChatFormatting color, Object... args) {
        String template = lookup(language, key);
        MutableComponent out = Texts.empty().withStyle(color);
        Matcher m = ARG.matcher(template);
        int last = 0;
        while (m.find()) {
            out.append(Texts.literal(template.substring(last, m.start())));
            int index = Integer.parseInt(m.group(1));
            if (index < args.length) {
                Object arg = args[index];
                out.append(arg instanceof Component c ? c.copy() : Texts.literal(String.valueOf(arg)).withStyle(ChatFormatting.GOLD));
            } else {
                out.append(Texts.literal(m.group()));
            }
            last = m.end();
        }
        out.append(Texts.literal(template.substring(last)));

        String prefix = lookup(language, "prefix");
        if (prefix.isEmpty() || prefix.equals("prefix")) {
            return out;
        }
        return Texts.literal(prefix).withStyle(ChatFormatting.GOLD).append(out);
    }

    public static void set(Map<String, Map<String, String>> loaded) {
        tables = Map.copyOf(loaded);
    }

    /**
     * Reads every {@code messages_<lang>.json} in {@code dir}. Bundled languages are written out
     * on first run, and keys added in newer versions are merged into existing files.
     */
    public static Map<String, Map<String, String>> read(Path dir) throws IOException {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (String language : BUNDLED) {
            result.put(language, readMerged(dir.resolve("messages_" + language + ".json"), bundled(language)));
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "messages_*.json")) {
            for (Path file : files) {
                Matcher m = FILE.matcher(file.getFileName().toString());
                if (m.matches() && !result.containsKey(m.group(1))) {
                    result.put(m.group(1), readFile(file));
                }
            }
        }
        return result;
    }

    private static Map<String, String> readMerged(Path file, Map<String, String> defaults) throws IOException {
        Map<String, String> merged = new LinkedHashMap<>(defaults);
        boolean write = true;
        if (Files.exists(file)) {
            Map<String, String> user = readFile(file);
            merged.putAll(user);
            write = !user.keySet().containsAll(defaults.keySet());
        }
        if (write) {
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(merged, writer);
            }
        }
        return merged;
    }

    private static Map<String, String> readFile(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, String> map = GSON.fromJson(reader, MAP_TYPE);
            return map == null ? Map.of() : map;
        } catch (RuntimeException e) {
            throw new IOException(file.getFileName() + " is not valid JSON: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> bundled(String language) throws IOException {
        try (InputStream in = Messages.class.getResourceAsStream("/cmdessentials/lang/" + language + ".json")) {
            if (in == null) {
                return Map.of();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Map<String, String> map = GSON.fromJson(reader, MAP_TYPE);
                return map == null ? Map.of() : map;
            }
        }
    }
}
