/// Gender of a pet.
enum PetGender {
  male('male'),
  female('female');

  const PetGender(this.value);

  /// Raw value used on the wire / API contract.
  final String value;

  static PetGender fromValue(String value) {
    return PetGender.values.firstWhere(
      (gender) => gender.value == value,
      orElse: () => PetGender.male,
    );
  }
}
