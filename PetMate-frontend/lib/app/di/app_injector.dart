import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';
import '../../core/storage/local_storage.dart';
import '../../features/authentication/data/datasources/auth_remote_data_source.dart';
import '../../features/authentication/data/repositories/auth_repository_impl.dart';
import '../../features/authentication/domain/repositories/auth_repository.dart';
import '../../features/authentication/domain/usecases/get_current_user.dart';
import '../../features/authentication/domain/usecases/login.dart';
import '../../features/authentication/domain/usecases/logout.dart';
import '../../features/authentication/domain/usecases/register.dart';
import '../../features/authentication/presentation/controllers/auth_controller.dart';
import '../../features/user_profile/data/datasources/user_profile_remote_data_source.dart';
import '../../features/user_profile/data/repositories/user_profile_repository_impl.dart';
import '../../features/user_profile/domain/repositories/user_profile_repository.dart';
import '../../features/user_profile/domain/usecases/get_my_profile.dart';
import '../../features/user_profile/domain/usecases/update_user_profile.dart';
import '../../features/user_profile/presentation/controllers/user_profile_controller.dart';
import '../../features/pets/data/datasources/pet_remote_data_source.dart';
import '../../features/pets/data/repositories/pet_repository_impl.dart';
import '../../features/pets/domain/repositories/pet_repository.dart';
import '../../features/pets/domain/usecases/update_pet.dart';
import '../../features/pets/domain/usecases/create_pet.dart';
import '../../features/pets/domain/usecases/get_my_pets.dart';
import '../../features/pets/domain/usecases/get_pet.dart';
import '../../features/pets/domain/usecases/get_user_pets.dart';
import '../../features/pets/domain/usecases/delete_pet.dart';

/// Manual dependency-injection container (composition root).
///
/// This lightweight service locator composes repositories, use cases and
/// controllers at a single place. It keeps feature code free of wiring and
/// makes it easy to replace infrastructure (e.g. swapping the HTTP stack)
/// without touching the Presentation or Domain layers.
///
/// The container is intentionally small for the MVP. If it grows, replace it
/// with a dedicated DI package such as `get_it`.
class AppInjector {
  AppInjector._();

  static final AppInjector instance = AppInjector._();

  // ---- Core infrastructure -------------------------------------------------
  late final LocalStorage localStorage = _InMemoryLocalStorage();

  late final ApiClient apiClient = ApiClient(
    tokenProvider: () => localStorage.read(ApiConstants.authTokenKey),
  );

  // ---- Authentication ------------------------------------------------------
  late final UserProfileRemoteDataSource userProfileRemoteDataSource =
      UserProfileRemoteDataSource(apiClient);

  late final UserProfileRepository userProfileRepository =
      UserProfileRepositoryImpl(userProfileRemoteDataSource);

  late final AuthRepository authRepository = AuthRepositoryImpl(
    AuthRemoteDataSource(apiClient),
    localStorage,
    userProfileRemoteDataSource,
  );

  late final Login login = Login(authRepository);
  late final Register register = Register(authRepository);
  late final Logout logout = Logout(authRepository);
  late final GetCurrentUser getCurrentUser = GetCurrentUser(authRepository);

  late final AuthController authController = AuthController(
    login: login,
    register: register,
    logout: logout,
    getCurrentUser: getCurrentUser,
  );

  // ---- Pets -----------------------------------------------------------------
  late final PetRemoteDataSource petRemoteDataSource =
      PetRemoteDataSource(apiClient);

  late final PetRepository petRepository =
      PetRepositoryImpl(petRemoteDataSource);

  late final GetMyPets getMyPets = GetMyPets(petRepository);
  late final GetUserPets getUserPets = GetUserPets(petRepository);
  late final GetPet getPet = GetPet(petRepository);
  late final CreatePet createPet = CreatePet(petRepository);
  late final UpdatePet updatePet = UpdatePet(petRepository);
  late final DeletePet deletePet = DeletePet(petRepository);

  // ---- User profile ---------------------------------------------------------
  late final GetMyProfile getMyProfile = GetMyProfile(userProfileRepository);
  late final UpdateUserProfile updateUserProfile =
      UpdateUserProfile(userProfileRepository);

  late final UserProfileController userProfileController =
      UserProfileController(
    getMyProfile: getMyProfile,
    updateUserProfile: updateUserProfile,
  );
}

/// In-memory [LocalStorage] implementation.
///
/// Used as a stand-in until a secure storage backend (`flutter_secure_storage`)
/// is integrated. Data is not persisted across launches — do not use in
/// production.
class _InMemoryLocalStorage implements LocalStorage {
  final Map<String, String> _store = {};

  @override
  Future<void> clear() async => _store.clear();

  @override
  Future<void> delete(String key) async => _store.remove(key);

  @override
  Future<String?> read(String key) async => _store[key];

  @override
  Future<void> write(String key, String value) async => _store[key] = value;
}
