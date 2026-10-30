package com.mtmrails.esscmd.compat;

/**
 * Added to {@code ServerPlayer} by {@code ServerPlayerMixin} before 1.20.2, where vanilla
 * reads the client's language from the settings packet but doesn't keep it.
 */
public interface ClientLanguage {
    String cmdessentials$getLanguage();
}
