import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_dimensions.dart';
import '../../../../core/utils/validators.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../controllers/auth_controller.dart';

/// Login screen.
class LoginPage extends StatefulWidget {
  const LoginPage({super.key, required this.controller});

  final AuthController controller;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  late final TextEditingController _emailController;
  late final TextEditingController _passwordController;
  final _formKey = GlobalKey<FormState>();

  @override
  void initState() {
    super.initState();
    _emailController = TextEditingController();
    _passwordController = TextEditingController();
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    final success = await widget.controller.login(
      email: _emailController.text.trim(),
      password: _passwordController.text,
    );
    if (success && mounted) {
      // TODO: navigate to home.
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.cream,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppDimensions.s6),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: AppDimensions.s8),
                const Icon(Icons.pets, size: 72, color: AppColors.primary),
                const SizedBox(height: AppDimensions.s4),
                Text(
                  'Welcome back',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: AppDimensions.s3),
                Text(
                  'Log in to continue to PawMate',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
                const SizedBox(height: AppDimensions.s8),
                AppTextField(
                  controller: _emailController,
                  label: 'Email',
                  hint: 'you@example.com',
                  prefixIcon: Icons.email_outlined,
                  keyboardType: TextInputType.emailAddress,
                  textInputAction: TextInputAction.next,
                  validator: Validators.email,
                ),
                const SizedBox(height: AppDimensions.s4),
                AppTextField(
                  controller: _passwordController,
                  label: 'Password',
                  hint: '••••••••',
                  prefixIcon: Icons.lock_outline,
                  obscureText: true,
                  textInputAction: TextInputAction.done,
                  validator: (value) => Validators.password(value),
                  onFieldSubmitted: (_) => _submit(),
                ),
                const SizedBox(height: AppDimensions.s2),
                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton(
                    onPressed: () {
                      // TODO: navigate to forgot password.
                    },
                    child: const Text('Forgot password?'),
                  ),
                ),
                const SizedBox(height: AppDimensions.s4),
                AppButton(
                  label: 'Log in',
                  loading: widget.controller.isLoading,
                  onPressed: _submit,
                ),
                const SizedBox(height: AppDimensions.s4),
                ListenableBuilder(
                  listenable: widget.controller,
                  builder: (context, _) {
                    final error = widget.controller.errorMessage;
                    if (error == null) {
                      return const SizedBox.shrink();
                    }
                    return Text(
                      error,
                      textAlign: TextAlign.center,
                      style: Theme.of(context)
                          .textTheme
                          .bodyMedium
                          ?.copyWith(color: AppColors.danger),
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
