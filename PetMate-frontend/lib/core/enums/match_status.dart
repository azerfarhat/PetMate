/// Status of a match between two users.
enum MatchStatus {
  pending('pending'),
  matched('matched'),
  unmatched('unmatched');

  const MatchStatus(this.value);

  /// Raw value used on the wire / API contract.
  final String value;

  static MatchStatus fromValue(String value) {
    return MatchStatus.values.firstWhere(
      (status) => status.value == value,
      orElse: () => MatchStatus.pending,
    );
  }
}
