/// Persistent representation of an authentication session.
///
/// Used for local session persistence and mapping back to the Domain
/// [AuthSession] entity. Serialization logic lives here.
class AuthSessionModel {
  const AuthSessionModel({
    required this.token,
    this.refreshToken,
    required this.userId,
    required this.email,
    this.displayName,
    this.avatarUrl,
    this.isVerified = false,
  });

  final String token;
  final String? refreshToken;
  final String userId;
  final String email;
  final String? displayName;
  final String? avatarUrl;
  final bool isVerified;

  /// Whether an auth session is currently stored.
  bool get isAuthenticated => token.isNotEmpty;

  Map<String, dynamic> toJson() {
    return {
      'token': token,
      'refresh_token': refreshToken,
      'user_id': userId,
      'email': email,
      'display_name': displayName,
      'avatar_url': avatarUrl,
      'is_verified': isVerified,
    };
  }

  factory AuthSessionModel.fromJson(Map<String, dynamic> json) {
    return AuthSessionModel(
      token: json['token'] as String,
      refreshToken: json['refresh_token'] as String?,
      userId: json['user_id'] as String,
      email: json['email'] as String,
      displayName: json['display_name'] as String?,
      avatarUrl: json['avatar_url'] as String?,
      isVerified: json['is_verified'] as bool? ?? false,
    );
  }
}
