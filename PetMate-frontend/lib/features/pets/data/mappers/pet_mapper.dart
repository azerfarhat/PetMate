import '../../domain/entities/pet.dart';
import '../dtos/pet_response_dto.dart';

/// Maps between pet DTOs/models and Domain entities.
abstract final class PetMapper {
  PetMapper._();

  /// Converts a [PetResponseDto] into a Domain [Pet].
  static Pet toEntity(PetResponseDto dto) {
    return Pet(
      id: dto.id,
      ownerId: dto.ownerId,
      name: dto.name,
      type: dto.type,
      gender: dto.gender,
      breed: dto.breed,
      ageYears: dto.ageYears,
      energyLevel: dto.energyLevel,
      bio: dto.bio,
      photos: dto.photos,
      isVaccinated: dto.isVaccinated,
      isNeutered: dto.isNeutered,
      createdAt: dto.createdAt != null ? DateTime.tryParse(dto.createdAt!) : null,
    );
  }

  /// Converts a list of DTOs into a list of entities.
  static List<Pet> toEntityList(List<PetResponseDto> dtos) {
    return dtos.map(toEntity).toList();
  }
}
