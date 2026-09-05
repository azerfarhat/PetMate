import 'package:flutter/foundation.dart';

import '../../../../core/errors/failures.dart';
import '../../../../core/utils/validators.dart';
import '../../domain/entities/auth_user.dart';
import '../../domain/usecases/get_current_user.dart';
import '../../domain/usecases/login.dart';
import '../../domain/usecases/logout.dart';
import '../../domain/usecases/register.dart';

/// Manages authentication UI state.
///
/// Depends on Domain use cases only — never makes direct API calls.
class AuthController extends ChangeNotifier {
  AuthController({
    required Login login,
    required Register register,
    required Logout logout,
    required GetCurrentUser getCurrentUser,
  })  : _login = login,
        _register = register,
        _logout = logout,
        _getCurrentUser = getCurrentUser;

  final Login _login;
  final Register _register;
  final Logout _logout;
  final GetCurrentUser _getCurrentUser;

  AuthUser? _user;
  AuthUser? get user => _user;

  bool _isAuthenticated = false;
  bool get isAuthenticated => _isAuthenticated;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  /// Validates and logs the user in.
  Future<bool> login({
    required String email,
    required String password,
  }) async {
    final emailError = Validators.email(email);
    final passwordError = Validators.password(password);
    if (emailError != null || passwordError != null) {
      _errorMessage = emailError ?? passwordError;
      notifyListeners();
      return false;
    }

    _setLoading(true);
    try {
      final session = await _login(email: email, password: password);
      _user = session.user;
      _isAuthenticated = true;
      _errorMessage = null;
      return true;
    } on Failure catch (failure) {
      _errorMessage = failure.message;
      return false;
    } finally {
      _setLoading(false);
    }
  }

  /// Validates and registers a new account.
  Future<bool> register({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  }) async {
    final emailError = Validators.email(email);
    final passwordError = Validators.password(password);
    final firstNameError = Validators.required(firstName);
    final lastNameError = Validators.required(lastName);
    final nameError = firstNameError ?? lastNameError;
    if (emailError != null || passwordError != null || nameError != null) {
      _errorMessage = emailError ?? passwordError ?? nameError;
      notifyListeners();
      return false;
    }

    _setLoading(true);
    try {
      final session = await _register(
        email: email,
        password: password,
        firstName: firstName,
        lastName: lastName,
      );
      _user = session.user;
      _isAuthenticated = true;
      _errorMessage = null;
      return true;
    } on Failure catch (failure) {
      _errorMessage = failure.message;
      return false;
    } finally {
      _setLoading(false);
    }
  }

  /// Logs the current user out.
  Future<void> logout() async {
    _setLoading(true);
    try {
      await _logout();
    } on Failure catch (failure) {
      _errorMessage = failure.message;
    } finally {
      _user = null;
      _isAuthenticated = false;
      _setLoading(false);
    }
  }

  /// Restores an existing session on app start.
  Future<void> restoreSession() async {
    _setLoading(true);
    try {
      final currentUser = await _getCurrentUser();
      _user = currentUser;
      _isAuthenticated = currentUser != null;
    } on Failure catch (failure) {
      _errorMessage = failure.message;
    } finally {
      _setLoading(false);
    }
  }

  void _setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }
}
