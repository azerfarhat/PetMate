import '../entities/user_profile.dart';
import '../repositories/user_profile_repository.dart';

/// Updates the current user's profile.
class UpdateUserProfile {
  const UpdateUserProfile(this._repository);

  final UserProfileRepository _repository;

  Future<UserProfile> call({
    String? displayName,
    String? bio,
    String? avatarUrl,
    DateTime? dateOfBirth,
  }) {
    return _repository.updateProfile(
      displayName: displayName,
      bio: bio,
      avatarUrl: avatarUrl,
      dateOfBirth: dateOfBirth,
    );
  }
}
