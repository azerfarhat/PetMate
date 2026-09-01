import 'auth_user.dart';

/// Result of a successful authentication, containing the token and user.
class AuthSession {
  const AuthSession({
    required this.token,
    this.refreshToken,
    required this.user,
  });

  /// Access token used for authenticated requests.
  final String token;

  /// Optional refresh token for session renewal.
  final String? refreshToken;

  final AuthUser user;
}
