/// Response body returned by authentication endpoints.
///
/// Follows the backend `AuthResponse` contract: only the tokens are present,
/// never the user profile. The user object is fetched separately via the
/// profile endpoint (`GET /users/me`).
class AuthResponseDto {
  const AuthResponseDto({
    required this.accessToken,
    this.refreshToken,
    this.tokenType = 'Bearer',
    this.expiresIn = 0,
  });

  final String accessToken;
  final String? refreshToken;
  final String tokenType;
  final int expiresIn;

  factory AuthResponseDto.fromJson(Map<String, dynamic> json) {
    return AuthResponseDto(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String?,
      tokenType: json['tokenType'] as String? ?? 'Bearer',
      expiresIn: json['expiresIn'] as int? ?? 0,
    );
  }
}