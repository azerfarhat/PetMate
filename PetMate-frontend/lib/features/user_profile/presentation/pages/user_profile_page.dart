import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_dimensions.dart';
import '../../../../core/widgets/error_widget.dart';
import '../../../../core/widgets/loading_widget.dart';
import '../../domain/entities/user_profile.dart';
import '../controllers/user_profile_controller.dart';

/// Displays the current user's profile.
class UserProfilePage extends StatefulWidget {
  const UserProfilePage({super.key, required this.controller});

  final UserProfileController controller;

  @override
  State<UserProfilePage> createState() => _UserProfilePageState();
}

class _UserProfilePageState extends State<UserProfilePage> {
  @override
  void initState() {
    super.initState();
    widget.controller.loadProfile();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.cream,
      appBar: AppBar(
        title: const Text('My Profile'),
        actions: [
          IconButton(
            onPressed: () {
              // TODO: navigate to profile edit.
            },
            icon: const Icon(Icons.edit_outlined),
          ),
        ],
      ),
      body: ListenableBuilder(
        listenable: widget.controller,
        builder: (context, _) {
          if (widget.controller.isLoading &&
              widget.controller.profile == null) {
            return const LoadingWidget();
          }
          final error = widget.controller.errorMessage;
          if (error != null && widget.controller.profile == null) {
            return ErrorView(
              message: error,
              onRetry: widget.controller.loadProfile,
            );
          }
          final profile = widget.controller.profile;
          if (profile == null) {
            return const ErrorView(message: 'Unable to load your profile.');
          }
          return _buildProfile(profile);
        },
      ),
    );
  }

  Widget _buildProfile(UserProfile profile) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppDimensions.s6),
      child: Column(
        children: [
          CircleAvatar(
            radius: AppDimensions.avatarSizeLarge / 2,
            backgroundColor: AppColors.divider,
            backgroundImage: profile.avatarUrl != null
                ? NetworkImage(profile.avatarUrl!)
                : null,
            child: profile.avatarUrl == null
                ? const Icon(
                    Icons.person,
                    size: 48,
                    color: AppColors.textSecondary,
                  )
                : null,
          ),
          const SizedBox(height: AppDimensions.s4),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Flexible(
                child: Text(
                  profile.displayName,
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
              ),
              if (profile.isVerified) ...[
                const SizedBox(width: AppDimensions.s2),
                const Icon(
                  Icons.verified,
                  size: 20,
                  color: AppColors.sage,
                ),
              ],
            ],
          ),
          if (profile.bio != null) ...[
            const SizedBox(height: AppDimensions.s2),
            Text(
              profile.bio!,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
        ],
      ),
    );
  }
}
