import '../../../core/enums/energy_level.dart';
import '../entities/pet.dart';
import '../repositories/pet_repository.dart';

/// Updates an existing pet profile.
class UpdatePet {
  const UpdatePet(this._repository);

  final PetRepository _repository;

  Future<Pet> call({
    required String petId,
    String? name,
    String? breed,
    int? ageYears,
    EnergyLevel? energyLevel,
    String? bio,
    List<String>? photos,
    bool? isVaccinated,
    bool? isNeutered,
  }) {
    return _repository.updatePet(
      petId: petId,
      name: name,
      breed: breed,
      ageYears: ageYears,
      energyLevel: energyLevel,
      bio: bio,
      photos: photos,
      isVaccinated: isVaccinated,
      isNeutered: isNeutered,
    );
  }
}
