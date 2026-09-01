import '../repositories/onboarding_repository.dart';

/// Marks onboarding as completed so the user is not shown it again.
class CompleteOnboarding {
  const CompleteOnboarding(this._repository);

  final OnboardingRepository _repository;

  Future<void> call() => _repository.completeOnboarding();
}
