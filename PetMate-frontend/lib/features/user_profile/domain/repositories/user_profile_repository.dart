import '../../../pets/domain/entities/pet.dart';
import '../entities/user_profile.dart';

/// Repository contract for user profile data.
abstract interface class UserProfileRepository {
  /// Fetches the profile of the currently authenticated user.
  Future<UserProfile> getMyProfile();

  /// Fetches the profile of any user by id (e.g. a match).
  Future<UserProfile> getUserProfile(String userId);

  /// Replaces the current user's profile.
  ///
  /// The backend PUT is a full replacement, so the first name, last name and
  /// the complete list of pets are always required.
  Future<UserProfile> updateProfile({
    required String firstName,
    required String lastName,
    String? profilePicture,
    String? bio,
    required List<Pet> pets,
    double? latitude,
    double? longitude,
    int? searchRadius,
  });
}