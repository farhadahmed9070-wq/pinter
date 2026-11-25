package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.PrintOrderEntity
import com.example.ui.navigation.Routes
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NewOrderScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.PrintImageScreen
import com.example.ui.screens.PrintReceiptScreen
import com.example.ui.screens.PrintTextScreen
import com.example.ui.screens.PrinterDiscoveryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TemplateEditorScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.BluetoothPrintTheme
import com.example.ui.viewmodel.BluetoothPrintViewModel
import com.example.util.LocaleHelper

private data class BottomNavTab(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: BluetoothPrintViewModel = viewModel()
      val settings by viewModel.settings.collectAsStateWithLifecycle()
      val baseContext = this@MainActivity
      val localizedContext =
        remember(settings.language) { LocaleHelper.wrapContext(baseContext, settings.language) }

      CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalActivityResultRegistryOwner provides this@MainActivity,
      ) {
        BluetoothPrintTheme {
          BluetoothPrintApp(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
private fun BluetoothPrintApp(viewModel: BluetoothPrintViewModel) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val snackbarHostState = remember { SnackbarHostState() }

  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val orders by viewModel.orders.collectAsStateWithLifecycle()
  val recentOrders by viewModel.recentOrders.collectAsStateWithLifecycle()
  val templates by viewModel.templates.collectAsStateWithLifecycle()
  val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
  val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
  val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
  val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
  val isBtSupported by viewModel.isBluetoothSupported.collectAsStateWithLifecycle()
  val isBtEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
  val hasBtPermissions by viewModel.hasBluetoothPermissions.collectAsStateWithLifecycle()
  val isPrinting by viewModel.isPrinting.collectAsStateWithLifecycle()

  val ditheredPreview by viewModel.ditheredPreview.collectAsStateWithLifecycle()
  val ditherMode by viewModel.ditherMode.collectAsStateWithLifecycle()
  val imageBrightness by viewModel.imageBrightness.collectAsStateWithLifecycle()
  val imageAlign by viewModel.imageAlign.collectAsStateWithLifecycle()

  var selectedReceiptTemplateId by remember { mutableStateOf<Long?>(null) }
  var pendingOrderPreview by remember { mutableStateOf<PrintOrderEntity?>(null) }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshBluetoothState()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(Unit) {
    viewModel.messages.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  val topLevelTabs =
    listOf(
      BottomNavTab(
        route = Routes.HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Default.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home_tab",
      ),
      BottomNavTab(
        route = Routes.ORDERS,
        labelRes = R.string.nav_orders,
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        testTag = "nav_orders_tab",
      ),
      BottomNavTab(
        route = Routes.TEMPLATES,
        labelRes = R.string.nav_templates,
        selectedIcon = Icons.Default.Description,
        unselectedIcon = Icons.Outlined.Description,
        testTag = "nav_templates_tab",
      ),
      BottomNavTab(
        route = Routes.SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Default.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings_tab",
      ),
    )

  val showTopLevelNav =
    topLevelTabs.any { tab ->
      currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (showTopLevelNav && !useNavRail) {
          NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            topLevelTabs.forEach { tab ->
              val selected =
                currentDestination?.hierarchy?.any { it.route == tab.route } == true
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.labelRes),
                  )
                },
                label = { Text(stringResource(tab.labelRes)) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (showTopLevelNav && useNavRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            topLevelTabs.forEach { tab ->
              val selected =
                currentDestination?.hierarchy?.any { it.route == tab.route } == true
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.labelRes),
                  )
                },
                label = { Text(stringResource(tab.labelRes)) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
          NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
          ) {
            composable(Routes.HOME) {
              HomeScreen(
                connectionState = connectionState,
                settings = settings,
                recentOrders = recentOrders,
                onFindPrinter = { navController.navigate(Routes.PRINTER_DISCOVERY) },
                onDisconnect = { viewModel.disconnectPrinter() },
                onTestPrint = { viewModel.printTestPage() },
                onNewOrder = { navController.navigate(Routes.NEW_ORDER) },
                onPrintText = { navController.navigate(Routes.PRINT_TEXT) },
                onPrintImages = { navController.navigate(Routes.PRINT_IMAGE) },
                onPrintReceipts = {
                  selectedReceiptTemplateId = null
                  navController.navigate(Routes.PRINT_RECEIPT)
                },
                onViewAllOrders = { navigateToTab(Routes.ORDERS) },
                onReprintOrder = { viewModel.reprintOrder(it) },
                onOpenOrderPreview = { order ->
                  pendingOrderPreview = order
                  navigateToTab(Routes.ORDERS)
                },
              )
            }

            composable(Routes.ORDERS) {
              OrdersScreen(
                orders = orders,
                onNewOrder = { navController.navigate(Routes.NEW_ORDER) },
                onReprintOrder = { viewModel.reprintOrder(it) },
                onDeleteOrder = { viewModel.deleteOrder(it) },
                initialPreviewOrder = pendingOrderPreview,
                onDismissInitialPreview = { pendingOrderPreview = null },
              )
            }

            composable(Routes.TEMPLATES) {
              TemplatesScreen(
                templates = templates,
                onCreateTemplate = { navController.navigate(Routes.templateEditor(-1L)) },
                onEditTemplate = { tpl ->
                  navController.navigate(Routes.templateEditor(tpl.id))
                },
                onUseTemplateForReceipt = { tpl ->
                  selectedReceiptTemplateId = tpl.id
                  navController.navigate(Routes.PRINT_RECEIPT)
                },
                onDuplicateTemplate = { viewModel.duplicateTemplate(it) },
                onSetDefault = { viewModel.setDefaultTemplate(it) },
                onDeleteTemplate = { viewModel.deleteTemplate(it) },
              )
            }

            composable(Routes.SETTINGS) {
              SettingsScreen(
                settings = settings,
                connectionState = connectionState,
                onFindPrinter = { navController.navigate(Routes.PRINTER_DISCOVERY) },
                onPaperWidthChange = { viewModel.setPaperWidth(it) },
                onPrintDensityChange = { viewModel.setPrintDensity(it) },
                onAutoCutChange = { viewModel.setAutoCut(it) },
                onCashDrawerChange = { viewModel.setCashDrawer(it) },
                onFeedLinesChange = { viewModel.setFeedLines(it) },
                onAutoReconnectChange = { viewModel.setAutoReconnect(it) },
                onEncodingChange = { viewModel.setEncoding(it) },
                onLanguageChange = { viewModel.setLanguage(it) },
                onTestPrint = { viewModel.printTestPage() },
              )
            }

            composable(Routes.PRINTER_DISCOVERY) {
              PrinterDiscoveryScreen(
                isBluetoothSupported = isBtSupported,
                isBluetoothEnabled = isBtEnabled,
                hasPermissions = hasBtPermissions,
                isScanning = isScanning,
                pairedDevices = pairedDevices,
                discoveredDevices = discoveredDevices,
                connectionState = connectionState,
                onBack = { navController.popBackStack() },
                onPermissionsResult = { viewModel.refreshBluetoothState() },
                onStartScan = { viewModel.startScan() },
                onStopScan = { viewModel.stopScan() },
                onConnect = { viewModel.connectToPrinter(it) },
                onDisconnect = { viewModel.disconnectPrinter() },
                onTestPrint = { viewModel.printTestPage() },
              )
            }

            composable(Routes.NEW_ORDER) {
              NewOrderScreen(
                settings = settings,
                templates = templates,
                nextOrderNumberProvider = { viewModel.nextOrderNumber() },
                onBack = { navController.popBackStack() },
                onSaveOrPrint = {
                  orderNumber,
                  orderType,
                  customerName,
                  businessName,
                  businessAddress,
                  businessPhone,
                  headerText,
                  footerText,
                  items,
                  taxPercent,
                  discountAmount,
                  currencySymbol,
                  notes,
                  headerAlign,
                  includeQrFooter,
                  dateTimeText,
                  shouldPrintNow ->
                  viewModel.saveOrPrintOrder(
                    orderNumber = orderNumber,
                    orderType = orderType,
                    customerName = customerName,
                    businessName = businessName,
                    businessAddress = businessAddress,
                    businessPhone = businessPhone,
                    headerText = headerText,
                    footerText = footerText,
                    items = items,
                    taxPercent = taxPercent,
                    discountAmount = discountAmount,
                    currencySymbol = currencySymbol,
                    notes = notes,
                    headerAlign = headerAlign,
                    includeQrFooter = includeQrFooter,
                    dateTimeText = dateTimeText,
                    shouldPrintNow = shouldPrintNow,
                    onDone = { navController.popBackStack() },
                  )
                },
              )
            }

            composable(Routes.PRINT_TEXT) {
              PrintTextScreen(
                settings = settings,
                isPrinting = isPrinting,
                onBack = { navController.popBackStack() },
                onPrintText = { text, align, size, bold, underline ->
                  viewModel.printText(text, align, size, bold, underline)
                },
              )
            }

            composable(Routes.PRINT_IMAGE) {
              PrintImageScreen(
                settings = settings,
                ditheredResult = ditheredPreview,
                ditherMode = ditherMode,
                brightness = imageBrightness,
                imageAlign = imageAlign,
                isPrinting = isPrinting,
                onBack = { navController.popBackStack() },
                onImagePicked = { viewModel.onImagePicked(it) },
                onDitherModeChange = { viewModel.updateDitherMode(it) },
                onBrightnessChange = { viewModel.updateImageBrightness(it) },
                onAlignChange = { viewModel.updateImageAlign(it) },
                onPrintImage = { viewModel.printCurrentImage() },
              )
            }

            composable(Routes.PRINT_RECEIPT) {
              PrintReceiptScreen(
                settings = settings,
                templates = templates,
                initialTemplateId = selectedReceiptTemplateId,
                isPrinting = isPrinting,
                nextOrderNumberProvider = { viewModel.nextOrderNumber() },
                onBack = { navController.popBackStack() },
                onPrintReceipt = {
                  orderNumber,
                  orderType,
                  customerName,
                  businessName,
                  businessAddress,
                  businessPhone,
                  headerText,
                  footerText,
                  items,
                  taxPercent,
                  discountAmount,
                  currencySymbol,
                  notes,
                  headerAlign,
                  includeQrFooter,
                  dateTimeText,
                  shouldPrintNow ->
                  viewModel.saveOrPrintOrder(
                    orderNumber = orderNumber,
                    orderType = orderType,
                    customerName = customerName,
                    businessName = businessName,
                    businessAddress = businessAddress,
                    businessPhone = businessPhone,
                    headerText = headerText,
                    footerText = footerText,
                    items = items,
                    taxPercent = taxPercent,
                    discountAmount = discountAmount,
                    currencySymbol = currencySymbol,
                    notes = notes,
                    headerAlign = headerAlign,
                    includeQrFooter = includeQrFooter,
                    dateTimeText = dateTimeText,
                    shouldPrintNow = shouldPrintNow,
                  )
                },
              )
            }

            composable(
              route = Routes.TEMPLATE_EDITOR_PATTERN,
              arguments =
                listOf(
                  navArgument(Routes.TEMPLATE_EDITOR_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                  }
                ),
            ) { entry ->
              val tplId = entry.arguments?.getLong(Routes.TEMPLATE_EDITOR_ARG) ?: -1L
              TemplateEditorScreen(
                templateId = tplId,
                loadTemplate = { viewModel.getTemplateById(it) },
                onBack = { navController.popBackStack() },
                onSaveTemplate = { template ->
                  viewModel.saveTemplate(template) { navController.popBackStack() }
                },
              )
            }
          }
        }
      }
    }
  }
}
