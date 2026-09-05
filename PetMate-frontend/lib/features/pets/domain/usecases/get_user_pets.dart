import '../entities/pet.dart';
import '../repositories/pet_repository.dart';

/// Fetches all pets owned by a given user.
class GetUserPets {
  const GetUserPets(this._repository);

  final PetRepository _repository;

  Future<List<Pet>> call(String userId) => _repository.getUserPets(userId);
}