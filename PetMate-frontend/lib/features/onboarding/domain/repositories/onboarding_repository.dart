abstract interface class OnboardingRepository {
  Future<bool> isOnboardingCompleted();
  Future<void> completeOnboarding();
}
