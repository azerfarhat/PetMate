/// Image-related helpers.
///
/// Provides placeholder and asset path resolution used across features.
/// Concrete image picking / cropping helpers will be added when the image
/// pipeline is finalized (e.g. via `image_picker`).
abstract final class ImageUtils {
  ImageUtils._();

  /// Placeholder image used when a pet has no photo yet.
  static const String petPlaceholder =
      'assets/images/placeholders/pet_placeholder.png';

  /// Placeholder image used when a user has no avatar yet.
  static const String userPlaceholder =
      'assets/images/placeholders/user_placeholder.png';

  /// Placeholder image used for activities.
  static const String activityPlaceholder =
      'assets/images/placeholders/activity_placeholder.png';

  /// Resolves an asset path, falling back to [fallback] when [path] is null
  /// or empty.
  static String resolve(String? path, {required String fallback}) {
    if (path == null || path.isEmpty) {
      return fallback;
    }
    return path;
  }
}
