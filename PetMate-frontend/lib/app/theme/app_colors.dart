import 'package:flutter/material.dart';

import 'app_colors.dart';

/// Centralized color configuration for PawMate.
///
/// All visual configuration lives here so that colors are never hardcoded
/// across the application. Use the named values rather than raw hex values.
class AppColors {
  AppColors._();

  /// Primary Coral - primary brand action color.
  static const Color primary = Color(0xFFFF6B4A);

  /// Sage Green - success, positive, and nature-related accents.
  static const Color sage = Color(0xFF7CBF8A);

  /// Warm Yellow - highlights, ratings and informational accents.
  static const Color warmYellow = Color(0xFFFFD166);

  /// Soft Pink - feminine accents, secondary highlight.
  static const Color softPink = Color(0xFFF28B9C);

  /// Cream Background - main application background.
  static const Color cream = Color(0xFFFFF9F5);

  /// Dark Brown - primary text and dark surfaces.
  static const Color darkBrown = Color(0xFF292321);

  // Derived neutrals.
  static const Color white = Colors.white;
  static const Color black = Colors.black;

  /// Text color on light content.
  static const Color textPrimary = darkBrown;

  /// Muted / secondary text.
  static const Color textSecondary = Color(0xFF6B5D59);

  /// Hint text.
  static const Color textHint = Color(0xFFA89B97);

  /// Divider and border color.
  static const Color divider = Color(0xFFEDE4DE);

  /// Danger / error color.
  static const Color danger = Color(0xFFE5484D);

  /// Success color.
  static const Color success = sage;

  /// Disabled state color.
  static const Color disabled = Color(0xFFD9CFCB);
}
