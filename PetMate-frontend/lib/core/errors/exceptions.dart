/// Application-level exceptions.
///
/// These represent exceptional conditions raised within the Data layer
/// (for example by datasources) before being mapped to a [Failure] in the
/// Domain layer.
///
/// Note: the primary error contract used by the Domain layer is [Failure]. 
/// Exceptions are a lower-level mechanism used by datasources and infrastructure.
library;

import 'package:flutter/foundation.dart';

/// Base exception for the application.
class AppException implements Exception {
  const AppException(this.message);

  final String message;

  @override
  String toString() => message;
}

/// Thrown when a network request fails or is unreachable.
class NetworkException extends AppException {
  const NetworkException([super.message = 'A network error occurred.']);
}

/// Thrown when an API returns an unauthorised status.
class UnauthorizedException extends AppException {
  const UnauthorizedException([super.message = 'Unauthenticated request.']);
}

/// Thrown when an API returns a forbidden/resource-denied status.
class ForbiddenException extends AppException {
  const ForbiddenException([super.message = 'Forbidden.']);
}

/// Thrown when an API returns a not-found status.
class NotFoundException extends AppException {
  const NotFoundException([super.message = 'Resource not found.']);
}

/// Thrown when an API returns a validation/bad-request error.
class ValidationException extends AppException {
  const ValidationException([super.message = 'Validation failed.']);

  /// Field-level validation errors keyed by field name.
  final Map<String, String> fieldErrors = const {};
}

/// Thrown when the server returns an unexpected error.
class ServerException extends AppException {
  const ServerException([super.message = 'Server error.']);
}

/// Thrown when local data cannot be read or written.
class CacheException extends AppException {
  const CacheException([super.message = 'Local storage error.']);
}

/// Thrown when a required permission has not been granted.
class PermissionException extends AppException {
  const PermissionException([super.message = 'Permission not granted.']);
}

/// Convenience marker for other unexpected exceptions.
class UnknownException extends AppException {
  const UnknownException([super.message = 'An unexpected error occurred.']);
}
