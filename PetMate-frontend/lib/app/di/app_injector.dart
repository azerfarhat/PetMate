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
  late final AuthRepository authRepository = AuthRepositoryImpl(
    AuthRemoteDataSource(apiClient),
    localStorage,
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
