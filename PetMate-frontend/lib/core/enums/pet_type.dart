/// Type of pet.
enum PetType {
  dog('dog'),
  cat('cat'),
  bird('bird'),
  rabbit('rabbit'),
  fish('fish'),
  hamster('hamster'),
  reptile('reptile'),
  other('other');

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
