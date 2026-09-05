/// Gender of a pet.
///
/// Values follow the backend contract (`PetGender`); `unknown` is kept for
/// profiles that do not state a gender, with a safe fallback to `male` for
/// unexpected wire values.
enum PetGender {
  male('MALE'),
  female('FEMALE'),
  unknown('UNKNOWN');

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