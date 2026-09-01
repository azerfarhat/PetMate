import '../../../../core/enums/energy_level.dart';
import '../../../../core/enums/pet_gender.dart';
import '../../../../core/enums/pet_type.dart';

/// Response body for a pet profile endpoint.
class PetResponseDto {
  const PetResponseDto({
    required this.id,
    required this.ownerId,
    required this.name,
    required this.type,
    required this.gender,
    this.breed,
    required this.ageYears,
    this.energyLevel,
    this.bio,
    this.photos = const [],
    this.isVaccinated = false,
    this.isNeutered = false,
    this.createdAt,
  });

  final String id;
  final String ownerId;
  final String name;
  final PetType type;
  final PetGender gender;
  final String? breed;
  final int ageYears;
  final EnergyLevel? energyLevel;
  final String? bio;
  final List<String> photos;
  final bool isVaccinated;
  final bool isNeutered;
  final String? createdAt;

  factory PetResponseDto.fromJson(Map<String, dynamic> json) {
    return PetResponseDto(
      id: json['id'] as String,
      ownerId: json['owner_id'] as String,
      name: json['name'] as String,
      type: PetType.fromValue(json['type'] as String? ?? ''),
      gender: PetGender.fromValue(json['gender'] as String? ?? ''),
      breed: json['breed'] as String?,
      ageYears: json['age_years'] as int? ?? 0,
      energyLevel: json['energy_level'] != null
          ? EnergyLevel.fromValue(json['energy_level'] as String)
          : null,
      bio: json['bio'] as String?,
      photos: (json['photos'] as List<dynamic>? ?? []).cast<String>(),
      isVaccinated: json['is_vaccinated'] as bool? ?? false,
      isNeutered: json['is_neutered'] as bool? ?? false,
      createdAt: json['created_at'] as String?,
    );
  }
}
