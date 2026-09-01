/// Date and time formatting helpers.
///
/// Note: named `AppDateUtils` (not `DateUtils`) to avoid clashing with
/// Flutter's own `DateUtils` class from the material library.
abstract final class AppDateUtils {
  AppDateUtils._();

  static const List<String> _months = [
    'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
    'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
  ];

  /// Returns a humanized relative representation of [date] such as
  /// "just now", "5m ago", "2h ago", "3d ago".
  static String timeAgo(DateTime date, {DateTime? now}) {
    final reference = now ?? DateTime.now();
    final difference = reference.difference(date);

    if (difference.inSeconds < 60) {
      return 'just now';
    }
    if (difference.inMinutes < 60) {
      return '${difference.inMinutes}m ago';
    }
    if (difference.inHours < 24) {
      return '${difference.inHours}h ago';
    }
    if (difference.inDays < 7) {
      return '${difference.inDays}d ago';
    }
    return _formatDate(date);
  }

  /// Formats a [DateTime] as `dd MMM yyyy`.
  static String _formatDate(DateTime date) {
    return '${date.day} ${_months[date.month - 1]} ${date.year}';
  }

  /// Formats a [DateTime] as a time such as `14:30`.
  static String formatTime(DateTime date) {
    final h = date.hour.toString().padLeft(2, '0');
    final m = date.minute.toString().padLeft(2, '0');
    return '$h:$m';
  }

  /// Formats a full date and time.
  static String formatDateTime(DateTime date) {
    return '${_formatDate(date)} · ${formatTime(date)}';
  }
}
