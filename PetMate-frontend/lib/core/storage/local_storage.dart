/// Abstraction over secure local storage.
///
/// Sensitive data (such as auth tokens) must be stored securely, not in
/// plain textual local storage. [LocalStorage] defines the operations that
/// infrastructure implementations must provide. A `flutter_secure_storage`
/// backed implementation will be supplied when the storage stack is finalized.
abstract interface class LocalStorage {
  /// Reads a string value for [key], or `null` when absent.
  Future<String?> read(String key);

  /// Writes a string [value] for [key].
  Future<void> write(String key, String value);

  /// Removes the value for [key].
  Future<void> delete(String key);

  /// Removes all stored values.
  Future<void> clear();
}
