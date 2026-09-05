/// Photo of a pet in a response payload.
///
/// Matches the backend `PetPhotoResponse` contract. The Domain layer only
/// needs the URL strings; the extra metadata is kept here so the wire format
/// maps 1:1 and no information is lost during parsing.
class PetPhotoDto {
  const PetPhotoDto({
    this.id,
    required this.url,
    this.primaryPhoto = false,
    this.displayOrder,
  });

  final int? id;
  final String url;
  final bool primaryPhoto;
  final int? displayOrder;

  factory PetPhotoDto.fromJson(Map<String, dynamic> json) {
    return PetPhotoDto(
      id: (json['id'] as num?)?.toInt(),
      url: json['url'] as String? ?? '',
      primaryPhoto: json['primaryPhoto'] as bool? ?? false,
      displayOrder: (json['displayOrder'] as num?)?.toInt(),
    );
  }
}