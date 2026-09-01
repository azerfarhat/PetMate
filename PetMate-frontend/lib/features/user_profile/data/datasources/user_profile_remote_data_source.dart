import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_client.dart';
import '../dtos/user_profile_dto.dart';

/// Remote datasource for user profiles.
///
/// Communicates with the backend and returns DTOs. No business logic here.
class UserProfileRemoteDataSource {
  UserProfileRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<UserProfileDto> getMyProfile() async {
    final response = await _apiClient.get(ApiConstants.usersBasePath);
    return UserProfileDto.fromJson(response as Map<String, dynamic>);
  }

  Future<UserProfileDto> getUserProfile(String userId) async {
    final response = await _apiClient.get(
      '${ApiConstants.usersBasePath}/$userId',
    );
    return UserProfileDto.fromJson(response as Map<String, dynamic>);
  }

  Future<UserProfileDto> updateProfile(Map<String, dynamic> body) async {
    final response = await _apiClient.patch(
      ApiConstants.usersBasePath,
      body: body,
    );
    return UserProfileDto.fromJson(response as Map<String, dynamic>);
  }
}
