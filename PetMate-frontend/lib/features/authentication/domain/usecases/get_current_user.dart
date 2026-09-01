import '../entities/auth_user.dart';
import '../repositories/auth_repository.dart';

/// Retrieves the currently authenticated user, or `null` when logged out.
class GetCurrentUser {
  const GetCurrentUser(this._repository);

  final AuthRepository _repository;

  Future<AuthUser?> call() => _repository.getCurrentUser();
}
