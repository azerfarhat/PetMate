import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_client.dart';
import '../dtos/pet_request_dto.dart';
import '../dtos/pet_response_dto.dart';

/// Remote datasource for pets.
class PetRemoteDataSource {
  PetRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<List<PetResponseDto>> getMyPets() async {
    final response = await _apiClient.get(ApiConstants.petsBasePath);
    final list = (response as List<dynamic>).cast<Map<String, dynamic>>();
    return list.map(PetResponseDto.fromJson).toList();
  }

  Future<List<PetResponseDto>> getUserPets(String userId) async {
    final response = await _apiClient.get(
      ApiConstants.petsBasePath,
      queryParameters: {'owner_id': userId},
    );
    final list = (response as List<dynamic>).cast<Map<String, dynamic>>();
    return list.map(PetResponseDto.fromJson).toList();
  }

  Future<PetResponseDto> getPet(String petId) async {
    final response = await _apiClient.get(
      '${ApiConstants.petsBasePath}/$petId',
    );
    return PetResponseDto.fromJson(response as Map<String, dynamic>);
  }

  Future<PetResponseDto> createPet(PetRequestDto dto) async {
    final response = await _apiClient.post(
      ApiConstants.petsBasePath,
      body: dto.toJson(),
    );
    return PetResponseDto.fromJson(response as Map<String, dynamic>);
  }

  Future<PetResponseDto> updatePet(String petId, PetRequestDto dto) async {
    final response = await _apiClient.put(
      '${ApiConstants.petsBasePath}/$petId',
      body: dto.toJson(),
    );
    return PetResponseDto.fromJson(response as Map<String, dynamic>);
  }

  Future<void> deletePet(String petId) async {
    await _apiClient.delete('${ApiConstants.petsBasePath}/$petId');
  }
}