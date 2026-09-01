/// Role of a user within the PawMate application.
enum UserRole {
  standard('standard'),
  premium('premium');

  const UserRole(this.value);

  /// Raw value used on the wire / API contract.
  final String value;

  static UserRole fromValue(String value) {
    return UserRole.values.firstWhere(
      (role) => role.value == value,
      orElse: () => UserRole.standard,
    );
  }
}
