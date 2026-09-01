/// Central registry of route names for the PawMate application.
///
/// Keep navigation configuration centralized here instead of scattering
/// string literals throughout the application.
abstract final class RouteNames {
  RouteNames._();

  // Onboarding.
  static const String onboarding = '/onboarding';

  // Authentication.
  static const String login = '/login';
  static const String register = '/register';
  static const String forgotPassword = '/forgot-password';

  // User profile.
  static const String userProfile = '/user-profile';
  static const String userProfileEdit = '/user-profile/edit';

  // Pets.
  static const String pets = '/pets';
  static const String petCreate = '/pets/create';
  static const String petDetails = '/pets/:petId';
  static const String petEdit = '/pets/:petId/edit';

  // Discovery.
  static const String discover = '/discover';

  // Matching.
  static const String matches = '/matches';

  // Favorites.
  static const String favorites = '/favorites';

  // Activities.
  static const String activities = '/activities';
  static const String activityDetails = '/activities/:activityId';

  // Location & map.
  static const String map = '/map';

  // Messaging.
  static const String conversations = '/messages';
  static const String chat = '/messages/:conversationId';

  // Notifications.
  static const String notifications = '/notifications';

  // Search.
  static const String search = '/search';

  // Reviews.
  static const String reviews = '/reviews';

  // Safety & reporting.
  static const String safety = '/safety';
  static const String reports = '/reports';
  static const String blocking = '/blocking';

  // Subscriptions.
  static const String subscriptions = '/subscriptions';

  // Settings.
  static const String settings = '/settings';

  // Root shell (contains the bottom navigation).
  static const String home = '/home';
}
