/// Energy level of a pet.
///
/// Values follow the backend contract (`EnergyLevel`). Note that the backend
/// spells the middle level `MEDIUM` while the app vocabulary uses `moderate`;
/// the enum keeps the app-facing name and maps the wire value separately.
enum EnergyLevel {
  low('LOW'),
  moderate('MEDIUM'),
  high('HIGH');

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