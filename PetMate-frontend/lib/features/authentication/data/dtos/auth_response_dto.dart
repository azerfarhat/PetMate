/// Response body returned by authentication endpoints.
///
/// Represents the API contract for auth responses. Transformed into Domain
/// entities via [AuthMapper] — never exposed directly to the UI.
class AuthResponseDto {
  const AuthResponseDto({
    required this.token,
    this.refreshToken,
    required this.user,
  });

  final String token;
  final String? refreshToken;
  final AuthUserDto user;

  factory AuthResponseDto.fromJson(Map<String, dynamic> json) {
    return AuthResponseDto(
      token: json['token'] as String,
      refreshToken: json['refresh_token'] as String?,
      user: AuthUserDto.fromJson(json['user'] as Map<String, dynamic>),
    );
  }
}

/// Nested user payload within an auth response.
class AuthUserDto {
  const AuthUserDto({
    required this.id,
    required this.email,
    this.displayName,
    this.avatarUrl,
    this.isVerified = false,
  });

  final String id;
  final String email;
  final String? displayName;
  final String? avatarUrl;
  final bool isVerified;

  factory AuthUserDto.fromJson(Map<String, dynamic> json) {
    return AuthUserDto(
      id: json['id'] as String,
      email: json['email'] as String,
      displayName: json['display_name'] as String?,
      avatarUrl: json['avatar_url'] as String?,
      isVerified: json['is_verified'] as bool? ?? false,
    );
  }
}
