import '../../../../core/constants/api_constants.dart';
import '../../../../core/storage/local_storage.dart';
import '../domain/repositories/onboarding_repository.dart';

/// Local datasource for onboarding state.
///
/// Onboarding completion is a purely local flag; no backend is required.
class OnboardingLocalDataSource {
  OnboardingLocalDataSource(this._storage);

  final LocalStorage _storage;

  Future<bool> isCompleted() async {
    final value = await _storage.read(ApiConstants.onboardingCompletedKey);
    return value == 'true';
  }

  Future<void> markCompleted() async {
    await _storage.write(ApiConstants.onboardingCompletedKey, 'true');
  }
}
