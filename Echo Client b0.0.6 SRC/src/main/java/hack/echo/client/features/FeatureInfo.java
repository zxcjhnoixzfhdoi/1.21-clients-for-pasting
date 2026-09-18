package hack.echo.client.features;

public record FeatureInfo(
        CharSequence name,
        CharSequence description,
        Category category,
        int key,
        boolean visible
) {
    public FeatureInfo(CharSequence name, CharSequence description, Category category, int key, boolean visible) {
        this.name = name != null ? name : "";
        this.description = description != null ? description : "";
        this.category = category != null ? category : Category.INTERNALS;
        this.key = key;
        this.visible = visible;
    }

    public FeatureInfo(CharSequence name, CharSequence description, Category category, int key) {
        this(name, description, category, key, true);
    }

    public FeatureInfo(CharSequence name, CharSequence description, Category category, boolean visible) {
        this(name, description, category, -1, visible);
    }

    public FeatureInfo(CharSequence name, CharSequence description, Category category) {
        this(name, description, category, -1, true);
    }
}
