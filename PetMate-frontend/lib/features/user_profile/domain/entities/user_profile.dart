/// A user's profile in the Domain layer.
///
/// Naming follows the app's vocabulary: the backend splits the name into
/// `firstName`/`lastName`, so the display name and avatar are derived here
/// instead of being stored as their own fields.
class UserProfile {
  const UserProfile({
    required this.id,
    required this.firstName,
    this.lastName,
    this.email,
    this.profilePicture,
    this.bio,
    this.latitude,
    this.longitude,
    this.searchRadius,
    this.isVerified = false,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String firstName;
  final String? lastName;
  final String? email;
  final String? profilePicture;
  final String? bio;
  final double? latitude;
  final double? longitude;
  final int? searchRadius;
  final bool isVerified;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  /// The full display name, e.g. `Jane Doe`.
  String get displayName {
    final parts = [firstName.trim(), lastName?.trim() ?? '']
        .where((part) => part.isNotEmpty);
    return parts.join(' ');
  }

  /// Avatar URL, if any.
  String? get avatarUrl => profilePicture;

  /// Date the account joined, if known.
  DateTime? get joinedAt => createdAt;
}