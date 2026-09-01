/// A user's public profile in the Domain layer.
class UserProfile {
  const UserProfile({
    required this.id,
    required this.displayName,
    this.bio,
    this.avatarUrl,
    this.city,
    this.dateOfBirth,
    this.isVerified = false,
    this.joinedAt,
  });

  final String id;
  final String displayName;
  final String? bio;
  final String? avatarUrl;
  final String? city;
  final DateTime? dateOfBirth;
  final bool isVerified;
  final DateTime? joinedAt;
}
