import '../entities/user_profile.dart';

/// Repository contract for user profile data.
abstract interface class UserProfileRepository {
  /// Fetches the profile of the currently authenticated user.
  Future<UserProfile> getMyProfile();

  /// Fetches the profile of any user by id (e.g. a match).
  Future<UserProfile> getUserProfile(String userId);

  /// Updates the current user's profile.
  Future<UserProfile> updateProfile({
    String? displayName,
    String? bio,
    String? avatarUrl,
    DateTime? dateOfBirth,
  });
}
