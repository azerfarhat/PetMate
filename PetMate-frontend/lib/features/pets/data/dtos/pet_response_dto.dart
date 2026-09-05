import '../../../../core/enums/energy_level.dart';
import '../../../../core/enums/pet_gender.dart';
import '../../../../core/enums/pet_type.dart';
import 'pet_photo_dto.dart';

/// Response body for a pet profile endpoint.
///
/// Follows the backend `PetResponse` contract exactly (camelCase, numeric
/// identifiers, `age`, `description`, `vaccinated/neutered` and photos as a
/// list of objects). Domain-friendly naming is applied by [PetMapper].
class PetResponseDto {
  const PetResponseDto({
    required this.id,
    required this.ownerId,
    required this.name,
    required this.type,
    required this.gender,
    this.breed,
    required this.age,
    this.energyLevel,
    this.description,
    this.vaccinated = false,
    this.neutered = false,
    this.photos = const [],
  });

  final int id;
  final int ownerId;
  final String name;
  final PetType type;
  final PetGender gender;
  final String? breed;

  /// Age in years (backend `age`).
  final int age;
  final EnergyLevel? energyLevel;
  final String? description;
  final bool vaccinated;
  final bool neutered;
  final List<PetPhotoDto> photos;

  factory PetResponseDto.fromJson(Map<String, dynamic> json) {
    return PetResponseDto(
      id: (json['id'] as num).toInt(),
      ownerId: (json['ownerId'] as num?)?.toInt() ?? 0,
      name: json['name'] as String,
      type: PetType.fromValue(json['type'] as String? ?? ''),
      gender: PetGender.fromValue(json['gender'] as String? ?? ''),
      breed: json['breed'] as String?,
      age: (json['age'] as num?)?.toInt() ?? 0,
      energyLevel: json['energyLevel'] != null
          ? EnergyLevel.fromValue(json['energyLevel'] as String)
          : null,
      description: json['description'] as String?,
      vaccinated: json['vaccinated'] as bool? ?? false,
      neutered: json['neutered'] as bool? ?? false,
      photos: (json['photos'] as List<dynamic>? ?? [])
          .whereType<Map<String, dynamic>>()
          .map(PetPhotoDto.fromJson)
          .toList(),
    );
  }
}