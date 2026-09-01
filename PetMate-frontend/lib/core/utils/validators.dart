/// Input validation helpers used across forms.
///
/// Keep validation logic centralized so it can be unit tested independently
/// of widgets.
abstract final class Validators {
  Validators._();

  static final RegExp _emailRegExp = RegExp(
    r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$',
  );

  /// Returns an error message when [value] is not a valid email, else `null`.
  static String? email(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Please enter your email address.';
    }
    if (!_emailRegExp.hasMatch(value.trim())) {
      return 'Please enter a valid email address.';
    }
    return null;
  }

  /// Returns an error message when [value] is not a valid password, else `null`.
  static String? password(String? value, {int minLength = 8}) {
    final v = value ?? '';
    if (v.isEmpty) {
      return 'Please enter a password.';
    }
    if (v.length < minLength) {
      return 'Password must be at least $minLength characters.';
    }
    return null;
  }

  /// Requires a non-empty value.
  static String? required(String? value, {String message = 'This field is required.'}) {
    if (value == null || value.trim().isEmpty) {
      return message;
    }
    return null;
  }

  /// Validates a pet name (letters, spaces, hyphens).
  static String? petName(String? value) {
    return required(value, message: 'Please enter your pet\'s name.');
  }

  /// Requires a numeric value greater than zero.
  static String? positiveNumber(String? value, {String fieldName = 'Value'}) {
    final v = double.tryParse(value ?? '');
    if (v == null || v <= 0) {
      return '$fieldName must be a positive number.';
    }
    return null;
  }
}
