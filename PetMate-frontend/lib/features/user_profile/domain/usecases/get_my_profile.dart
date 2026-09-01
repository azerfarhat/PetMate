import '../entities/user_profile.dart';
import '../repositories/user_profile_repository.dart';

/// Fetches the profile of the currently authenticated user.
class GetMyProfile {
  const GetMyProfile(this._repository);

  final UserProfileRepository _repository;

  Future<UserProfile> call() => _repository.getMyProfile();
}
