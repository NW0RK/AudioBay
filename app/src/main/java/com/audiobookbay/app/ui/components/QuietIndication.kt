package com.audiobookbay.app.ui.components
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
object QuietIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node = QuietNode(interactionSource)
    override fun equals(other: Any?) = other === this
    override fun hashCode() = 4319
}
private class QuietNode(val source: InteractionSource) : Modifier.Node(), DrawModifierNode {
    private val focuses = mutableSetOf<FocusInteraction.Focus>()
    private var focused by androidx.compose.runtime.mutableStateOf(false)
    private val presses = mutableSetOf<PressInteraction.Press>()
    private var pressed by androidx.compose.runtime.mutableStateOf(false)
    override fun onAttach() { coroutineScope.launch { source.interactions.collect { interaction ->
        when (interaction) {
            is FocusInteraction.Focus -> focuses.add(interaction)
            is FocusInteraction.Unfocus -> focuses.remove(interaction.focus)
            is PressInteraction.Press -> presses.add(interaction)
            is PressInteraction.Release -> presses.remove(interaction.press)
            is PressInteraction.Cancel -> presses.remove(interaction.press)
        }
        pressed = presses.isNotEmpty()
        focused = focuses.isNotEmpty()
    } } }
    override fun ContentDrawScope.draw() { drawContent(); if (pressed) drawRect(Color.Gray.copy(alpha = 0.1f)); if (focused) drawRect(Color.Gray, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx())) }
}
