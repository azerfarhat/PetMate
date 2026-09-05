import '../datasources/onboarding_local_data_source.dart';
import '../../domain/repositories/onboarding_repository.dart';

/// Implementation of [OnboardingRepository] backed by local storage.
class OnboardingRepositoryImpl implements OnboardingRepository {
  OnboardingRepositoryImpl(this._dataSource);

  final OnboardingLocalDataSource _dataSource;

  @override
  Future<bool> isOnboardingCompleted() {
    return _dataSource.isCompleted();
  }

  @override
  Future<void> completeOnboarding() {
    return _dataSource.markCompleted();
  }
}
