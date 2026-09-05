import '../../domain/entities/pet.dart';
import '../dtos/pet_request_dto.dart';
import '../dtos/pet_response_dto.dart';
import '../dtos/photo_request_dto.dart';

/// Maps between pet DTOs and Domain entities.
///
/// The DTOs mirror the backend wire contract; translation to the app's own
/// vocabulary (string ids, `ageYears`, photo URLs) happens exclusively here.
abstract final class PetMapper {
  PetMapper._();

  /// Converts a [PetResponseDto] into a Domain [Pet].
  static Pet toEntity(PetResponseDto dto) {
    return Pet(
      id: dto.id.toString(),
      ownerId: dto.ownerId.toString(),
      name: dto.name,
      type: dto.type,
      gender: dto.gender,
      breed: dto.breed,
      ageYears: dto.age,
      energyLevel: dto.energyLevel,
      bio: dto.description,
      photos: dto.photos.map((photo) => photo.url).toList(),
      isVaccinated: dto.vaccinated,
      isNeutered: dto.neutered,
    );
  }

  /// Converts a list of DTOs into a list of entities.
  static List<Pet> toEntityList(List<PetResponseDto> dtos) {
    return dtos.map(toEntity).toList();
  }

  /// Serializes a Domain [Pet] into the wire payload expected by the backend,
  /// retaining the pet id for profile updates (new pets have a `null` id).
  static PetRequestDto toRequestDto(Pet pet) {
    return PetRequestDto(
      id: int.tryParse(pet.id),
      name: pet.name,
      type: pet.type,
      gender: pet.gender,
      breed: pet.breed,
      age: pet.ageYears,
      energyLevel: pet.energyLevel,
      description: pet.bio,
      vaccinated: pet.isVaccinated,
      neutered: pet.isNeutered,
      photos: pet.photos
          .map((url) => PhotoRequestDto(url: url))
          .toList(),
    );
  }
}