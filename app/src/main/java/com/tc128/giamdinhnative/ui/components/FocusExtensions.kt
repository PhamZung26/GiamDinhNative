package com.tc128.giamdinhnative.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.launch

/**
 * Tự cuộn ô này lên trên bàn phím ngay khi được focus — không cần người dùng tự cuộn tay.
 * Dùng cho các trường nằm trong Column có verticalScroll() + imePadding() (vd: ItemDetailScreen,
 * NewItemScreen) — Compose không tự làm việc này như EditText/ScrollView truyền thống của View.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.autoBringIntoViewOnFocus(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return this
        .bringIntoViewRequester(requester)
        .onFocusEvent { state ->
            if (state.isFocused) {
                scope.launch { requester.bringIntoView() }
            }
        }
}
