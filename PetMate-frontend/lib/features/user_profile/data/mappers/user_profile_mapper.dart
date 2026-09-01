import '../../domain/entities/user_profile.dart';
import '../dtos/user_profile_dto.dart';

/// Maps between user profile DTOs and Domain entities.
abstract final class UserProfileMapper {
  UserProfileMapper._();

  static UserProfile toEntity(UserProfileDto dto) {
    return UserProfile(
      id: dto.id,
      displayName: dto.displayName,
      bio: dto.bio,
      avatarUrl: dto.avatarUrl,
      city: dto.city,
      dateOfBirth: dto.dateOfBirth != null
          ? DateTime.tryParse(dto.dateOfBirth!)
          : null,
      isVerified: dto.isVerified,
      joinedAt: dto.joinedAt != null ? DateTime.tryParse(dto.joinedAt!) : null,
    );
  }
}
