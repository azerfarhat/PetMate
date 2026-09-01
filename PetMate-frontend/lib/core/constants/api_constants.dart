/// API configuration constants for PawMate.
///
/// Centralize API base URL, endpoints and header keys here so that feature
/// datasources do not duplicate configuration. Values are environment-aware
/// via dart-define at build time where useful.
abstract final class ApiConstants {
  ApiConstants._();

  /// Base URL of the PawMate backend.
  ///
  /// Override at build time with:
  /// `flutter run --dart-define=API_BASE_URL=https://api.example.com`
  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'https://api.pawmate.dev',
  );

  /// Default request timeout.
  static const Duration timeout = Duration(seconds: 15);

  // Authorisation header keys.
  static const String authorizationHeader = 'Authorization';
  static const String bearerPrefix = 'Bearer';
  static const String contentTypeHeader = 'Content-Type';
  static const String jsonContentType = 'application/json';

  // Storage keys.
  static const String authTokenKey = 'auth_token';
  static const String refreshTokenKey = 'refresh_token';
  static const String currentUserIdKey = 'current_user_id';
  static const String currentUserKey = 'current_user';
  static const String onboardingCompletedKey = 'onboarding_completed';

  // Endpoints. Feature-specific datasources build upon these roots.
  static const String authBasePath = '/auth';
  static const String usersBasePath = '/users';
  static const String petsBasePath = '/pets';
  static const String discoveryBasePath = '/discovery';
  static const String matchesBasePath = '/matches';
  static const String favoritesBasePath = '/favorites';
  static const String activitiesBasePath = '/activities';
  static const String conversationsBasePath = '/conversations';
  static const String messagesBasePath = '/messages';
  static const String notificationsBasePath = '/notifications';

  static const String loginEndpoint = '$authBasePath/login';
  static const String registerEndpoint = '$authBasePath/register';
  static const String logoutEndpoint = '$authBasePath/logout';
  static const String refreshTokenEndpoint = '$authBasePath/refresh';
}
