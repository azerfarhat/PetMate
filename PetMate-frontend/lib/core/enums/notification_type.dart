/// Type of notification delivered to a user.
enum NotificationType {
  like('like'),
  match('match'),
  message('message'),
  activity('activity'),
  review('review'),
  system('system');

  const NotificationType(this.value);

  /// Raw value used on the wire / API contract.
  final String value;

  static NotificationType fromValue(String value) {
    return NotificationType.values.firstWhere(
      (type) => type.value == value,
      orElse: () => NotificationType.system,
    );
  }
}
