import 'package:flutter/foundation.dart';

import '../../../core/errors/failures.dart';
import '../../../core/enums/energy_level.dart';
import '../../../core/enums/pet_gender.dart';
import '../../../core/enums/pet_type.dart';
import '../domain/entities/pet.dart';
import '../domain/usecases/create_pet.dart';
import '../domain/usecases/delete_pet.dart';
import '../domain/usecases/get_my_pets.dart';
import '../domain/usecases/update_pet.dart';

/// Manages pet list/profile UI state.
class PetController extends ChangeNotifier {
  PetController({
    required GetMyPets getMyPets,
    required CreatePet createPet,
    required UpdatePet updatePet,
    required DeletePet deletePet,
  })  : _getMyPets = getMyPets,
        _createPet = createPet,
        _updatePet = updatePet,
        _deletePet = deletePet;

  final GetMyPets _getMyPets;
  final CreatePet _createPet;
  final UpdatePet _updatePet;
  final DeletePet _deletePet;

  List<Pet> _pets = [];
  List<Pet> get pets => List.unmodifiable(_pets);

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  /// Loads all pets owned by the current user.
  Future<void> loadMyPets() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();
    try {
      _pets = await _getMyPets();
    } on Failure catch (failure) {
      _errorMessage = failure.message;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Creates a new pet and refreshes the list.
  Future<bool> createPet({
    required String name,
    required PetType type,
    required PetGender gender,
    String? breed,
    required int ageYears,
    EnergyLevel? energyLevel,
    String? bio,
    List<String>? photos,
  }) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();
    try {
      final pet = await _createPet(
        name: name,
        type: type,
        gender: gender,
        breed: breed,
        ageYears: ageYears,
        energyLevel: energyLevel,
        bio: bio,
        photos: photos,
      );
      _pets = [..._pets, pet];
      return true;
    } on Failure catch (failure) {
      _errorMessage = failure.message;
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Deletes a pet and removes it from the local list.
  Future<bool> deletePet(String petId) async {
    _errorMessage = null;
    try {
      await _deletePet(petId);
      _pets = _pets.where((p) => p.id != petId).toList();
      notifyListeners();
      return true;
    } on Failure catch (failure) {
      _errorMessage = failure.message;
      notifyListeners();
      return false;
    }
  }
}
