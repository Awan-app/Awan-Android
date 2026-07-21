package com.awan.feature.auth.impl.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.awan.app.core.common.text.UiText
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.auth.impl.R

@Composable
fun AuthEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: UiText? = null,
    enabled: Boolean = true,
    onDone: () -> Unit = {},
) {
    Column(modifier = modifier) {
        AwanTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            enabled = enabled,
            contentDescriptionText = stringResource(R.string.auth_email_input_description),
            placeholder = {
                AwanText(
                    text = stringResource(R.string.auth_email_placeholder),
                    style = AwanTheme.styles.placeholderText,
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone() },
            ),
        )

        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            ErrorMessage(message = errorMessage)
        }
    }
}
