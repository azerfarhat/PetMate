import '../../../pets/domain/entities/pet.dart';
import '../entities/user_profile.dart';
import '../repositories/user_profile_repository.dart';

/// Updates the current user's profile.
class UpdateUserProfile {
  const UpdateUserProfile(this._repository);

  final UserProfileRepository _repository;

  Future<UserProfile> call({
    required String firstName,
    required String lastName,
    String? profilePicture,
    String? bio,
    required List<Pet> pets,
    double? latitude,
    double? longitude,
    int? searchRadius,
  }) {
    return _repository.updateProfile(
      firstName: firstName,
      lastName: lastName,
      profilePicture: profilePicture,
      bio: bio,
      pets: pets,
      latitude: latitude,
      longitude: longitude,
      searchRadius: searchRadius,
    );
  }
}