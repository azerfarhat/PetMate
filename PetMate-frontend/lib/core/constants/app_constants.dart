/// Application-wide constants for PawMate.
///
/// Reusable non-API constants that are shared across layers and features.
abstract final class AppConstants {
  AppConstants._();

  /// Application display name.
  static const String appName = 'PawMate';

  /// Maximum number of pets a user can create.
  static const int maxPetsPerUser = 3;

  /// Minimum age required to use the application.
  static const int minimumAge = 18;

  /// Default search radius in kilometers.
  static const double defaultSearchRadiusKm = 10;

  /// Supported search radius presets (km).
  static const List<double> searchRadiusOptions = [2, 5, 10];

  /// Timeout (seconds) for the connectivity check used by [NetworkInfo].
  static const int connectivityTimeoutSeconds = 5;
}
