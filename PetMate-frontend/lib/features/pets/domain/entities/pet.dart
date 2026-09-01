import '../../../core/enums/energy_level.dart';
import '../../../core/enums/pet_gender.dart';
import '../../../core/enums/pet_type.dart';

/// A pet owned by a user in the Domain layer.
class Pet {
  const Pet({
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
  final DateTime? createdAt;
}
