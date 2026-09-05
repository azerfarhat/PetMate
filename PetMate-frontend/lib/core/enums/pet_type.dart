/// Type of pet.
///
/// Values follow the backend contract (`PetType`), which currently supports
/// dogs, cats, birds, rabbits, hamsters and a generic `OTHER` fallback.
enum PetType {
  dog('DOG'),
  cat('CAT'),
  bird('BIRD'),
  rabbit('RABBIT'),
  hamster('HAMSTER'),
  other('OTHER');

  const PetType(this.value);

  /// Raw value used on the wire / API contract.
  final String value;

  static PetType fromValue(String value) {
    return PetType.values.firstWhere(
      (type) => type.value == value,
      orElse: () => PetType.other,
    );
  }
}