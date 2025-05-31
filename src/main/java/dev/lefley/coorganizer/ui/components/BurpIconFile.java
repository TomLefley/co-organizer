package dev.lefley.coorganizer.ui.components;

public enum BurpIconFile {
    ADD("add.svg"),
    COPY("copy.svg"),
    LOGIN("login.svg"),
    CLOSE("close.svg"),
    UP("arrow-up.svg"),
    DOWN("arrow-down.svg"),
    TICK("tick.svg"),
    WARNING("warning.svg");

    private static final String SVG_DIRECTORY_FILEPATH = "resources/Media/svg/%s";

    private final String filename;

    BurpIconFile(String filename) {
        this.filename = filename;
    }

    public String getFile() {
        return String.format(SVG_DIRECTORY_FILEPATH, filename);
    }

    public String getFilename() {
        return filename;
    }
}