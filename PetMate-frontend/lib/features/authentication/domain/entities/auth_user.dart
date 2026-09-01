/// Authenticated user entity in the Domain layer.
class AuthUser {
  const AuthUser({
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
}
