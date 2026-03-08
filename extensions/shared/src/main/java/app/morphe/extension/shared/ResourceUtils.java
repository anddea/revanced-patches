package app.morphe.extension.shared;

import androidx.annotation.NonNull;

public final class ResourceUtils {
    private ResourceUtils() {
    }

    public static int getIdentifier(@NonNull String name, @NonNull ResourceType resourceType) {
        return app.morphe.extension.shared.utils.ResourceUtils.getIdentifier(
                name,
                app.morphe.extension.shared.utils.ResourceUtils.ResourceType.valueOf(resourceType.name())
        );
    }

    public static int getIdentifierOrThrow(@NonNull String name, @NonNull ResourceType resourceType) {
        int identifier = getIdentifier(name, resourceType);
        if (identifier == 0) {
            throw new IllegalArgumentException("Resource not found: " + resourceType + " " + name);
        }
        return identifier;
    }

    public static int getColorIdentifier(@NonNull String name) {
        return app.morphe.extension.shared.utils.ResourceUtils.getColorIdentifier(name);
    }

    public static int getIntegerIdentifier(@NonNull String name) {
        return app.morphe.extension.shared.utils.ResourceUtils.getIntegerIdentifier(name);
    }

    public static String getRawResource(@NonNull String name) {
        return app.morphe.extension.shared.utils.ResourceUtils.getRawResource(name);
    }

    public static int getColor(@NonNull String name) {
        return app.morphe.extension.shared.utils.ResourceUtils.getColor(name);
    }

    public static int getInteger(@NonNull String name) {
        return app.morphe.extension.shared.utils.ResourceUtils.getInteger(name);
    }
}
