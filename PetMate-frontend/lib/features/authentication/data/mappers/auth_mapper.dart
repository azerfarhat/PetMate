import '../../domain/entities/auth_session.dart';
import '../../domain/entities/auth_user.dart';
import '../../../user_profile/data/dtos/user_profile_dto.dart';
import '../dtos/auth_response_dto.dart';
import '../models/auth_session_model.dart';

/// Maps between authentication DTOs/models and Domain entities.
///
/// Keeps transformation logic out of widgets, datasources and repositories.
abstract final class AuthMapper {
  AuthMapper._();

  /// Converts the login/register token response plus the fetched profile into
  /// a Domain [AuthSession].
  static AuthSession toSession(AuthResponseDto auth, UserProfileDto user) {
    return AuthSession(
      token: auth.accessToken,
      refreshToken: auth.refreshToken,
      user: AuthUser(
        id: user.id,
        email: user.email ?? '',
        displayName: [user.firstName.trim(), user.lastName?.trim() ?? '']
            .where((part) => part.isNotEmpty)
            .join(' '),
        avatarUrl: user.profilePicture,
        isVerified: user.emailVerified,
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