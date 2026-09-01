import 'package:equatable/equatable.dart';

/// Domain-level error representation.
///
/// The Domain layer communicates errors using [Failure] objects rather than
/// raw [Exception]s. All failures extend [Failure] so they can be matched
/// and displayed uniformly by the Presentation layer.
///
/// Note: In this scaffold, [equatable] is a lightweight dependency used only
/// when added. When the project dependencies are finalized, add
/// `equatable: ^2.0.5` to pubspec to activate value equality. Until then this
/// file compiles without it (the base class uses default equality semantics).
abstract class Failure extends Equatable {
  const Failure({required this.message});

  /// Human-readable error message suitable for display.
  final String message;

  @override
  List<Object?> get props => [message];
}

/// Failure caused by lack of network connectivity.
class NetworkFailure extends Failure {
  const NetworkFailure({super.message = 'No internet connection.'});
}

/// Failure caused by token expiration or invalid credentials.
class UnauthorizedFailure extends Failure {
  const UnauthorizedFailure({super.message = 'Your session has expired.'});
}

/// Failure caused by insufficient permissions.
class ForbiddenFailure extends Failure {
  const ForbiddenFailure({super.message = 'You cannot perform this action.'});
}

/// Failure caused by a missing resource.
class NotFoundFailure extends Failure {
  const NotFoundFailure({super.message = 'The requested item was not found.'});
}

/// Failure caused by invalid user input.
class ValidationFailure extends Failure {
  const ValidationFailure({
    super.message = 'Please check your input.',
    this.fieldErrors = const {},
  });

  /// Field-level errors keyed by field name.
  final Map<String, String> fieldErrors;

  @override
  List<Object?> get props => [message, fieldErrors];
}

/// Failure caused by a server-side error.
class ServerFailure extends Failure {
  const ServerFailure({super.message = 'Something went wrong. Please retry.'});
}

/// Failure caused by local storage errors.
class CacheFailure extends Failure {
  const CacheFailure({super.message = 'Unable to access local data.'});
}

/// Failure caused by a missing or denied permission.
class PermissionFailure extends Failure {
  const PermissionFailure({
    super.message = 'Permission is required to continue.',
  });
}

/// Catch-all failure for unexpected conditions.
class UnexpectedFailure extends Failure {
  const UnexpectedFailure({super.message = 'An unexpected error occurred.'});
}
