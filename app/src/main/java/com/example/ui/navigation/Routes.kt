package com.example.ui.navigation

object Routes {
  const val HOME = "home"
  const val ORDERS = "orders"
  const val TEMPLATES = "templates"
  const val SETTINGS = "settings"

  const val PRINTER_DISCOVERY = "printer_discovery"
  const val NEW_ORDER = "new_order"
  const val PRINT_TEXT = "print_text"
  const val PRINT_IMAGE = "print_image"
  const val PRINT_RECEIPT = "print_receipt"
  const val TEMPLATE_EDITOR = "template_editor"
  const val TEMPLATE_EDITOR_ARG = "templateId"
  const val TEMPLATE_EDITOR_PATTERN = "$TEMPLATE_EDITOR/{$TEMPLATE_EDITOR_ARG}"

  fun templateEditor(templateId: Long = -1L): String = "$TEMPLATE_EDITOR/$templateId"
}
