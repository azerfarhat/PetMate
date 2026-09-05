import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_client.dart';
import '../dtos/user_profile_dto.dart';
import '../dtos/user_profile_update_request_dto.dart';

/// Remote datasource for user profiles.
///
/// Communicates with the backend and returns DTOs. No business logic here.
class UserProfileRemoteDataSource {
  UserProfileRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<UserProfileDto> getMyProfile() async {
    final response = await _apiClient.get(ApiConstants.currentUserEndpoint);
    return UserProfileDto.fromJson(response as Map<String, dynamic>);
  }

  Future<UserProfileDto> getUserProfile(String userId) async {
    final response = await _apiClient.get(
      '${ApiConstants.usersBasePath}/$userId',
    );
    return UserProfileDto.fromJson(response as Map<String, dynamic>);
  }

  Future<UserProfileDto> updateProfile(UserProfileUpdateRequestDto dto) async {
    final response = await _apiClient.put(
      ApiConstants.currentUserEndpoint,
      body: dto.toJson(),
    );
    return UserProfileDto.fromJson(response as Map<String, dynamic>);
  }
}