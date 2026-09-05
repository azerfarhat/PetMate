import '../../../pets/data/dtos/pet_request_dto.dart';

/// Request body for updating the current user's profile.
///
/// Follows the backend `UpdateProfileRequest` contract: full replacement of
/// the personal fields plus the complete list of pets (at least one required).
class UserProfileUpdateRequestDto {
  const UserProfileUpdateRequestDto({
    required this.firstName,
    required this.lastName,
    this.profilePicture,
    this.bio,
    this.latitude,
    this.longitude,
    this.searchRadius,
    required this.pets,
  });

  final String firstName;
  final String lastName;
  final String? profilePicture;
  final String? bio;
  final double? latitude;
  final double? longitude;
  final int? searchRadius;
  final List<PetRequestDto> pets;

  Map<String, dynamic> toJson() {
    return {
      'firstName': firstName,
      'lastName': lastName,
      'profilePicture': profilePicture,
      'bio': bio,
      'latitude': latitude,
      'longitude': longitude,
      'searchRadius': searchRadius,
      'pets': pets.map((pet) => pet.toJson()).toList(),
    };
  }
}