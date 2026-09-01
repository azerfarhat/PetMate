/// A permission that the application may request from the user.
enum AppPermission {
  location,
  notifications,
  camera,
  photos,
}

/// Abstraction over platform permission requesting.
///
/// [PermissionService] keeps permission handling behind a contract so the
/// Presentation layer is decoupled from the platform-specific permission
/// packages. A concrete implementation backed by `permission_handler` will be
/// provided when the permission stack is finalized.
abstract interface class PermissionService {
  /// Requests [permission] and returns `true` when granted.
  Future<bool> request(AppPermission permission);

  /// Checks the current status of [permission].
  Future<bool> isGranted(AppPermission permission);
}
