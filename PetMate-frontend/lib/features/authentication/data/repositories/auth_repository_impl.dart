import 'dart:convert';

import '../../../../core/constants/api_constants.dart';
import '../../../../core/storage/local_storage.dart';
import '../../domain/entities/auth_session.dart';
import '../../domain/entities/auth_user.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../../user_profile/data/datasources/user_profile_remote_data_source.dart';
import '../datasources/auth_remote_data_source.dart';
import '../dtos/auth_response_dto.dart';
import '../mappers/auth_mapper.dart';
import '../models/auth_session_model.dart';

/// Implementation of [AuthRepository].
///
/// Orchestrates the remote datasources (backend calls) and local secure
/// storage (session persistence). No business logic lives in widgets.
class AuthRepositoryImpl implements AuthRepository {
  AuthRepositoryImpl(
    this._remoteDataSource,
    this._localStorage,
    this._profileDataSource,
  );

  final AuthRemoteDataSource _remoteDataSource;
  final LocalStorage _localStorage;
  final UserProfileRemoteDataSource _profileDataSource;

  @override
  Future<AuthSession> login({
    required String email,
    required String password,
  }) async {
    final auth = await _remoteDataSource.login(
      email: email,
      password: password,
    );
    return _completeSession(auth);
  }

  @override
  Future<AuthSession> register({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  }) async {
    final auth = await _remoteDataSource.register(
      email: email,
      password: password,
      firstName: firstName,
      lastName: lastName,
    );
    return _completeSession(auth);
  }

  @override
  Future<void> logout() async {
    try {
      await _remoteDataSource.logout();
    } finally {
      await _clearSession();
    }
  }

  @override
  Future<AuthUser?> getCurrentUser() async {
    final session = await _readSession();
    if (session == null) {
      return null;
    }
    return AuthMapper.toSessionFromModel(session).user;
  }

  /// Persists the tokens, fetches the current user profile and builds the
  /// session. The token is stored before the profile call so the request is
  /// authenticated; on failure the already-stored tokens are kept for retry.
  Future<AuthSession> _completeSession(AuthResponseDto auth) async {
    await _localStorage.write(ApiConstants.authTokenKey, auth.accessToken);
    if (auth.refreshToken != null) {
      await _localStorage.write(
        ApiConstants.refreshTokenKey,
        auth.refreshToken!,
      );
    }
    final userDto = await _profileDataSource.getMyProfile();
    final session = AuthMapper.toSession(auth, userDto);
    final model = AuthMapper.toModel(session);
    await _localStorage.write(ApiConstants.currentUserIdKey, model.userId);
    await _localStorage.write(
      ApiConstants.currentUserKey,
      jsonEncode(model.toJson()),
    );
    return session;
  }

  Future<AuthSessionModel?> _readSession() async {
    final raw = await _localStorage.read(ApiConstants.currentUserKey);
    if (raw == null || raw.isEmpty) {
      return null;
    }
    try {
      final json = jsonDecode(raw) as Map<String, dynamic>;
      return AuthSessionModel.fromJson(json);
    } catch (_) {
      return null;
    }
  }

  Future<void> _clearSession() async {
    await _localStorage.delete(ApiConstants.authTokenKey);
    await _localStorage.delete(ApiConstants.refreshTokenKey);
    await _localStorage.delete(ApiConstants.currentUserIdKey);
    await _localStorage.delete(ApiConstants.currentUserKey);
  }
}