import '../../domain/entities/user_profile.dart';
import '../dtos/user_profile_dto.dart';

/// Maps between user profile DTOs and Domain entities.
abstract final class UserProfileMapper {
  UserProfileMapper._();

  static UserProfile toEntity(UserProfileDto dto) {
    return UserProfile(
      id: dto.id,
      firstName: dto.firstName,
      lastName: dto.lastName,
      email: dto.email,
      profilePicture: dto.profilePicture,
      bio: dto.bio,
      latitude: dto.latitude,
      longitude: dto.longitude,
      searchRadius: dto.searchRadius,
      isVerified: dto.emailVerified,
      createdAt: dto.createdAt != null ? DateTime.tryParse(dto.createdAt!) : null,
      updatedAt: dto.updatedAt != null ? DateTime.tryParse(dto.updatedAt!) : null,
    );
  }
}