package tw.com.rehome.ui.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tw.com.rehome.R
import tw.com.rehome.model.ApiService
import tw.com.rehome.model.Result
import tw.com.rehome.model.ResultJsonAdapter
import tw.com.rehome.ui.theme.ModeColor

@Composable
internal fun ResultView(
    goBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var result: Result? by remember { mutableStateOf(null) }
    var backgroundColor: Color by remember { mutableStateOf(Color.Transparent) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(vertical = 5.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_arrow_back_ios_24),
                contentDescription = null,
                modifier = Modifier.clickable(onClick = goBack)
            )

            Text("Result", fontSize = 18.sp)
        }
        LaunchedEffect(Unit) {
            scope.launch {
                val allMode = listOf("A", "B", "C", "D", "E")
                val type = allMode.random()
                val postJsonString = """
                {
                    "mode_keyword" : "$type"
                }
            """.trimIndent()
                val raw =
                    ApiService.postJson("http://34.81.156.28:8000/fallback", json = postJsonString)

                delay(3000L)
                result = ResultJsonAdapter(Moshi.Builder().build()).fromJson(raw)
                backgroundColor = ModeColor.colorList[allMode.indexOf(type)]
            }
        }

        result?.let { resultNonNull ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp)
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Label(
                    labelColor = backgroundColor,
                    title = resultNonNull.mode,
                    content = resultNonNull.mood
                )

                Label(
                    labelColor = backgroundColor,
                    title = "Destination",
                    content = resultNonNull.destination
                )

                Label(
                    labelColor = backgroundColor,
                    title = "Activity",
                    content = resultNonNull.activity
                )

                Label(
                    labelColor = backgroundColor,
                    title = "Why",
                    content = resultNonNull.why
                )

                Label(
                    labelColor = backgroundColor,
                    title = "CTA",
                    content = resultNonNull.cta
                )
            }
        } ?: run {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "AI Analyzing...",
                    fontSize = 30.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(alignment = Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun Label(labelColor: Color, title: String, content: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color = labelColor)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight()
                    .background(labelColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(content)
            }
        }
    }
}