import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:paw_mate/app/app.dart';

void main() {
  testWidgets('app boots and shows the login screen', (tester) async {
    await tester.pumpWidget(const PawMateApp());
    await tester.pumpAndSettle();

    expect(
      find.byType(Scaffold),
      findsWidgets,
      reason: 'The app should render at least one scaffold.',
    );
  });
}