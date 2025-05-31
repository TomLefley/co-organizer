package dev.lefley.coorganizer.ui.components;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;

public class BurpIcon extends FlatSVGIcon {
    private static final Color PRIMARY = Color.BLACK;

    private final BurpColor normalColour;
    private final BurpColor hoverColour;

    private final ColourMapping primaryColourMapping;
    private final FlatSVGIcon.ColorFilter colorFilter;

    public BurpIcon(BurpIconFile iconFile, float scale, BurpColor normalColour, BurpColor hoverColour) {
        super(iconFile.getFile(), scale);

        this.normalColour = normalColour;
        this.hoverColour = hoverColour;

        this.primaryColourMapping = new ColourMapping(PRIMARY);

        this.colorFilter = new FlatSVGIcon.ColorFilter();

        setColorFilter(colorFilter);
        setNormal();
    }

    public void setNormal() {
        primaryColourMapping.setToColor(normalColour);
    }

    public void setHover() {
        primaryColourMapping.setToColor(hoverColour);
    }

    public static BurpIcon of(BurpIconFile iconFile) {
        return new Builder(iconFile).build();
    }
    

    public class ColourMapping {
        private final Color fromColor;

        private ColourMapping(Color fromColor) {
            this.fromColor = fromColor;
        }

        public void setToColor(BurpColor toColor) {
            colorFilter.remove(fromColor);

            if (toColor != null) {
                colorFilter.add(fromColor, toColor.getColor());
            }
        }
    }

    public static class Builder {
        private final BurpIconFile iconFile;

        private float scale = 1.0f;
        private BurpColor normalColour = BurpColor.ACTION_NORMAL;
        private BurpColor hoverColour = BurpColor.ACTION_HOVER;

        public Builder(BurpIconFile iconFile) {
            this.iconFile = iconFile;
        }

        public Builder withScale(float scale) {
            this.scale = scale;
            return this;
        }

        public Builder withNormalColor(BurpColor normalColour) {
            this.normalColour = normalColour;
            return this;
        }

        public Builder withHoverColor(BurpColor hoverColour) {
            this.hoverColour = hoverColour;
            return this;
        }

        public Builder fontSized() {
            this.scale = 0.6f;
            return this;
        }

        public BurpIcon build() {
            return new BurpIcon(iconFile, scale, normalColour, hoverColour);
        }
    }
}