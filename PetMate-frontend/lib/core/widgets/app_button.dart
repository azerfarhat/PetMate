import 'package:flutter/material.dart';

import '../../app/theme/app_dimensions.dart';

/// Reusable primary/primary-outlined button used across the application.
class AppButton extends StatelessWidget {
  const AppButton({
    super.key,
    required this.label,
    this.onPressed,
    this.loading = false,
    this.outlined = false,
    this.icon,
    this.expand = true,
  });

  /// Button label.
  final String label;

  /// Callback when the button is pressed. When null the button is disabled.
  final VoidCallback? onPressed;

  /// Shows a spinner and disables the button when true.
  final bool loading;

  /// Renders an outlined (secondary) button when true.
  final bool outlined;

  /// Optional leading icon.
  final IconData? icon;

  /// Whether the button fills the available width.
  final bool expand;

  @override
  Widget build(BuildContext context) {
    final onTap = (loading || onPressed == null) ? null : onPressed;

    final child = loading
        ? const SizedBox(
            width: 22,
            height: 22,
            child: CircularProgressIndicator(
              strokeWidth: 2.5,
              color: Colors.white,
            ),
          )
        : Row(
            mainAxisSize: expand ? MainAxisSize.max : MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(icon, size: 20),
                const SizedBox(width: AppDimensions.s2),
              ],
              Flexible(
                child: Text(
                  label,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.labelLarge,
                ),
              ),
            ],
          );

    final childWithPadding = Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppDimensions.s4,
        vertical: AppDimensions.s3,
      ),
      child: child,
    );

    if (outlined) {
      return OutlinedButton(
        onPressed: loading ? null : onPressed,
        style: ButtonStyle(
          minimumSize: WidgetStatePropertyAll(
            Size.fromHeight(expand ? AppDimensions.buttonHeight : 44),
          ),
        ),
        child: childWithPadding,
      );
    }

    return ElevatedButton(
      onPressed: onTap,
      style: ButtonStyle(
        minimumSize: WidgetStatePropertyAll(
          Size.fromHeight(expand ? AppDimensions.buttonHeight : 44),
        ),
      ),
      child: childWithPadding,
    );
  }
}
