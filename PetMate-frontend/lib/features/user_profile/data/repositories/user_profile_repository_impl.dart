import '../../domain/entities/user_profile.dart';
import '../../domain/repositories/user_profile_repository.dart';
import '../datasources/user_profile_remote_data_source.dart';
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
    String? displayName,
    String? bio,
    String? avatarUrl,
    DateTime? dateOfBirth,
  }) async {
    final body = <String, dynamic>{
      if (displayName != null) 'display_name': displayName,
      if (bio != null) 'bio': bio,
      if (avatarUrl != null) 'avatar_url': avatarUrl,
      if (dateOfBirth != null)
        'date_of_birth': dateOfBirth.toIso8601String(),
    };
    final dto = await _remoteDataSource.updateProfile(body);
    return UserProfileMapper.toEntity(dto);
  }
}
