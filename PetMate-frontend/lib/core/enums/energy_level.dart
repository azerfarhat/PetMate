/// Energy level of a pet.
enum EnergyLevel {
  low('low'),
  moderate('moderate'),
  high('high');

  const EnergyLevel(this.value);

  /// Raw value used on the wire / API contract.
  final String value;

  static EnergyLevel fromValue(String value) {
    return EnergyLevel.values.firstWhere(
      (level) => level.value == value,
      orElse: () => EnergyLevel.moderate,
    );
  }
}
