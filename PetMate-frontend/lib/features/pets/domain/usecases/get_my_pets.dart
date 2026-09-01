import '../entities/pet.dart';
import '../repositories/pet_repository.dart';

/// Fetches all pets owned by the current user.
class GetMyPets {
  const GetMyPets(this._repository);

  final PetRepository _repository;

  Future<List<Pet>> call() => _repository.getMyPets();
}
