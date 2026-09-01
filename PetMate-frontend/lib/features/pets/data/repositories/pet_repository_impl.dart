import '../../../core/enums/energy_level.dart';
import '../../../core/enums/pet_gender.dart';
import '../../../core/enums/pet_type.dart';
import '../../domain/entities/pet.dart';
import '../../domain/repositories/pet_repository.dart';
import '../datasources/pet_remote_data_source.dart';
import '../dtos/create_pet_request_dto.dart';
import '../dtos/update_pet_request_dto.dart';
import '../mappers/pet_mapper.dart';

/// Implementation of [PetRepository].
class PetRepositoryImpl implements PetRepository {
  PetRepositoryImpl(this._remoteDataSource);

  final PetRemoteDataSource _remoteDataSource;

  @override
  Future<List<Pet>> getMyPets() async {
    final dtos = await _remoteDataSource.getMyPets();
    return PetMapper.toEntityList(dtos);
  }

  @override
  Future<List<Pet>> getUserPets(String userId) async {
    final response = await _remoteDataSource.getUserPets(userId);
    return PetMapper.toEntityList(response);
  }

  @override
  Future<Pet> getPet(String petId) async {
    final dto = await _remoteDataSource.getPet(petId);
    return PetMapper.toEntity(dto);
  }

  @override
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
  }) async {
    final request = CreatePetRequestDto(
      name: name,
      type: type,
      gender: gender,
      breed: breed,
      ageYears: ageYears,
      energyLevel: energyLevel,
      bio: bio,
      photos: photos ?? const [],
      isVaccinated: isVaccinated,
      isNeutered: isNeutered,
    );
    final dto = await _remoteDataSource.createPet(request);
    return PetMapper.toEntity(dto);
  }

  @override
  Future<Pet> updatePet({
    required String petId,
    String? name,
    String? breed,
    int? ageYears,
    EnergyLevel? energyLevel,
    String? bio,
    List<String>? photos,
    bool? isVaccinated,
    bool? isNeutered,
  }) async {
    final request = UpdatePetRequestDto(
      name: name,
      breed: breed,
      ageYears: ageYears,
      energyLevel: energyLevel,
      bio: bio,
      photos: photos,
      isVaccinated: isVaccinated,
      isNeutered: isNeutered,
    );
    final dto = await _remoteDataSource.updatePet(petId, request);
    return PetMapper.toEntity(dto);
  }

  @override
  Future<void> deletePet(String petId) {
    return _remoteDataSource.deletePet(petId);
  }
}
