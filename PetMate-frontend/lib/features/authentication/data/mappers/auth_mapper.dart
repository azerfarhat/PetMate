import '../../domain/entities/auth_session.dart';
import '../../domain/entities/auth_user.dart';
import '../dtos/auth_response_dto.dart';
import '../models/auth_session_model.dart';

/// Maps between authentication DTOs/models and Domain entities.
///
/// Keeps transformation logic out of widgets, datasources and repositories.
abstract final class AuthMapper {
  AuthMapper._();

  /// Converts an API response DTO into a Domain [AuthSession].
  static AuthSession toSession(AuthResponseDto dto) {
    return AuthSession(
      token: dto.token,
      refreshToken: dto.refreshToken,
      user: AuthUser(
        id: dto.user.id,
        email: dto.user.email,
        displayName: dto.user.displayName,
        avatarUrl: dto.user.avatarUrl,
        isVerified: dto.user.isVerified,
      ),
    );
  }

  /// Converts a persisted [AuthSessionModel] into a Domain [AuthSession].
  static AuthSession toSessionFromModel(AuthSessionModel model) {
    return AuthSession(
      token: model.token,
      refreshToken: model.refreshToken,
      user: AuthUser(
        id: model.userId,
        email: model.email,
        displayName: model.displayName,
        avatarUrl: model.avatarUrl,
        isVerified: model.isVerified,
      ),
    );
  }

  /// Converts a Domain [AuthSession] into a persisted [AuthSessionModel].
  static AuthSessionModel toModel(AuthSession session) {
    return AuthSessionModel(
      token: session.token,
      refreshToken: session.refreshToken,
      userId: session.user.id,
      email: session.user.email,
      displayName: session.user.displayName,
      avatarUrl: session.user.avatarUrl,
      isVerified: session.user.isVerified,
    );
  }
}
