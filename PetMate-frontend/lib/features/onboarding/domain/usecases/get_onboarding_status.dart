import '../repositories/onboarding_repository.dart';

/// Checks whether the user has already completed onboarding.
class GetOnboardingStatus {
  const GetOnboardingStatus(this._repository);

  final OnboardingRepository _repository;

  Future<bool> call() => _repository.isOnboardingCompleted();
}
