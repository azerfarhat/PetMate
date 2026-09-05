import '../../../../core/enums/energy_level.dart';
import '../../../../core/enums/pet_gender.dart';
import '../../../../core/enums/pet_type.dart';
import '../entities/pet.dart';
import '../repositories/pet_repository.dart';

/// Replaces an existing pet profile (full PUT on the backend).
class UpdatePet {
  const UpdatePet(this._repository);

  final PetRepository _repository;

  Future<Pet> call({
    required String petId,
    required String name,
    required PetType type,
    required PetGender gender,
    String? breed,
    required int ageYears,
    EnergyLevel? energyLevel,
    String? bio,
    List<String>? photos,
    bool? isVaccinated,
    bool? isNeutered,
  }) {
    return _repository.updatePet(
      petId: petId,
      name: name,
      type: type,
      gender: gender,
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