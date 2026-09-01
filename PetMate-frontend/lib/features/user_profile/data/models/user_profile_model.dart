import '../dtos/user_profile_dto.dart';

/// Data-layer representation of a user profile.
///
/// Wraps a [UserProfileDto] and provides serialization for local caching.
class UserProfileModel {
  const UserProfileModel({required this.dto});

  final UserProfileDto dto;

  factory UserProfileModel.fromDto(UserProfileDto dto) =>
      UserProfileModel(dto: dto);

  factory UserProfileModel.fromJson(Map<String, dynamic> json) =>
      UserProfileModel(dto: UserProfileDto.fromJson(json));

  Map<String, dynamic> toJson() => dto.toJson();

  String get id => dto.id;
  String get displayName => dto.displayName;
}
