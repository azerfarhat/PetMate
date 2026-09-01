import 'package:flutter/material.dart';

import '../theme/app_theme.dart';
import 'route_names.dart';

/// Centralized routing configuration for the PawMate application.
///
/// All navigation must be configured here. Individual widgets must not
/// scatter navigation logic across the codebase. This router is intentionally
/// minimal in early MVP stages; destination pages are wired up as features
/// are implemented.
class AppRouter {
  AppRouter._();

  static const String _errorRoute = '/error';

  /// Returns the root [MaterialApp] configuration for the router.
  ///
  /// [initialRoute] points to the onboarding flow for now. Features will
  /// register their pages here as they are implemented.
  static Widget buildApp() {
    return MaterialApp(
      title: 'PawMate',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      initialRoute: RouteNames.onboarding,
      onGenerateRoute: _onGenerateRoute,
    );
  }

  static Route<dynamic>? _onGenerateRoute(RouteSettings settings) {
    final routeName = settings.name;

    switch (routeName) {
      // Pages are wired up as their features are implemented.
      case RouteNames.onboarding:
        return _placeholderRoute('Onboarding');
      case RouteNames.login:
        return _placeholderRoute('Login');
      case RouteNames.register:
        return _placeholderRoute('Register');
      case RouteNames.discover:
        return _placeholderRoute('Discover');
      case RouteNames.matches:
        return _placeholderRoute('Matches');
      case RouteNames.home:
        return _placeholderRoute('Home');
      case _errorRoute:
        return MaterialPageRoute(
          settings: settings,
          builder: (_) => const _ErrorPage(),
        );
      default:
        return MaterialPageRoute(
          settings: const RouteSettings(name: _errorRoute),
          builder: (_) => const _ErrorPage(),
        );
    }
  }

  /// Temporary helper that renders an informational placeholder page for a
  /// feature that has not been implemented yet.
  static Route<dynamic> _placeholderRoute(String title) {
    return MaterialPageRoute(
      builder: (_) => _PlaceholderPage(title: title),
    );
  }
}

class _PlaceholderPage extends StatelessWidget {
  const _PlaceholderPage({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: Center(
        child: Text(
          '$title will be implemented here.',
          style: Theme.of(context).textTheme.titleMedium,
        ),
      ),
    );
  }
}

class _ErrorPage extends StatelessWidget {
  const _ErrorPage();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Error')),
      body: const Center(
        child: Text('The requested route could not be found.'),
      ),
    );
  }
}
