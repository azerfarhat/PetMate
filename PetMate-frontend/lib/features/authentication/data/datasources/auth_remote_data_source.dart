import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_client.dart';
import '../../../pets/data/dtos/pet_request_dto.dart';
import '../dtos/auth_response_dto.dart';
import '../dtos/login_request_dto.dart';
import '../dtos/register_request_dto.dart';

/// Remote datasource for authentication.
///
/// Communicates with the backend auth endpoints. Contains no business logic.
class AuthRemoteDataSource {
  AuthRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<AuthResponseDto> login({
    required String email,
    required String password,
  }) async {
    final body = LoginRequestDto(email: email, password: password).toJson();
    final response = await _apiClient.post(
      ApiConstants.loginEndpoint,
      body: body,
    );
    return AuthResponseDto.fromJson(response as Map<String, dynamic>);
  }

  Future<AuthResponseDto> register({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
    List<PetRequestDto> pets = const [],
  }) async {
    final body = RegisterRequestDto(
      email: email,
      password: password,
      firstName: firstName,
      lastName: lastName,
      pets: pets,
    ).toJson();
    final response = await _apiClient.post(
      ApiConstants.registerEndpoint,
      body: body,
    );
    return AuthResponseDto.fromJson(response as Map<String, dynamic>);
  }

  Future<void> logout() async {
    await _apiClient.post(ApiConstants.logoutEndpoint);
  }
}