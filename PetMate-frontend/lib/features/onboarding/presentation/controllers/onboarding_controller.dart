import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';

import '../domain/entities/onboarding_slide.dart';

/// UI state for the onboarding flow.
class OnboardingController extends ChangeNotifier {
  OnboardingController({List<OnboardingSlide>? slides})
      : _slides = slides ?? defaultSlides;

  /// The slides displayed in the onboarding carousel.
  final List<OnboardingSlide> _slides;
  List<OnboardingSlide> get slides => List.unmodifiable(_slides);

  int _currentPage = 0;
  int get currentPage => _currentPage;

  bool get isLastPage => _currentPage == _slides.length - 1;

  /// Advances to the next slide. Returns `false` when already on the last page.
  bool nextPage() {
    if (isLastPage) {
      return false;
    }
    _currentPage += 1;
    notifyListeners();
    return true;
  }

  /// Jumps to a specific slide index.
  void setPage(int page) {
    if (page < 0 || page >= _slides.length) {
      return;
    }
    _currentPage = page;
    notifyListeners();
  }

  static const List<OnboardingSlide> defaultSlides = [
    OnboardingSlide(
      title: 'Welcome to PawMate',
      description:
          'Find compatible pets and their owners nearby. It\'s like dating, but for your pet!',
      imagePath: 'assets/images/onboarding/welcome.png',
    ),
    OnboardingSlide(
      title: 'Create pet profiles',
      description:
          'Set up profiles for your furry friends and show off their personality.',
      imagePath: 'assets/images/onboarding/create_pets.png',
    ),
    OnboardingSlide(
      title: 'Discover & match',
      description:
          'Swipe to like or pass on nearby pets and match with compatible owners.',
      imagePath: 'assets/images/onboarding/discover.png',
    ),
    OnboardingSlide(
      title: 'Meet up & play',
      description:
          'Chat with matches, organise walks, picnics and playdates for your pets.',
      imagePath: 'assets/images/onboarding/meetup.png',
    ),
  ];
}
