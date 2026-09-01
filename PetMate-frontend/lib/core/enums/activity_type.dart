/// Type of activity a user can organise or join.
enum ActivityType {
  walk('walk'),
  picnic('picnic'),
  playdate('playdate'),
  hiking('hiking'),
  parkVisit('park_visit'),
  beachOuting('beach_outing'),
  event('event'),
  other('other');

  const ActivityType(this.value);

  /// Raw value used on the wire / API contract.
  final String value;

  static ActivityType fromValue(String value) {
    return ActivityType.values.firstWhere(
      (type) => type.value == value,
      orElse: () => ActivityType.other,
    );
  }
}
