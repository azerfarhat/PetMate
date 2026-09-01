import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_dimensions.dart';
import '../../../../core/widgets/app_button.dart';
import '../controllers/onboarding_controller.dart';
import '../widgets/onboarding_slide_view.dart';

/// Onboarding screen shown on first launch.
class OnboardingPage extends StatefulWidget {
  const OnboardingPage({super.key, this.controller});

  final OnboardingController? controller;

  @override
  State<OnboardingPage> createState() => _OnboardingPageState();
}

class _OnboardingPageState extends State<OnboardingPage> {
  late final OnboardingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? OnboardingController();
  }

  @override
  void dispose() {
    if (widget.controller == null) {
      _controller.dispose();
    }
    super.dispose();
  }

  void _onNext() {
    final moved = _controller.nextPage();
    if (!moved) {
      _finish();
    }
  }

  void _finish() {
    // TODO: mark onboarding complete and navigate to authentication.
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.cream,
      body: SafeArea(
        child: AnimatedBuilder(
          animation: _controller,
          builder: (context, _) {
            return Padding(
              padding: const EdgeInsets.all(AppDimensions.s6),
              child: Column(
                children: [
                  Align(
                    alignment: Alignment.topRight,
                    child: _controller.isLastPage
                        ? const SizedBox.shrink()
                        : TextButton(
                            onPressed: _finish,
                            child: const Text('Skip'),
                          ),
                  ),
                  Expanded(
                    child: PageView.builder(
                      controller: PageController(),
                      itemCount: _controller.slides.length,
                      onPageChanged: _controller.setPage,
                      itemBuilder: (context, index) =>
                          OnboardingSlideView(
                            slide: _controller.slides[index],
                          ),
                    ),
                  ),
                  const SizedBox(height: AppDimensions.s6),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(
                      _controller.slides.length,
                      (i) => AnimatedContainer(
                        duration: const Duration(milliseconds: 250),
                        margin: const EdgeInsets.symmetric(
                          horizontal: 4,
                        ),
                        width: _controller.currentPage == i ? 24 : 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: _controller.currentPage == i
                              ? AppColors.primary
                              : AppColors.divider,
                          borderRadius: BorderRadius.circular(
                            AppDimensions.radiusRound,
                          ),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: AppDimensions.s6),
                  AppButton(
                    label: _controller.isLastPage ? 'Get started' : 'Next',
                    onPressed: _onNext,
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}
