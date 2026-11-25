package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ReceiptTemplateEntity
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CobaltBlue

@Composable
fun TemplatesScreen(
  templates: List<ReceiptTemplateEntity>,
  onCreateTemplate: () -> Unit,
  onEditTemplate: (ReceiptTemplateEntity) -> Unit,
  onUseTemplateForReceipt: (ReceiptTemplateEntity) -> Unit,
  onDuplicateTemplate: (ReceiptTemplateEntity) -> Unit,
  onSetDefault: (Long) -> Unit,
  onDeleteTemplate: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = onCreateTemplate,
        containerColor = CobaltBlue,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("create_template_fab"),
      ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_template))
      }
    },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
          text = stringResource(R.string.templates_title),
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(
          text = "Create, customize, and reuse receipt layouts for 58mm & 80mm printers",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (templates.isEmpty()) {
        Column(
          modifier = Modifier.fillMaxSize().padding(32.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text("No receipt templates yet", style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(12.dp))
          Button(onClick = onCreateTemplate) { Text(stringResource(R.string.create_template)) }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(templates, key = { it.id }) { tpl ->
            TemplateCard(
              template = tpl,
              onUse = { onUseTemplateForReceipt(tpl) },
              onEdit = { onEditTemplate(tpl) },
              onDuplicate = { onDuplicateTemplate(tpl) },
              onToggleDefault = { onSetDefault(tpl.id) },
              onDelete = { onDeleteTemplate(tpl.id) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TemplateCard(
  template: ReceiptTemplateEntity,
  onUse: () -> Unit,
  onEdit: () -> Unit,
  onDuplicate: () -> Unit,
  onToggleDefault: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    onClick = onEdit,
    modifier = Modifier.fillMaxWidth().testTag("template_card_${template.id}"),
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (template.isDefault) CobaltBlue.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.outlineVariant,
      ),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = template.templateName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text = "${template.paperWidthMm}mm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "${template.businessName} • Tax ${template.defaultTaxPercent}% • ${template.currencySymbol}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        IconButton(onClick = onToggleDefault, modifier = Modifier.testTag("default_template_${template.id}")) {
          Icon(
            imageVector = if (template.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = "Default template",
            tint = if (template.isDefault) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (template.businessAddress.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = template.businessAddress,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onUse,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.testTag("use_template_${template.id}"),
          ) {
            Text(stringResource(R.string.use_template))
          }
          OutlinedButton(
            onClick = onEdit,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Edit")
          }
        }

        Row {
          IconButton(onClick = onDuplicate) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = stringResource(R.string.duplicate),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDelete) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = stringResource(R.string.delete),
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}
