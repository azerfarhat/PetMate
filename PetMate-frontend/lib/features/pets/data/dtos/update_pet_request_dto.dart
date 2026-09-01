import '../../../../core/enums/energy_level.dart';

/// Request body for updating a pet profile. Only non-null fields are sent.
class UpdatePetRequestDto {
  const UpdatePetRequestDto({
    this.name,
    this.breed,
    this.ageYears,
    this.energyLevel,
    this.bio,
    this.photos,
    this.isVaccinated,
    this.isNeutered,
  });

  final String? name;
  final String? breed;
  final int? ageYears;
  final EnergyLevel? energyLevel;
  final String? bio;
  final List<String>? photos;
  final bool? isVaccinated;
  final bool? isNeutered;

  Map<String, dynamic> toJson() {
    return {
      if (name != null) 'name': name,
      if (breed != null) 'breed': breed,
      if (ageYears != null) 'age_years': ageYears,
      if (energyLevel != null) 'energy_level': energyLevel!.value,
      if (bio != null) 'bio': bio,
      if (photos != null) 'photos': photos,
      if (isVaccinated != null) 'is_vaccinated': isVaccinated,
      if (isNeutered != null) 'is_neutered': isNeutered,
    };
  }
}
