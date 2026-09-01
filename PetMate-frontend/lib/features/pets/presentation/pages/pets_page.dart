import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_dimensions.dart';
import '../../../../core/widgets/empty_state.dart';
import '../../../../core/widgets/error_widget.dart';
import '../../../../core/widgets/loading_widget.dart';
import '../../domain/entities/pet.dart';
import '../controllers/pet_controller.dart';

/// Lists the current user's pets.
class PetsPage extends StatefulWidget {
  const PetsPage({super.key, required this.controller});

  final PetController controller;

  @override
  State<PetsPage> createState() => _PetsPageState();
}

class _PetsPageState extends State<PetsPage> {
  @override
  void initState() {
    super.initState();
    widget.controller.loadMyPets();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.cream,
      appBar: AppBar(
        title: const Text('My Pets'),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // TODO: navigate to pet create page.
        },
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.white,
        child: const Icon(Icons.add),
      ),
      body: ListenableBuilder(
        listenable: widget.controller,
        builder: (context, _) {
          if (widget.controller.isLoading &&
              widget.controller.pets.isEmpty) {
            return const LoadingWidget();
          }
          final error = widget.controller.errorMessage;
          if (error != null && widget.controller.pets.isEmpty) {
            return ErrorWidget(
              message: error,
              onRetry: widget.controller.loadMyPets,
            );
          }
          if (widget.controller.pets.isEmpty) {
            return const EmptyState(
              icon: Icons.pets,
              title: 'No pets yet',
              message:
                  'Add a pet profile to start discovering compatible pets.',
            );
          }
          return ListView.separated(
            padding: const EdgeInsets.all(AppDimensions.s4),
            itemCount: widget.controller.pets.length,
            separatorBuilder: (_, __) =>
                const SizedBox(height: AppDimensions.s2),
            itemBuilder: (context, index) {
              final pet = widget.controller.pets[index];
              return _PetListItem(pet: pet);
            },
          );
        },
      ),
    );
  }
}

class _PetListItem extends StatelessWidget {
  const _PetListItem({required this.pet});

  final Pet pet;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: AppColors.sage,
          child: const Icon(Icons.pets, color: AppColors.white),
        ),
        title: Text(pet.name),
        subtitle: Text('${pet.type.value} · ${pet.ageYears} yrs'),
        onTap: () {
          // TODO: navigate to pet details.
        },
      ),
    );
  }
}
