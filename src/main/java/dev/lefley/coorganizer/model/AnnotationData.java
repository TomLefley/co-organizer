package dev.lefley.coorganizer.model;

public class AnnotationData {
    public final String notes;
    public final String highlightColor;
    public final boolean hasNotes;
    public final boolean hasHighlightColor;
    
    public AnnotationData(String notes, String highlightColor, boolean hasNotes, boolean hasHighlightColor) {
        this.notes = notes;
        this.highlightColor = highlightColor;
        this.hasNotes = hasNotes;
        this.hasHighlightColor = hasHighlightColor;
    }
}