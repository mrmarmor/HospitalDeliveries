package com.appstairs.hospitaldeliveries

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appstairs.hospitaldeliveries.ui.HomeScreen
import com.appstairs.hospitaldeliveries.ui.HomeViewModel
import com.appstairs.hospitaldeliveries.ui.theme.HospitalDeliveriesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as HospitalDeliveriesApp).repository
        setContent {
            HospitalDeliveriesTheme {
                // this will force ltr direction for english text on rtl-oriented devices
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
                        HomeScreen(vm)
                    }
                }
            }
        }
    }
}