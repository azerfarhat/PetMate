/// Response body for a user profile endpoint.
///
/// Follows the backend contract: the private profile (`UserResponse`, from
/// `GET /users/me`) and the public profile (`PublicUserResponse`, from
/// `GET /users/{id}`) share this shape, the public variant simply omitting
/// the private fields (`email`, location, verification…).
class UserProfileDto {
  const UserProfileDto({
    required this.id,
    required this.firstName,
    this.lastName,
    this.email,
    this.profilePicture,
    this.bio,
    this.latitude,
    this.longitude,
    this.searchRadius,
    this.emailVerified = false,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String firstName;
  final String? lastName;
  final String? email;
  final String? profilePicture;
  final String? bio;
  final double? latitude;
  final double? longitude;
  final int? searchRadius;
  final bool emailVerified;
  final String? createdAt;
  final String? updatedAt;

  factory UserProfileDto.fromJson(Map<String, dynamic> json) {
    return UserProfileDto(
      id: (json['id'] as num).toString(),
      firstName: json['firstName'] as String? ?? '',
      lastName: json['lastName'] as String?,
      email: json['email'] as String?,
      profilePicture: json['profilePicture'] as String?,
      bio: json['bio'] as String?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      searchRadius: (json['searchRadius'] as num?)?.toInt(),
      emailVerified: json['emailVerified'] as bool? ?? false,
      createdAt: json['createdAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'firstName': firstName,
      'lastName': lastName,
      'email': email,
      'profilePicture': profilePicture,
      'bio': bio,
      'latitude': latitude,
      'longitude': longitude,
      'searchRadius': searchRadius,
      'emailVerified': emailVerified,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
    };
  }
}