import '../../../pets/data/mappers/pet_mapper.dart';
import '../../../pets/domain/entities/pet.dart';
import '../../domain/entities/user_profile.dart';
import '../../domain/repositories/user_profile_repository.dart';
import '../datasources/user_profile_remote_data_source.dart';
import '../dtos/user_profile_update_request_dto.dart';
import '../mappers/user_profile_mapper.dart';

/// Implementation of [UserProfileRepository].
class UserProfileRepositoryImpl implements UserProfileRepository {
  UserProfileRepositoryImpl(this._remoteDataSource);

  final UserProfileRemoteDataSource _remoteDataSource;

  @override
  Future<UserProfile> getMyProfile() async {
    final dto = await _remoteDataSource.getMyProfile();
    return UserProfileMapper.toEntity(dto);
  }

  @override
  Future<UserProfile> getUserProfile(String userId) async {
    final dto = await _remoteDataSource.getUserProfile(userId);
    return UserProfileMapper.toEntity(dto);
  }

  @override
  Future<UserProfile> updateProfile({
    required String firstName,
    required String lastName,
    String? profilePicture,
    String? bio,
    required List<Pet> pets,
    double? latitude,
    double? longitude,
    int? searchRadius,
  }) async {
    final request = UserProfileUpdateRequestDto(
      firstName: firstName,
      lastName: lastName,
      profilePicture: profilePicture,
      bio: bio,
      latitude: latitude,
      longitude: longitude,
      searchRadius: searchRadius,
      pets: pets.map(PetMapper.toRequestDto).toList(),
    );
    final dto = await _remoteDataSource.updateProfile(request);
    return UserProfileMapper.toEntity(dto);
  }
}