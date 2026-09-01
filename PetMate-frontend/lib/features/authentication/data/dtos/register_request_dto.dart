/// Request body for the register endpoint.
class RegisterRequestDto {
  const RegisterRequestDto({
    required this.email,
    required this.password,
    required this.displayName,
  });

  final String email;
  final String password;
  final String displayName;

  Map<String, dynamic> toJson() {
    return {
      'email': email,
      'password': password,
      'display_name': displayName,
    };
  }
}
