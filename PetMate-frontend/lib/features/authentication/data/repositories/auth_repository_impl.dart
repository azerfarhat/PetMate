import 'dart:convert';

import '../../../../core/constants/api_constants.dart';
import '../../../../core/storage/local_storage.dart';
import '../../domain/entities/auth_session.dart';
import '../../domain/entities/auth_user.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_data_source.dart';
import '../mappers/auth_mapper.dart';
import '../models/auth_session_model.dart';

/// Implementation of [AuthRepository].
///
/// Orchestrates the remote datasource (backend calls) and local secure
/// storage (session persistence). No business logic lives in widgets.
class AuthRepositoryImpl implements AuthRepository {
  AuthRepositoryImpl(this._remoteDataSource, this._localStorage);

  final AuthRemoteDataSource _remoteDataSource;
  final LocalStorage _localStorage;

  @override
  Future<AuthSession> login({
    required String email,
    required String password,
  }) async {
    final dto = await _remoteDataSource.login(
      email: email,
      password: password,
    );
    final session = AuthMapper.toSession(dto);
    await _persistSession(session);
    return session;
  }

  @override
  Future<AuthSession> register({
    required String email,
    required String password,
    required String displayName,
  }) async {
    final dto = await _remoteDataSource.register(
      email: email,
      password: password,
      displayName: displayName,
    );
    final session = AuthMapper.toSession(dto);
    await _persistSession(session);
    return session;
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

  Future<void> _persistSession(AuthSession session) async {
    final model = AuthMapper.toModel(session);
    await _localStorage.write(ApiConstants.authTokenKey, model.token);
    if (model.refreshToken != null) {
      await _localStorage.write(
        ApiConstants.refreshTokenKey,
        model.refreshToken!,
      );
    }
    await _localStorage.write(ApiConstants.currentUserIdKey, model.userId);
    await _localStorage.write(
      ApiConstants.currentUserKey,
      jsonEncode(model.toJson()),
    );
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
