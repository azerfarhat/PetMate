import '../entities/auth_session.dart';
import '../entities/auth_user.dart';

/// Repository contract for authentication.
///
/// Implemented in the Data layer. The Domain layer depends only on this
/// interface, keeping business logic independent of the backend.
abstract interface class AuthRepository {
  /// Logs in with email/password and returns the resulting [AuthSession].
  Future<AuthSession> login({
    required String email,
    required String password,
  });

  /// Registers a new account and returns the resulting [AuthSession].
  Future<AuthSession> register({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  });

  /// Logs the current user out and clears the local session.
  Future<void> logout();

  /// Returns the currently authenticated user, or `null` when logged out.
  Future<AuthUser?> getCurrentUser();
}
