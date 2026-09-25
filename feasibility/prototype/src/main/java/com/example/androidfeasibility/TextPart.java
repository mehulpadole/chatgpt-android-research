package com.example.androidfeasibility;

public final class TextPart implements ContentPart {
    public final String text;

    public TextPart(String text) { this.text = text == null ? "" : text; }

    @Override public String type() { return "text"; }
    @Override public ContentPart copy() { return new TextPart(text); }
}
