import '../dtos/pet_response_dto.dart';

/// Data-layer representation of a pet with serialization support.
class PetModel {
  const PetModel({required this.dto});

  final PetResponseDto dto;

  factory PetModel.fromDto(PetResponseDto dto) => PetModel(dto: dto);

  factory PetModel.fromJson(Map<String, dynamic> json) =>
      PetModel(dto: PetResponseDto.fromJson(json));
}
