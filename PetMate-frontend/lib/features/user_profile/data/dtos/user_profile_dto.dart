/// Response body for a user profile endpoint.
class UserProfileDto {
  const UserProfileDto({
    required this.id,
    required this.displayName,
    this.bio,
    this.avatarUrl,
    this.city,
    this.dateOfBirth,
    this.isVerified = false,
    this.joinedAt,
  });

  final String id;
  final String displayName;
  final String? bio;
  final String? avatarUrl;
  final String? city;
  final String? dateOfBirth;
  final bool isVerified;
  final String? joinedAt;

  factory UserProfileDto.fromJson(Map<String, dynamic> json) {
    return UserProfileDto(
      id: json['id'] as String,
      displayName: json['display_name'] as String,
      bio: json['bio'] as String?,
      avatarUrl: json['avatar_url'] as String?,
      city: json['city'] as String?,
      dateOfBirth: json['date_of_birth'] as String?,
      isVerified: json['is_verified'] as bool? ?? false,
      joinedAt: json['joined_at'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'display_name': displayName,
      'bio': bio,
      'avatar_url': avatarUrl,
      'city': city,
      'date_of_birth': dateOfBirth,
    };
  }
}
