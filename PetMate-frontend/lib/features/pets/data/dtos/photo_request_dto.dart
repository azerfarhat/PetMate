/// Photo payload in a pet write request.
///
/// Matches the backend `PhotoUpdateRequest` contract: an optional `id` for
/// existing photos, a mandatory HTTP(S) URL and ordering metadata.
class PhotoRequestDto {
  const PhotoRequestDto({
    this.id,
    required this.url,
    this.primaryPhoto = false,
    this.displayOrder,
  });

  final int? id;
  final String url;
  final bool primaryPhoto;
  final int? displayOrder;

  Map<String, dynamic> toJson() {
    return {
      if (id != null) 'id': id,
      'url': url,
      'primaryPhoto': primaryPhoto,
      'displayOrder': displayOrder,
    };
  }
}