import '../entities/auth_session.dart';
import '../repositories/auth_repository.dart';

/// Registers a new user account.
class Register {
  const Register(this._repository);

  final AuthRepository _repository;

  Future<AuthSession> call({
    required String email,
    required String password,
    required String displayName,
  }) {
    return _repository.register(
      email: email,
      password: password,
      displayName: displayName,
    );
  }
}
