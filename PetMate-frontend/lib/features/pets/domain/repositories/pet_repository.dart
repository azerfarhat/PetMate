import '../../../../core/enums/energy_level.dart';
import '../../../../core/enums/pet_gender.dart';
import '../../../../core/enums/pet_type.dart';
import '../entities/pet.dart';

/// Repository contract for pet data.
abstract interface class PetRepository {
  /// Fetches all pets owned by the current user.
  Future<List<Pet>> getMyPets();

  /// Fetches all pets of a given user.
  Future<List<Pet>> getUserPets(String userId);

  /// Fetches a single pet by id.
  Future<Pet> getPet(String petId);

  /// Creates a new pet profile.
  Future<Pet> createPet({
    required String name,
    required PetType type,
    required PetGender gender,
    String? breed,
    required int ageYears,
    EnergyLevel? energyLevel,
    String? bio,
    List<String>? photos,
    bool isVaccinated = false,
    bool isNeutered = false,
  });

  /// Replaces an existing pet profile.
  ///
  /// The backend uses a full-replacement PUT, so all core fields are required.
  Future<Pet> updatePet({
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
  });

  /// Deletes a pet profile.
  Future<void> deletePet(String petId);
}