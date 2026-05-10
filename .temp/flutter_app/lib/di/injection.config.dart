// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// InjectableConfigGenerator
// **************************************************************************

// ignore_for_file: type=lint
// coverage:ignore-file

// ignore_for_file: no_leading_underscores_for_library_prefixes
import 'package:expense_tracker_flutter/data/local/dao/category_dao.dart'
    as _i526;
import 'package:expense_tracker_flutter/data/local/dao/expense_dao.dart'
    as _i939;
import 'package:expense_tracker_flutter/data/local/dao/installment_dao.dart'
    as _i966;
import 'package:expense_tracker_flutter/data/local/dao/tag_dao.dart' as _i765;
import 'package:expense_tracker_flutter/data/local/database.dart' as _i299;
import 'package:expense_tracker_flutter/data/preferences/budget_preferences_impl.dart'
    as _i203;
import 'package:expense_tracker_flutter/di/module.dart' as _i427;
import 'package:expense_tracker_flutter/domain/preferences/budget_preferences.dart'
    as _i555;
import 'package:expense_tracker_flutter/domain/repository/expense_repository.dart'
    as _i323;
import 'package:expense_tracker_flutter/domain/repository/installment_repository.dart'
    as _i228;
import 'package:expense_tracker_flutter/domain/use_cases/add_expense_use_case.dart'
    as _i1;
import 'package:expense_tracker_flutter/domain/use_cases/create_installment_plan_use_case.dart'
    as _i33;
import 'package:expense_tracker_flutter/domain/use_cases/get_categories_use_case.dart'
    as _i228;
import 'package:expense_tracker_flutter/domain/use_cases/get_expense_by_id_use_case.dart'
    as _i256;
import 'package:expense_tracker_flutter/domain/use_cases/get_expenses_use_case.dart'
    as _i647;
import 'package:expense_tracker_flutter/domain/use_cases/get_installment_items_use_case.dart'
    as _i284;
import 'package:expense_tracker_flutter/domain/use_cases/get_installments_use_case.dart'
    as _i856;
import 'package:expense_tracker_flutter/domain/use_cases/get_item_name_suggestions_use_case.dart'
    as _i712;
import 'package:expense_tracker_flutter/domain/use_cases/get_merchant_suggestions_use_case.dart'
    as _i185;
import 'package:expense_tracker_flutter/domain/use_cases/get_stats_use_case.dart'
    as _i578;
import 'package:expense_tracker_flutter/domain/use_cases/update_expense_use_case.dart'
    as _i761;
import 'package:expense_tracker_flutter/domain/use_cases/update_installment_item_status_use_case.dart'
    as _i614;
import 'package:expense_tracker_flutter/presentation/add_expense/cubit/add_expense_cubit.dart'
    as _i1052;
import 'package:expense_tracker_flutter/presentation/expense_list/cubit/expense_list_cubit.dart'
    as _i723;
import 'package:expense_tracker_flutter/presentation/installments/cubit/installment_list_cubit.dart'
    as _i826;
import 'package:expense_tracker_flutter/presentation/settings/cubit/settings_cubit.dart'
    as _i335;
import 'package:expense_tracker_flutter/presentation/stats/cubit/stats_cubit.dart'
    as _i691;
import 'package:get_it/get_it.dart' as _i174;
import 'package:injectable/injectable.dart' as _i526;

extension GetItInjectableX on _i174.GetIt {
  // initializes the registration of main-scope dependencies inside of GetIt
  _i174.GetIt init({
    String? environment,
    _i526.EnvironmentFilter? environmentFilter,
  }) {
    final gh = _i526.GetItHelper(this, environment, environmentFilter);
    final appModule = _$AppModule();
    gh.singleton<_i299.AppDatabase>(() => appModule.db);
    gh.lazySingleton<_i939.ExpenseDao>(
      () => appModule.getExpenseDao(gh<_i299.AppDatabase>()),
    );
    gh.lazySingleton<_i526.CategoryDao>(
      () => appModule.getCategoryDao(gh<_i299.AppDatabase>()),
    );
    gh.lazySingleton<_i765.TagDao>(
      () => appModule.getTagDao(gh<_i299.AppDatabase>()),
    );
    gh.lazySingleton<_i966.InstallmentDao>(
      () => appModule.getInstallmentDao(gh<_i299.AppDatabase>()),
    );
    gh.lazySingleton<_i555.BudgetPreferences>(
      () => _i203.BudgetPreferencesImpl(),
    );
    gh.lazySingleton<_i323.ExpenseRepository>(
      () => appModule.getExpenseRepository(
        gh<_i939.ExpenseDao>(),
        gh<_i526.CategoryDao>(),
        gh<_i765.TagDao>(),
      ),
    );
    gh.lazySingleton<_i1.AddExpenseUseCase>(
      () => _i1.AddExpenseUseCase(gh<_i323.ExpenseRepository>()),
    );
    gh.lazySingleton<_i228.GetCategoriesUseCase>(
      () => _i228.GetCategoriesUseCase(gh<_i323.ExpenseRepository>()),
    );
    gh.lazySingleton<_i256.GetExpenseByIdUseCase>(
      () => _i256.GetExpenseByIdUseCase(gh<_i323.ExpenseRepository>()),
    );
    gh.lazySingleton<_i647.GetExpensesUseCase>(
      () => _i647.GetExpensesUseCase(gh<_i323.ExpenseRepository>()),
    );
    gh.lazySingleton<_i712.GetItemNameSuggestionsUseCase>(
      () => _i712.GetItemNameSuggestionsUseCase(gh<_i323.ExpenseRepository>()),
    );
    gh.lazySingleton<_i185.GetMerchantSuggestionsUseCase>(
      () => _i185.GetMerchantSuggestionsUseCase(gh<_i323.ExpenseRepository>()),
    );
    gh.lazySingleton<_i578.GetStatsUseCase>(
      () => _i578.GetStatsUseCase(gh<_i323.ExpenseRepository>()),
    );
    gh.factory<_i335.SettingsCubit>(
      () => _i335.SettingsCubit(
        gh<_i323.ExpenseRepository>(),
        gh<_i555.BudgetPreferences>(),
      ),
    );
    gh.lazySingleton<_i228.InstallmentRepository>(
      () => appModule.getInstallmentRepository(gh<_i966.InstallmentDao>()),
    );
    gh.factory<_i691.StatsCubit>(
      () => _i691.StatsCubit(gh<_i578.GetStatsUseCase>()),
    );
    gh.lazySingleton<_i284.GetInstallmentItemsUseCase>(
      () => _i284.GetInstallmentItemsUseCase(gh<_i228.InstallmentRepository>()),
    );
    gh.lazySingleton<_i856.GetInstallmentsUseCase>(
      () => _i856.GetInstallmentsUseCase(gh<_i228.InstallmentRepository>()),
    );
    gh.lazySingleton<_i614.UpdateInstallmentItemStatusUseCase>(
      () => _i614.UpdateInstallmentItemStatusUseCase(
        gh<_i228.InstallmentRepository>(),
      ),
    );
    gh.lazySingleton<_i33.CreateInstallmentPlanUseCase>(
      () =>
          _i33.CreateInstallmentPlanUseCase(gh<_i228.InstallmentRepository>()),
    );
    gh.factory<_i826.InstallmentListCubit>(
      () => _i826.InstallmentListCubit(
        gh<_i856.GetInstallmentsUseCase>(),
        gh<_i284.GetInstallmentItemsUseCase>(),
        gh<_i614.UpdateInstallmentItemStatusUseCase>(),
        gh<_i228.InstallmentRepository>(),
      ),
    );
    gh.factory<_i723.ExpenseListCubit>(
      () => _i723.ExpenseListCubit(
        gh<_i323.ExpenseRepository>(),
        gh<_i228.InstallmentRepository>(),
        gh<_i228.GetCategoriesUseCase>(),
        gh<_i555.BudgetPreferences>(),
      ),
    );
    gh.lazySingleton<_i761.UpdateExpenseUseCase>(
      () => _i761.UpdateExpenseUseCase(
        gh<_i323.ExpenseRepository>(),
        gh<_i228.InstallmentRepository>(),
        gh<_i33.CreateInstallmentPlanUseCase>(),
      ),
    );
    gh.factory<_i1052.AddExpenseCubit>(
      () => _i1052.AddExpenseCubit(
        gh<_i1.AddExpenseUseCase>(),
        gh<_i761.UpdateExpenseUseCase>(),
        gh<_i256.GetExpenseByIdUseCase>(),
        gh<_i228.GetCategoriesUseCase>(),
        gh<_i33.CreateInstallmentPlanUseCase>(),
        gh<_i712.GetItemNameSuggestionsUseCase>(),
        gh<_i185.GetMerchantSuggestionsUseCase>(),
        gh<_i228.InstallmentRepository>(),
        gh<_i323.ExpenseRepository>(),
      ),
    );
    return this;
  }
}

class _$AppModule extends _i427.AppModule {}
