import 'dart:math' as math;

/// Location-related helpers.
///
/// PawMate uses a privacy-preserving proximity model. Rather than exposing a
/// user's exact coordinates, we expose coarse distance bands such as
/// "Within 2 km".
abstract final class LocationUtils {
  LocationUtils._();

  /// Approximate mean radius of the Earth in kilometers.
  static const double _earthRadiusKm = 6371.0;

  /// Distance bands displayed to users instead of exact coordinates.
  static const List<String> proximityBands = [
    'Within 2 km',
    'Within 5 km',
    'Within 10 km',
  ];

  /// Calculates the great-circle distance in kilometers between two points
  /// using the haversine formula.
  static double distanceInKm(
    double lat1,
    double lng1,
    double lat2,
    double lng2,
  ) {
    final dLat = _toRadians(lat2 - lat1);
    final dLng = _toRadians(lng2 - lng1);

    final a = _sinSquaredHalf(dLat) +
        math.cos(_toRadians(lat1)) *
            math.cos(_toRadians(lat2)) *
            _sinSquaredHalf(dLng);

    final c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a));
    return _earthRadiusKm * c;
  }

  /// Returns the distance band label for [distanceKm].
  static String bandForDistance(double distanceKm) {
    if (distanceKm <= 2) {
      return proximityBands[0];
    }
    if (distanceKm <= 5) {
      return proximityBands[1];
    }
    return proximityBands[2];
  }

  static double _toRadians(num degrees) => degrees * math.pi / 180;

  static double _sinSquaredHalf(num angle) {
    final s = math.sin(angle / 2);
    return s * s;
  }
}
