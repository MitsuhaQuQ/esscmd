package com.mtmrails.esscmd.permission;

import java.util.function.IntSupplier;

/**
 * A numeric per-player option, such as the home limit. Permission plugins can set it (LuckPerms
 * meta on Fabric, an integer node on Forge); otherwise {@code fallback} applies.
 */
public record IntOption(String name, IntSupplier fallback) {}
