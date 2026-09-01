import 'package:flutter/material.dart';

import '../../app/theme/app_colors.dart';

/// Centered loading indicator used for full-screen or in-place loading states.
class LoadingWidget extends StatelessWidget {
  const LoadingWidget({
    super.key,
    this.message,
    this.inline = false,
    this.padding = const EdgeInsets.all(32),
  });

  /// Optional message shown below the spinner.
  final String? message;

  /// When true the widget is used inside an already-laid-out container (no
  /// centering by an outer Stack is assumed).
  final bool inline;

  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    final content = Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const CircularProgressIndicator(color: AppColors.primary),
        if (message != null) ...[
          const SizedBox(height: 16),
          Text(
            message!,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ],
    );

    return Center(
      child: Padding(
        padding: padding,
        child: content,
      ),
    );
  }
}
