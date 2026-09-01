import 'package:flutter/foundation.dart';

import '../../../core/errors/failures.dart';
import '../domain/entities/user_profile.dart';
import '../domain/usecases/get_my_profile.dart';
import '../domain/usecases/update_user_profile.dart';

/// Manages the current user's profile UI state.
class UserProfileController extends ChangeNotifier {
  UserProfileController({
    required GetMyProfile getMyProfile,
    required UpdateUserProfile updateUserProfile,
  })  : _getMyProfile = getMyProfile,
        _updateUserProfile = updateUserProfile;

  final GetMyProfile _getMyProfile;
  final UpdateUserProfile _updateUserProfile;

  UserProfile? _profile;
  UserProfile? get profile => _profile;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  /// Loads the current user's profile.
  Future<void> loadProfile() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();
    try {
      _profile = await _getMyProfile();
    } on Failure catch (failure) {
      _errorMessage = failure.message;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Updates the current user's profile.
  Future<bool> updateProfile({
    String? displayName,
    String? bio,
    String? avatarUrl,
    DateTime? dateOfBirth,
  }) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();
    try {
      _profile = await _updateUserProfile(
        displayName: displayName,
        bio: bio,
        avatarUrl: avatarUrl,
        dateOfBirth: dateOfBirth,
      );
      return true;
    } on Failure catch (failure) {
      _errorMessage = failure.message;
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
