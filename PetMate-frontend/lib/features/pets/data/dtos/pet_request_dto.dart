import '../../../../core/enums/energy_level.dart';
import '../../../../core/enums/pet_gender.dart';
import '../../../../core/enums/pet_type.dart';
import 'photo_request_dto.dart';

/// Full pet payload shared by every write endpoint.
///
/// Matches the backend `PetRequest` contract used by `POST /pets` and
/// `PUT /pets/{id}` (a full replacement, no partial PATCH). When the payload
/// is embedded in a profile update the backend `PetUpdateRequest` also
/// accepts an optional `id` to target an existing pet; the id is simply
/// ignored by the other endpoints.
class PetRequestDto {
  const PetRequestDto({
    this.id,
    required this.name,
    required this.type,
    required this.gender,
    this.breed,
    required this.age,
    this.energyLevel,
    this.description,
    this.vaccinated,
    this.neutered,
    this.photos = const [],
  });

  final int? id;
  final String name;
  final PetType type;
  final PetGender gender;
  final String? breed;

  /// Age in years (`age` on the backend, exposed as `ageYears` on the app).
  final int age;
  final EnergyLevel? energyLevel;
  final String? description;
  final bool? vaccinated;
  final bool? neutered;
  final List<PhotoRequestDto> photos;

  Map<String, dynamic> toJson() {
    return {
      if (id != null) 'id': id,
      'name': name,
      'type': type.value,
      'gender': gender.value,
      'breed': breed,
      'age': age,
      'energyLevel': energyLevel?.value,
      'description': description,
      'vaccinated': vaccinated,
      'neutered': neutered,
      'photos': photos.map((photo) => photo.toJson()).toList(),
    };
  }
}