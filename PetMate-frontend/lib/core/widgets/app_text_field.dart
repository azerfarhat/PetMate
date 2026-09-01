import 'package:flutter/material.dart';

import '../../app/theme/app_dimensions.dart';

/// Reusable text field with centralized styling and optional validation.
class AppTextField extends StatelessWidget {
  const AppTextField({
    super.key,
    required this.controller,
    this.label,
    this.hint,
    this.obscureText = false,
    this.keyboardType,
    this.textInputAction,
    this.onChanged,
    this.validator,
    this.errorText,
    this.prefixIcon,
    this.suffixIcon,
    this.maxLines = 1,
    this.textCapitalization = TextCapitalization.none,
    this.onFieldSubmitted,
  });

  final TextEditingController controller;
  final String? label;
  final String? hint;
  final bool obscureText;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final ValueChanged<String>? onChanged;
  final FormFieldValidator<String>? validator;
  final String? errorText;
  final IconData? prefixIcon;
  final Widget? suffixIcon;
  final int maxLines;
  final TextCapitalization textCapitalization;
  final ValueChanged<String>? onFieldSubmitted;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      obscureText: obscureText,
      keyboardType: keyboardType,
      textInputAction: textInputAction,
      onChanged: onChanged,
      validator: validator,
      onFieldSubmitted: onFieldSubmitted,
      maxLines: maxLines,
      textCapitalization: textCapitalization,
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        prefixIcon: prefixIcon != null ? Icon(prefixIcon, size: 20) : null,
        suffixIcon: suffixIcon,
        errorText: errorText,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: AppDimensions.s4,
          vertical: AppDimensions.s4,
        ),
      ),
    );
  }
}
