package com.auca.interfaces;

/** Anything that can be identified, named and checked for activity. */
public interface Identifiable {
    String getId();

    String getDisplayName();

    boolean isActive();
}
