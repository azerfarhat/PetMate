import '../entities/pet.dart';
import '../repositories/pet_repository.dart';

/// Fetches a single pet by id.
class GetPet {
  const GetPet(this._repository);

  final PetRepository _repository;

  Future<Pet> call(String petId) => _repository.getPet(petId);
}
