import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_dimensions.dart';
import '../../../../core/utils/validators.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../controllers/auth_controller.dart';

/// Registration screen.
class RegisterPage extends StatefulWidget {
  const RegisterPage({super.key, required this.controller});

  final AuthController controller;

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> {
  late final TextEditingController _firstNameController;
  late final TextEditingController _lastNameController;
  late final TextEditingController _emailController;
  late final TextEditingController _passwordController;
  final _formKey = GlobalKey<FormState>();

  @override
  void initState() {
    super.initState();
    _firstNameController = TextEditingController();
    _lastNameController = TextEditingController();
    _emailController = TextEditingController();
    _passwordController = TextEditingController();
  }

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    final success = await widget.controller.register(
      email: _emailController.text.trim(),
      password: _passwordController.text,
      firstName: _firstNameController.text.trim(),
      lastName: _lastNameController.text.trim(),
    );
    if (success && mounted) {
      // TODO: navigate to home / onboarding-complete flow.
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
                  'Create your account',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: AppDimensions.s3),
                Text(
                  'Join PawMate and meet pet owners nearby',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
                const SizedBox(height: AppDimensions.s8),
                Row(
                  children: [
                    Expanded(
                      child: AppTextField(
                        controller: _firstNameController,
                        label: 'First name',
                        hint: 'Your first name',
                        prefixIcon: Icons.person_outline,
                        textInputAction: TextInputAction.next,
                        validator: (value) => Validators.required(
                          value,
                          message: 'Enter your first name.',
                        ),
                      ),
                    ),
                    const SizedBox(width: AppDimensions.s3),
                    Expanded(
                      child: AppTextField(
                        controller: _lastNameController,
                        label: 'Last name',
                        hint: 'Your last name',
                        prefixIcon: Icons.person_outline,
                        textInputAction: TextInputAction.next,
                        validator: (value) => Validators.required(
                          value,
                          message: 'Enter your last name.',
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppDimensions.s4),
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
                  hint: 'Minimum 8 characters',
                  prefixIcon: Icons.lock_outline,
                  obscureText: true,
                  textInputAction: TextInputAction.done,
                  validator: (value) => Validators.password(value),
                  onFieldSubmitted: (_) => _submit(),
                ),
                const SizedBox(height: AppDimensions.s8),
                AppButton(
                  label: 'Create account',
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