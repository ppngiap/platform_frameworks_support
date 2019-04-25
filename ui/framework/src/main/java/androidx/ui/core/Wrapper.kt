/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.ui.core

import android.content.Context
import androidx.annotation.CheckResult
import androidx.ui.core.input.FocusManager
import androidx.ui.input.TextInputService
import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.R4a
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.compositionReference
import com.google.r4a.effectOf
import com.google.r4a.memo
import com.google.r4a.unaryPlus

@Composable
fun CraneWrapper(@Children children: () -> Unit) {
    val rootRef = +memo { Ref<AndroidCraneView>() }

    // TODO(nona): Tie the focus manger lifecycle to Window, otherwise FocusManager won't work with
    //             nested AndroidCraneView case
    val focusManager = FocusManager()

    <AndroidCraneView ref=rootRef>
        val reference = +compositionReference()
        val rootLayoutNode = rootRef.value?.root ?: error("Failed to create root platform view")
        val context = rootRef.value?.context ?: composer.composer.context
        R4a.composeInto(container = rootLayoutNode, context = context, parent = reference) {
            <ContextAmbient.Provider value=context>
                <DensityAmbient.Provider value=Density(context)>
                    <FocusManagerAmbient.Provider value=focusManager>
                        <TextInputServiceAmbient.Provider value=rootRef.value?.textInputService>
                            <children />
                        </TextInputServiceAmbient.Provider>
                    </FocusManagerAmbient.Provider>
                </DensityAmbient.Provider>
            </ContextAmbient.Provider>
        }
    </AndroidCraneView>
}

val ContextAmbient = Ambient.of<Context>()

val DensityAmbient = Ambient.of<Density>()

internal val FocusManagerAmbient = Ambient.of<FocusManager>()

internal val TextInputServiceAmbient = Ambient.of<TextInputService?>()

/**
 * [ambient] to get a [Density] object from an internal [DensityAmbient].
 *
 * Note: this is an experiment with the ways to achieve a read-only public [Ambient]s.
 */
@CheckResult(suggest = "+")
fun ambientDensity() = effectOf<Density> { +ambient(DensityAmbient) }

/**
 * An effect to be able to convert dimensions between each other.
 * A [Density] object will be taken from an ambient.
 *
 * Usage examples:
 *
 *     +withDensity() {
 *        val pxHeight = DpHeight.toPx()
 *     }
 *
 * or
 *
 *     val pxHeight = +withDensity(density) { DpHeight.toPx() }
 *
 */
@CheckResult(suggest = "+")
// can't make this inline as tests are failing with "DensityKt.$jacocoInit()' is inaccessible"
/*inline*/ fun <R> withDensity(/*crossinline*/ block: @Composable() DensityReceiver.() -> R) =
    effectOf<R> {
        withDensity(+ambientDensity(), block)
    }