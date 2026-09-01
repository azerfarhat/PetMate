import 'package:flutter/material.dart';

import 'routes/app_router.dart';

/// Root widget of the PawMate application.
///
/// Owns the global [AppRouter] (and in the future global service provider
/// composition) used by the whole application.
class PawMateApp extends StatelessWidget {
  const PawMateApp({super.key});

  @override
  Widget build(BuildContext context) {
    return AppRouter.buildApp();
  }
}
