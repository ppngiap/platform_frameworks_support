/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.androidview

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.r4a.Composable
import com.google.r4a.Model
import com.google.r4a.adapters.dp
import com.google.r4a.adapters.setControlledText
import com.google.r4a.adapters.setLayoutHeight
import com.google.r4a.adapters.setLayoutWeight
import com.google.r4a.adapters.setLayoutWidth
import com.google.r4a.adapters.setOnClick
import com.google.r4a.adapters.setOnTextChanged
import com.google.r4a.composer
import com.google.r4a.setContent
import com.google.r4a.state
import com.google.r4a.unaryPlus

@Model
class WebParams {
    var url: String = "https://www.google.com"
}

open class WebComponentActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            if (WebContext.debug) {
                Log.e("WebCompAct", "setContent")
            }

            <FrameLayout>
                <renderViews />
            </FrameLayout>
        }
    }
}

@Composable
fun renderViews(webParams: WebParams = WebParams(), webContext: WebContext = WebContext()) {
    if (WebContext.debug) {
        Log.d("WebCompAct", "renderViews")
    }

    val textContents = +state { "https://www.google.com" }

    <LinearLayout
        orientation=LinearLayout.VERTICAL
        layoutWidth=MATCH_PARENT
        layoutHeight=MATCH_PARENT>
        <LinearLayout
            orientation=LinearLayout.HORIZONTAL
            layoutWidth=MATCH_PARENT
            layoutHeight=WRAP_CONTENT
            weightSum=1f>
            <Button
                layoutWidth=40.dp
                layoutHeight=WRAP_CONTENT
                text="<"
                onClick={
                    webContext.goBack()
                } />
            <Button
                layoutWidth=40.dp
                layoutHeight=WRAP_CONTENT
                text=">"
                onClick={
                    webContext.goForward()
                } />
            <EditText
                layoutWidth=0.dp
                layoutHeight=WRAP_CONTENT
                layoutWeight=1f
                singleLine=true
                controlledText=textContents.value
                onTextChanged={ s: CharSequence?, _, _, _ ->
                    textContents.value = s.toString()
                } />
            <Button
                layoutWidth=WRAP_CONTENT
                layoutHeight=WRAP_CONTENT
                text="Go"
                onClick={
                    if (textContents.value.isNotBlank()) {
                        if (WebContext.debug) {
                            Log.d("WebCompAct", "setting url to " + textContents.value)
                        }
                        webParams.url = textContents.value
                    }
                } />
        </LinearLayout>

        if (WebContext.debug) {
            Log.d("WebCompAct", "webComponent: start")
        }

        <WebComponent url=webParams.url webContext />

        if (WebContext.debug) {
            Log.d("WebCompAct", "webComponent: end")
        }
    </LinearLayout>
}