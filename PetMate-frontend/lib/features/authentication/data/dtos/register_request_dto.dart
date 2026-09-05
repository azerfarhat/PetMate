import '../../../pets/data/dtos/pet_request_dto.dart';

/// Request body for the register endpoint.
///
/// Follows the backend `RegisterRequest` contract: the app must supply a
/// first name and a last name (no single "display name" field). Pets are
/// required by the backend (at least one), so the wizard payload is expected;
/// pass an empty list only until the pet wizard is wired up.
class RegisterRequestDto {
  const RegisterRequestDto({
    required this.email,
    required this.password,
    required this.firstName,
    required this.lastName,
    this.pets = const [],
  });

  final String email;
  final String password;
  final String firstName;
  final String lastName;
  final List<PetRequestDto> pets;

  Map<String, dynamic> toJson() {
    return {
      'email': email,
      'password': password,
      'firstName': firstName,
      'lastName': lastName,
      'pets': pets.map((pet) => pet.toJson()).toList(),
    };
  }
}