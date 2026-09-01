import '../../../../core/enums/energy_level.dart';
import '../../../../core/enums/pet_gender.dart';
import '../../../../core/enums/pet_type.dart';

/// Request body for creating a pet profile.
class CreatePetRequestDto {
  const CreatePetRequestDto({
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
  });

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

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'type': type.value,
      'gender': gender.value,
      'breed': breed,
      'age_years': ageYears,
      'energy_level': energyLevel?.value,
      'bio': bio,
      'photos': photos,
      'is_vaccinated': isVaccinated,
      'is_neutered': isNeutered,
    };
  }
}
