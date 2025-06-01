package com.billbuddy.android

import android.app.Application
import com.billbuddy.shared.auth.AuthenticationManager
import com.billbuddy.shared.auth.GoogleAuthService
import com.billbuddy.shared.db.BillBuddyDatabase
import com.billbuddy.shared.db.createDriver // Expecting a createDriver function
import com.billbuddy.shared.expenses.ExpenseRepository
import com.billbuddy.shared.splitting.ExpenseSplitterService // Corrected import
import com.billbuddy.shared.groups.GroupRepository
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel // Required for viewModel { } block
import org.koin.core.context.startKoin
import org.koin.dsl.module
import com.billbuddy.android.ui.login.LoginViewModel
import com.billbuddy.android.ui.grouplist.GroupListViewModel
import com.billbuddy.android.ui.groupdetails.GroupDetailsViewModel


val appModule = module {
    // Database-related dependencies
    // The actual createDriver() is in shared/androidMain and uses Koin to get context
    single<BillBuddyDatabase> { BillBuddyDatabase(createDriver()) }

    // Repositories
    single { GroupRepository(get()) }
    single { ExpenseRepository(get()) }

    // Services
    single { ExpenseSplitterService(get(), get()) } // Now uses repositories

    // Auth
    single { GoogleAuthService() } // KMP actual GoogleAuthService
    single { AuthenticationManager(get()) }

    // ViewModels
    viewModel { LoginViewModel() }
    viewModel { GroupListViewModel() }
    viewModel { params -> GroupDetailsViewModel(savedStateHandle = params.get()) } // For SavedStateHandle
}

class BillBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger() // Use androidLogger for Koin logging
            androidContext(this@BillBuddyApplication) // Provide Android context to Koin
            modules(appModule) // Your Koin modules
        }
    }
}
