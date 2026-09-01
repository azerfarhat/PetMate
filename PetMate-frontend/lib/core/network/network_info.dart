/// Abstraction over connectivity monitoring.
///
/// [NetworkInfo] is deliberately an interface so that the rest of the
/// application depends on the contract rather than a specific connectivity
/// package. A concrete implementation backed by `connectivity_plus` will be
/// provided when the network stack is finalized.
abstract interface class NetworkInfo {
  /// Returns `true` when the device currently has some form of connectivity.
  Future<bool> get isConnected;

  /// Broadcasts connectivity changes as they happen.
  Stream<bool> get onConnectivityChanged;
}
