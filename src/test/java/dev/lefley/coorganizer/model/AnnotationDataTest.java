package dev.lefley.coorganizer.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnnotationData")
class AnnotationDataTest {

    @Test
    @DisplayName("should create instance with all annotation data")
    void shouldCreateInstanceWithAllAnnotationData() {
        // Given
        String notes = "These are test notes";
        String highlightColor = "RED";
        boolean hasNotes = true;
        boolean hasHighlightColor = true;

        // When
        AnnotationData annotationData = new AnnotationData(notes, highlightColor, hasNotes, hasHighlightColor);

        // Then
        assertThat(annotationData.notes).isEqualTo(notes);
        assertThat(annotationData.highlightColor).isEqualTo(highlightColor);
        assertThat(annotationData.hasNotes).isTrue();
        assertThat(annotationData.hasHighlightColor).isTrue();
    }

    @Test
    @DisplayName("should handle empty annotations")
    void shouldHandleEmptyAnnotations() {
        // Given
        String notes = "";
        String highlightColor = "";
        boolean hasNotes = false;
        boolean hasHighlightColor = false;

        // When
        AnnotationData annotationData = new AnnotationData(notes, highlightColor, hasNotes, hasHighlightColor);

        // Then
        assertThat(annotationData.notes).isEmpty();
        assertThat(annotationData.highlightColor).isEmpty();
        assertThat(annotationData.hasNotes).isFalse();
        assertThat(annotationData.hasHighlightColor).isFalse();
    }

    @Test
    @DisplayName("should handle null values")
    void shouldHandleNullValues() {
        // When
        AnnotationData annotationData = new AnnotationData(null, null, false, false);

        // Then
        assertThat(annotationData.notes).isNull();
        assertThat(annotationData.highlightColor).isNull();
        assertThat(annotationData.hasNotes).isFalse();
        assertThat(annotationData.hasHighlightColor).isFalse();
    }

    @Test
    @DisplayName("should handle mixed annotation states")
    void shouldHandleMixedAnnotationStates() {
        // Given - has notes but no highlight color
        String notes = "Important finding";
        String highlightColor = "";
        boolean hasNotes = true;
        boolean hasHighlightColor = false;

        // When
        AnnotationData annotationData = new AnnotationData(notes, highlightColor, hasNotes, hasHighlightColor);

        // Then
        assertThat(annotationData.notes).isEqualTo(notes);
        assertThat(annotationData.highlightColor).isEmpty();
        assertThat(annotationData.hasNotes).isTrue();
        assertThat(annotationData.hasHighlightColor).isFalse();
    }
}