package com.mtmrails.esscmd.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.20.5 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
*///?} else {
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
//?}

/**
 * Written books (title, author, pages) and book and quills: NBT before 1.20.5, the
 * {@code written_book_content} and {@code writable_book_content} components after.
 */
public final class Books {
    public static final int TITLE_MAX_LENGTH = 32;
    private static final int PAGE_MAX_LENGTH = 1024;
    private static final int MAX_PAGES = 100;

    private Books() {}

    public static boolean isWritten(ItemStack stack) {
        return stack.is(Items.WRITTEN_BOOK);
    }

    public static boolean isWritable(ItemStack stack) {
        return stack.is(Items.WRITABLE_BOOK);
    }

    /** The author of a written book, or an empty string. */
    public static String author(ItemStack stack) {
        //? if >=1.20.5 {
        /*WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        return content == null ? "" : content.author();
        *///?} else {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString("author");
        //?}
    }

    public static void setAuthor(ItemStack stack, String author) {
        //? if >=1.20.5 {
        /*WrittenBookContent c = stack.getOrDefault(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY);
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(c.title(), author, c.generation(), c.pages(), c.resolved()));
        *///?} else {
        stack.getOrCreateTag().putString("author", author);
        //?}
    }

    /** {@code title} must be at most {@link #TITLE_MAX_LENGTH} characters. */
    public static void setTitle(ItemStack stack, String title) {
        //? if >=1.20.5 {
        /*WrittenBookContent c = stack.getOrDefault(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY);
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(title), c.author(), c.generation(), c.pages(), c.resolved()));
        *///?} else {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", title);
        tag.remove("filtered_title");
        //?}
    }

    /** A written book turned back into a book and quill; formatting is reduced to plain text. */
    public static ItemStack unseal(ItemStack written) {
        List<String> pages = new ArrayList<>();
        //? if >=1.20.5 {
        /*WrittenBookContent c = written.getOrDefault(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY);
        c.pages().forEach(page -> pages.add(page.raw().getString()));
        ItemStack book = written.transmuteCopy(Items.WRITABLE_BOOK);
        book.remove(DataComponents.WRITTEN_BOOK_CONTENT);
        book.set(DataComponents.WRITABLE_BOOK_CONTENT,
                new WritableBookContent(fit(pages).stream().map(Filterable::passThrough).toList()));
        return book;
        *///?} else {
        CompoundTag tag = written.hasTag() ? written.getTag().copy() : new CompoundTag();
        ListTag json = tag.getList("pages", Tag.TAG_STRING);
        for (int i = 0; i < json.size(); i++) {
            Component page = Component.Serializer.fromJson(json.getString(i));
            pages.add(page == null ? json.getString(i) : page.getString());
        }
        for (String key : List.of("title", "filtered_title", "author", "generation", "resolved", "filtered_pages")) {
            tag.remove(key);
        }
        ListTag plain = new ListTag();
        fit(pages).forEach(page -> plain.add(StringTag.valueOf(page)));
        tag.put("pages", plain);
        ItemStack book = new ItemStack(Items.WRITABLE_BOOK, written.getCount());
        book.setTag(tag);
        return book;
        //?}
    }

    /** A book and quill signed as a written book, like the vanilla "Sign" button. */
    public static ItemStack seal(ItemStack writable, String title, String author) {
        List<String> pages = new ArrayList<>();
        //? if >=1.20.5 {
        /*WritableBookContent c = writable.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY);
        c.pages().forEach(page -> pages.add(page.raw()));
        ItemStack book = writable.transmuteCopy(Items.WRITTEN_BOOK);
        book.remove(DataComponents.WRITABLE_BOOK_CONTENT);
        List<Filterable<Component>> text = pages.stream()
                .map(page -> Filterable.<Component>passThrough(Texts.literal(page)))
                .toList();
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(title), author, 0, text, true));
        return book;
        *///?} else {
        CompoundTag tag = writable.hasTag() ? writable.getTag().copy() : new CompoundTag();
        ListTag plain = tag.getList("pages", Tag.TAG_STRING);
        for (int i = 0; i < plain.size(); i++) {
            pages.add(plain.getString(i));
        }
        tag.remove("filtered_pages");
        ListTag json = new ListTag();
        pages.forEach(page -> json.add(StringTag.valueOf(Component.Serializer.toJson(Texts.literal(page)))));
        tag.put("pages", json);
        tag.putString("title", title);
        tag.putString("author", author);
        tag.putBoolean("resolved", true);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK, writable.getCount());
        book.setTag(tag);
        return book;
        //?}
    }

    /** At most 100 pages of at most 1024 characters, the limits of a book and quill. */
    private static List<String> fit(List<String> pages) {
        return pages.stream()
                .limit(MAX_PAGES)
                .map(page -> page.length() > PAGE_MAX_LENGTH ? page.substring(0, PAGE_MAX_LENGTH) : page)
                .toList();
    }
}
